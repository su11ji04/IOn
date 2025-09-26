from fastapi import APIRouter, HTTPException
from uuid import uuid4
from typing import Any, Dict
import os

from workbook.models import (
    McqOut, SequenceStartIn, SequenceNextWritingIn, WritingOut,
    SequenceToken
)
from workbook.settings import CSV_PATH, HAVE_SIM_ENGINE
from workbook.run_workbook_simulation import run_workbook_simulation

router = APIRouter(prefix="/workbook/sequence", tags=["sequence"])

def _demo_activity() -> Dict[str, Any]:
    return {
        "activity_title": "데모 활동",
        "activities": [
            {"type": "WRITING", "instruction": "오늘 아이와 있었던 긍정적 순간을 서술하세요.", "example_answer": "아이와 블록 놀이를 하며 차분히 기다려줬다."},
            {"type": "MCQ", "instruction": "아이의 떼쓰기 대응으로 더 적절한 것은?", "options": ["무시", "소리치기", "감정명명 후 선택지 제시", "바로 사주기"], "optimal_option": "감정명명 후 선택지 제시"},
            {"type": "SIMULATION", "instruction": "상황에 맞게 대화하세요.", "situation": "마트에서 과자를 사달라고 떼쓰는 상황", "ai_optimal_response": "부모: ...\\n아이: ..."}
        ]
    }

def _build_one_activity(topic: str, user: Any) -> Dict[str, Any]:
    # ✅ dict 변환 보장
    if not isinstance(user, dict):
        try:
            user = user.dict()
        except Exception:
            raise HTTPException(500, f"user is not dict-like: {type(user)}")

    if not HAVE_SIM_ENGINE or not os.path.exists(CSV_PATH):
        return _demo_activity()
    try:
        acts = run_workbook_simulation(csv_path=CSV_PATH, topic=topic, user=user, max_chunks=1)
        if not acts:
            return _demo_activity()
        return acts[0]
    except Exception as e:
        print(f"[WORKBOOK] sequence_api fallback to demo due to error: {e}")
        return _demo_activity()

def _pick(activity: Dict[str, Any], t: str) -> Dict[str, Any]:
    for item in activity.get("activities", []):
        if item.get("type") == t:
            return item
    raise HTTPException(500, f"{t} not found in activity")

@router.post("/start", response_model=McqOut)
def sequence_start(req: SequenceStartIn):
    try:
        activity = _build_one_activity(req.topic, req.user)
        mcq = _pick(activity, "MCQ")
        if not mcq.get("options") or len(mcq["options"]) < 4:
            raise HTTPException(500, "MCQ options < 4")

        tok = SequenceToken(id=str(uuid4()), payload={
            "topic": req.topic,
            "user": req.user.dict(),
            "activity_title": activity.get("activity_title"),
            # "activity_full": activity,   # ❌ 제거 → 선택형만 리턴
            "mcq": {
                "instruction": mcq.get("instruction"),
                "options": mcq.get("options"),
                "optimal_option": mcq.get("optimal_option")
            }
        })
        return McqOut(token=tok, mcq=tok.payload["mcq"])
    except Exception as e:
        raise HTTPException(500, f"sequence_start failed: {e}")

@router.post("/next/writing", response_model=WritingOut)
def sequence_next_writing(req: SequenceNextWritingIn):
    try:
        payload = req.token.payload
        if "mcq" not in payload:
            raise HTTPException(400, "token payload missing mcq")
        payload["mcq"]["selected"] = req.selected_option

        activity = payload.get("activity_full")
        if not activity:
            activity = _build_one_activity(payload["topic"], payload["user"])
            payload["activity_full"] = activity

        writing = _pick(activity, "WRITING")
        payload["writing"] = {
            "instruction": writing.get("instruction"),
            "example_answer": writing.get("example_answer")
        }
        return WritingOut(token=req.token, writing=payload["writing"])
    except Exception as e:
        raise HTTPException(500, f"sequence_next_writing failed: {e}")
