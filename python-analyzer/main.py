import sys
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import json
import shutil
import tempfile
from datetime import date
from typing import List, Optional

from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel

from stt import run_stt
from diarization import run_diarization
from mapping import map_speaker_segments
from claude_request import build_prompt, request_claude

import logging
logger = logging.getLogger("uvicorn.error")


try:
    import librosa
except Exception:
    librosa = None

# ---- DTO ----
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
# -------------


app = FastAPI(title="Voice Analyzer (JSON)", version="1.1.0")
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


def _build_dialogue_text(segments, diarization):
    """
    diarization 이 Annotation 이면 itertracks 사용.
    dict 등 비호환 타입이면 폴백으로 STT 텍스트만 이어붙임.
    """
    try:
        turns = map_speaker_segments(segments, diarization)
        lines = [f"{speaker}: {text}" for speaker, text in turns if text]
        txt = "\n".join(lines).strip()
        if txt:
            return txt
    except Exception:
        pass

    # 폴백: 스피커 구분 없이 STT 텍스트 이어붙이기
    return "\n".join([f"UNKNOWN: {s.get('text','')}" for s in segments if s.get("text")]).strip()


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

    # [STEP3][PY-REQ] 폼 파라미터 수신 로그
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

    # 저장된 파일 사이즈 확인
    try:
        fsize = os.path.getsize(tmp_path)
        logger.info("[STEP3][PY-FILE] saved tmp_path=%s, bytes=%d", tmp_path, fsize)
    except Exception:
        logger.info("[STEP3][PY-FILE] saved tmp_path=%s (size check failed)", tmp_path)

    # 2) 사용자 JSON 파싱
    user_info = {}
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

    try:
        # 3) STT + 화자분리
        segments = run_stt(tmp_path)      # whisper segments (list[dict])
        diar = run_diarization(tmp_path)  # pyannote Annotation

        # STT/DIAR 로그
        try:
            logger.info("[STEP3][PY-STT] segments.count=%d", len(segments) if segments else 0)
        except Exception:
            pass
        try:
            has_iter = hasattr(diar, "itertracks")
            logger.info("[STEP3][PY-DIAR] type=%s, has_itertracks=%s", type(diar).__name__, has_iter)
        except Exception:
            pass

        # 4) 대화 텍스트 구성
        dialogue = _build_dialogue_text(segments, diar)
        logger.info("[STEP3][PY-DIALOGUE] len_chars=%d, lines=%d",
                    len(dialogue or ""), (dialogue or "").count("\n") + 1 if dialogue else 0)

        # 5) 프롬프트 & LLM 호출
        prompt = build_prompt(dialogue, user_info)
        logger.info("[STEP3][PY-PROMPT] len_chars=%d, user_keys=%s", len(prompt), list(user_info.keys()))

        model_out = request_claude(prompt)  # dict
        logger.info("[STEP3][PY-LLM] keys=%s", list(model_out.keys()))

        # 6) 길이(초) 계산: 모델 우선, 없으면 파일에서 추정
        dur = model_out.get("length")
        if dur is None:
            dur = _safe_duration_seconds(tmp_path)
            logger.info("[STEP3][PY-AUDIO] duration.fallback=%s", dur)

        # 7) 안전 매핑
        freq = model_out.get("frequency")
        frequency = Frequency(**freq) if isinstance(freq, dict) else None

        expr = model_out.get("expression")
        expression = Expression(**expr) if isinstance(expr, dict) else None

        emo = model_out.get("emotion")
        if isinstance(emo, dict):
            tl = emo.get("timeline") or []
            timeline = [EmotionTimeline(**t) for t in tl if isinstance(t, dict)]
            emotion = Emotion(timeline=timeline, emotionFeedback=emo.get("emotionFeedback"))
        else:
            emotion = None

        cps = model_out.get("changeProposal")
        change_proposal = [ChangeProposal(**cp) for cp in cps] if isinstance(cps, list) else None

        dto = AnalysisReportDto(
            subTitle=model_out.get("subTitle") or subTitle or "제목 없음",
            day=model_out.get("day") or day or date.today().isoformat(),
            conversationSummary=model_out.get("conversationSummary"),
            length=dur,
            overallFeedback=model_out.get("overallFeedback"),
            frequency=frequency,
            expression=expression,
            emotion=emotion,
            kidAttitude=model_out.get("kidAttitude"),
            changeProposal=change_proposal,
            pattern=model_out.get("pattern"),
            strength=model_out.get("strength"),
        )

        # 응답 직전 요약
        logger.info(
            "[STEP3][PY-RES] subTitle=%s, day=%s, length=%s, has_freq=%s, has_expr=%s, timeline_len=%s",
            dto.subTitle, dto.day, dto.length,
            dto.frequency is not None, dto.expression is not None,
            len(dto.emotion.timeline) if (dto.emotion and dto.emotion.timeline) else 0
        )

        return JSONResponse(dto.model_dump())

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"analysis failed: {e}")
    finally:
        try:
            os.remove(tmp_path)
        except Exception:
            pass
