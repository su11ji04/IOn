# voicereport/main.py
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import json
import shutil
import tempfile
from datetime import date
from typing import List, Optional, Dict, Any

from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel

from stt import run_stt
from voicereport.diarization import run_diarization  # <- 표준 리스트형을 반환하도록 수정된 버전 전제
from mapping import map_speaker_segments              # <- (stt_segments, diar_segments_list) 시그니처로 사용
from voicereport.claude_request import build_prompt, request_claude

import logging
logger = logging.getLogger("uvicorn.error")

try:
    import librosa
except Exception:
    librosa = None

# ====== DTOs ======
class Frequency(BaseModel):
    parentFrequency: Optional[int] = None
    kidFrequency: Optional[int] = None
    frequencyFeedback: Optional[str] = None

class Expression(BaseModel):
    parentExpression: Optional[str] = None
    kidExpression: Optional[str] = None
    parentConditions: Optional[str] = None
    kidConditions: Optional[str] = None
    expressionFeedback: Optional[str] = None

class EmotionTimeline(BaseModel):
    time: Optional[str] = None
    momentEmotion: Optional[str] = None

class Emotion(BaseModel):
    timeline: Optional[List[EmotionTimeline]] = None
    emotionFeedback: Optional[str] = None

class ChangeProposal(BaseModel):
    existingExpression: Optional[str] = None
    proposalExpression: Optional[str] = None

class AnalysisReportDto(BaseModel):
    subTitle: Optional[str] = None
    day: Optional[str] = None
    conversationSummary: Optional[str] = None
    length: Optional[int] = None
    overallFeedback: Optional[str] = None
    frequency: Optional[Frequency] = None
    expression: Optional[Expression] = None
    emotion: Optional[Emotion] = None
    kidAttitude: Optional[str] = None
    changeProposal: Optional[List[ChangeProposal]] = None
    pattern: Optional[str] = None
    strength: Optional[str] = None
# ===================

# 옵션: 다이어리제이션 스킵 (속도/원인 고립용)
SKIP_DIARIZATION = os.getenv("SKIP_DIARIZATION", "0") == "1"

app = FastAPI(title="Voice Analyzer (JSON)", version="1.2.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], allow_credentials=True,
    allow_methods=["*"], allow_headers=["*"],
)

def _safe_duration_seconds(path: str) -> Optional[int]:
    if not librosa:
        return None
    try:
        return int(round(librosa.get_duration(path=path)))
    except Exception:
        return None

