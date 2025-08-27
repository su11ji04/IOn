import sys
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import shutil
import tempfile
from datetime import date
from typing import List, Optional

from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel

from load_user import load_user_profile
from stt import run_stt
from diarization import run_diarization
from mapping import map_speaker_segments
from claude_request import build_prompt, request_claude

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

app = FastAPI(title="Voice Analyzer (JSON)", version="1.0.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], allow_credentials=True,
    allow_methods=["*"], allow_headers=["*"],
)

USER_CSV_PATH = os.path.join(os.path.dirname(__file__), "data", "user_profiles.csv")


def _safe_duration_seconds(path: str) -> Optional[int]:
    if not librosa:
        return None
    try:
        return int(round(librosa.get_duration(path=path)))
    except Exception:
        return None

def _build_dialogue_text(segments, diarization):
    turns = map_speaker_segments(segments, diarization)
    lines = [f"{speaker}: {text}" for speaker, text in turns if text]
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
):
    if not audio.filename:
        raise HTTPException(status_code=400, detail="audio file required")

    _, ext = os.path.splitext(audio.filename)
    if not ext:
        ext = ".wav"
    with tempfile.NamedTemporaryFile(delete=False, suffix=ext) as tmp:
        tmp_path = tmp.name
        try:
            shutil.copyfileobj(audio.file, tmp)
        finally:
            try: audio.file.close()
            except Exception: pass

    try:
        user_info = load_user_profile(USER_CSV_PATH, user_id or "u000")
        segments = run_stt(tmp_path)
        diar = run_diarization(tmp_path)
        dialogue = _build_dialogue_text(segments, diar)

        prompt = build_prompt(dialogue, user_info)
        model_out = request_claude(prompt)  # <-- dict 반환

        # 길이: 모델이 주면 우선, 없으면 파일에서 추정
        dur = model_out.get("length")
        if dur is None:
            dur = _safe_duration_seconds(tmp_path)

        # frequency/expression/emotion/changeProposal 안전 매핑
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
            subTitle = model_out.get("subTitle") or subTitle or "제목 없음",
            day      = model_out.get("day") or day or date.today().isoformat(),
            conversationSummary = model_out.get("conversationSummary"),
            length   = dur,
            overallFeedback = model_out.get("overallFeedback"),
            frequency= frequency,
            expression= expression,
            emotion = emotion,
            kidAttitude = model_out.get("kidAttitude"),
            changeProposal = change_proposal,
            pattern = model_out.get("pattern"),
            strength = model_out.get("strength"),
        )

        return JSONResponse(dto.model_dump())

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"analysis failed: {e}")
    finally:
        try: os.remove(tmp_path)
        except Exception: pass