def _build_dialogue_text(turns: List[Dict[str, Any]]) -> str:
    """
    turns: [{"speaker": "SPEAKER_00", "start": float, "end": float, "text": str}, ...]
    """
    lines = []
    for t in turns:
        spk = t.get("speaker", "UNKNOWN")
        tx = (t.get("text") or "").strip()
        if tx:
            lines.append(f"{spk}: {tx}")
    return "\n".join(lines).strip()

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/analyze")
async def analyze(
        audio: UploadFile = File(...),
        subTitle: str = Form(""),
        day: Optional[str] = Form(None),
        user_id: Optional[str] = Form(None),
        user_profile_json: Optional[str] = Form(None),
):
    if not audio.filename:
        raise HTTPException(status_code=400, detail="audio file required")

    # [REQ LOG]
    try:
        logger.info("[STEP3][PY-REQ] audio.filename=%s, content_type=%s, user_id=%s",
                    audio.filename, getattr(audio, "content_type", None), user_id)
        if user_profile_json:
            preview = user_profile_json[:200].replace("\n", " ")
            bytelen = len(user_profile_json.encode("utf-8"))
            logger.info("[STEP3][PY-REQ] user_profile_json.len=%d, preview=%s", bytelen, preview)
        else:
            logger.info("[STEP3][PY-REQ] user_profile_json is EMPTY")
    except Exception:
        pass

    # 1) 오디오 임시 저장
    _, ext = os.path.splitext(audio.filename)
    if not ext:
        ext = ".wav"
    with tempfile.NamedTemporaryFile(delete=False, suffix=ext) as tmp:
        tmp_path = tmp.name
        try:
            shutil.copyfileobj(audio.file, tmp)
        finally:
            try:
                audio.file.close()
            except Exception:
                pass

    try:
        try:
            fsize = os.path.getsize(tmp_path)
            logger.info("[STEP3][PY-FILE] saved tmp_path=%s, bytes=%d", tmp_path, fsize)
        except Exception:
            logger.info("[STEP3][PY-FILE] saved tmp_path=%s (size check failed)", tmp_path)

        # 2) 사용자 JSON 파싱
        user_info: Dict[str, Any] = {}
        if user_profile_json:
            try:
                user_info = json.loads(user_profile_json)
                logger.info("[STEP3][PY-USER] keys=%s", list(user_info.keys()))
                logger.info("[STEP3][PY-USER] preview child_age=%s, preferred_tone=%s",
                            user_info.get("child_age"), user_info.get("preferred_tone"))
            except Exception as e:
                logger.warning("[STEP3][PY-USER] parse error: %s", e)
        else:
            logger.warning("[STEP3][PY-USER] user_profile_json missing")

        # 3) STT
        try:
            stt_result = run_stt(tmp_path)  # 권장 형태: {"text": "...", "segments":[{start, end, text}, ...]}
            stt_segments = stt_result.get("segments", []) if isinstance(stt_result, dict) else stt_result
            logger.info("[STEP3][PY-STT] segments.count=%d", len(stt_segments) if stt_segments else 0)
        except Exception as e:
            logger.error("[ERR] STT failed: %s", e, exc_info=True)
            raise HTTPException(status_code=500, detail="STT_FAILED")

        # 4) Diarization (선택)
        diar_segments: List[Dict[str, Any]] = []
        if SKIP_DIARIZATION:
            logger.info("[STEP3][PY-DIAR] skipped by env (SKIP_DIARIZATION=1)")
        else:
            try:
                # run_diarization은 표준 리스트형 반환:
                # [{"start": float, "end": float, "speaker": "SPEAKER_00"}, ...]
                diar_segments = run_diarization(tmp_path)
                logger.info("[STEP3][PY-DIAR] ok, n_segments=%d", len(diar_segments))
            except Exception as e:
                logger.error("[ERR] DIAR failed: %s", e, exc_info=True)
                raise HTTPException(status_code=500, detail="DIARIZATION_FAILED")

        # 5) STT x DIAR 매핑 → turns
        try:
            turns = map_speaker_segments(stt_segments, diar_segments)
            # 기대: turns = [{"speaker", "start", "end", "text"}, ...]
            logger.info("[STEP3][PY-MAP] turns=%d", len(turns) if turns else 0)
        except Exception as e:
            logger.error("[ERR] MAP failed: %s", e, exc_info=True)
            raise HTTPException(status_code=500, detail="MAPPING_FAILED")

        # 6) 대화 텍스트 생성
        dialogue = _build_dialogue_text(turns)
        logger.info("[STEP3][PY-DIALOGUE] len_chars=%d, lines=%d",
                    len(dialogue or ""), (dialogue or "").count("\n") + (1 if dialogue else 0))

        # 7) 프롬프트 & LLM 호출
        try:
            prompt = build_prompt(dialogue, user_info)
            logger.info("[STEP3][PY-PROMPT] len_chars=%d, user_keys=%s", len(prompt), list(user_info.keys()))
        except Exception as e:
            logger.error("[ERR] PROMPT build failed: %s", e, exc_info=True)
            raise HTTPException(status_code=500, detail="PROMPT_BUILD_FAILED")

        try:
            model_out = request_claude(prompt)  # dict 기대
            logger.info("[STEP3][PY-LLM] keys=%s", list(model_out.keys()) if isinstance(model_out, dict) else type(model_out))
        except Exception as e:
            logger.error("[ERR] LLM failed: %s", e, exc_info=True)
            raise HTTPException(status_code=500, detail="LLM_FAILED")

        # 8) 길이(초) 계산: 모델 → 없으면 파일에서 추정
        dur = None
        if isinstance(model_out, dict):
            dur = model_out.get("length")
        if dur is None:
            dur = _safe_duration_seconds(tmp_path)
            logger.info("[STEP3][PY-AUDIO] duration.fallback=%s", dur)

        # 9) 안전 매핑 (모델 출력 -> DTO)
        def as_frequency(obj):
            return Frequency(**obj) if isinstance(obj, dict) else None

        def as_expression(obj):
            return Expression(**obj) if isinstance(obj, dict) else None

        def as_emotion(obj):
            if not isinstance(obj, dict):
                return None
            tl = obj.get("timeline") or []
            timeline = [EmotionTimeline(**t) for t in tl if isinstance(t, dict)]
            return Emotion(timeline=timeline, emotionFeedback=obj.get("emotionFeedback"))

        def as_change_proposal(obj):
            if not isinstance(obj, list):
                return None
            return [ChangeProposal(**cp) for cp in obj if isinstance(cp, dict)]

        frequency = as_frequency(model_out.get("frequency")) if isinstance(model_out, dict) else None
        expression = as_expression(model_out.get("expression")) if isinstance(model_out, dict) else None
        emotion = as_emotion(model_out.get("emotion")) if isinstance(model_out, dict) else None
        change_proposal = as_change_proposal(model_out.get("changeProposal")) if isinstance(model_out, dict) else None

        dto = AnalysisReportDto(
            subTitle=(model_out.get("subTitle") if isinstance(model_out, dict) else None) or subTitle or "제목 없음",
            day=(model_out.get("day") if isinstance(model_out, dict) else None) or day or date.today().isoformat(),
            conversationSummary=(model_out.get("conversationSummary") if isinstance(model_out, dict) else None),
            length=dur,
            overallFeedback=(model_out.get("overallFeedback") if isinstance(model_out, dict) else None),
            frequency=frequency,
            expression=expression,
            emotion=emotion,
            kidAttitude=(model_out.get("kidAttitude") if isinstance(model_out, dict) else None),
            changeProposal=change_proposal,
            pattern=(model_out.get("pattern") if isinstance(model_out, dict) else None),
            strength=(model_out.get("strength") if isinstance(model_out, dict) else None),
        )

        logger.info(
            "[STEP3][PY-RES] subTitle=%s, day=%s, length=%s, has_freq=%s, has_expr=%s, timeline_len=%s",
            dto.subTitle, dto.day, dto.length,
            dto.frequency is not None, dto.expression is not None,
            len(dto.emotion.timeline) if (dto.emotion and dto.emotion.timeline) else 0,
            )

        return JSONResponse(dto.model_dump())

    except HTTPException:
        # 위에서 이미 적절히 세팅됨
        raise
    except Exception as e:
        logger.error("[ERR] analysis failed: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=f"analysis failed: {e}")
    finally:
        try:
            os.remove(tmp_path)
        except Exception:
            pass
