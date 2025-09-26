from fastapi import APIRouter, HTTPException
from typing import Any, Dict
import os

from openai import OpenAI
from workbook.utils import load_openai_api_key
from workbook.models import (
    SimStartIn, SimStartOut, SimNextIn, SimNextOut
)
from workbook.settings import CSV_PATH, HAVE_SIM_ENGINE
from workbook.run_workbook_simulation import run_workbook_simulation

router = APIRouter(prefix="/workbook/sim", tags=["simulation"])

def _client():
    return OpenAI(api_key=load_openai_api_key())

def _demo_activity() -> Dict[str, Any]:
    return {
        "activity_title": "데모 활동",
        "activities": [
            {"type": "SIMULATION", "instruction": "상황에 맞게 대화하세요.", "situation": "집에서 숙제를 하기 싫다며 짜증내는 상황"}
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
        print(f"[WORKBOOK] sim_api fallback to demo due to error: {e}")
        return _demo_activity()


# ========================
# 실제 엔드포인트
# ========================

@router.post("/start", response_model=SimStartOut)
def sim_start(req: SimStartIn):
    try:
        payload = req.token.payload
        if "writing" not in payload:
            raise HTTPException(400, "writing step not completed")

        payload["writing_answer"] = req.writing_answer

        activity = payload.get("activity_full")
        if not activity:
            activity = _build_one_activity(payload["topic"], payload["user"])
            payload["activity_full"] = activity

        sim = None
        for item in activity.get("activities", []):
            if item.get("type") == "SIMULATION":
                sim = item
                break
        if not sim:
            raise HTTPException(500, "SIMULATION activity not found")

        situation = sim.get("situation") or "상황 설명이 없습니다."

        messages = [
            {"role": "system", "content": "너는 아이 역할로 대화해. 한 번에 한 문장만, 짧고 자연스럽게 한국어로 답해."},
            {"role": "system", "content": f"상황: {situation}\n주제: {payload['topic']}\n부모는 아직 말하지 않았다. 아이가 먼저 한마디를 건넨다."}
        ]

        client = _client()
        resp = client.chat.completions.create(
            model="gpt-4",
            messages=messages,
            temperature=0.7,
        )
        ai_first = resp.choices[0].message.content.strip()

        payload["sim"] = {"situation": situation, "history": [{"role": "ai", "text": ai_first}]}

        return SimStartOut(token=req.token, situation=situation, ai_first_line=ai_first)
    except Exception as e:
        raise HTTPException(500, f"sim_start failed: {e}")


@router.post("/next", response_model=SimNextOut)
def sim_next(req: SimNextIn):
    try:
        user_turns = sum(1 for t in req.history if t.role == "user") + (1 if req.parent_reply.strip() else 0)
        if user_turns >= 2:
            return SimNextOut(ai_line="", finished=True, final_feedback=None)

        client = _client()
        messages = [
            {"role": "system", "content": "너는 아이 역할로 대화해. 한 번에 한 문장만, 짧고 자연스럽게 한국어로 답해."},
            {"role": "system", "content": f"상황: {req.situation}\n주제: {req.topic}"}
        ]
        for t in req.history:
            if t.role == "ai":
                messages.append({"role": "assistant", "content": t.text})
            else:
                messages.append({"role": "user", "content": t.text})
        messages.append({"role": "user", "content": req.parent_reply})

        resp = client.chat.completions.create(
            model="gpt-4",
            messages=messages,
            temperature=0.7,
        )
        ai_line = resp.choices[0].message.content.strip()

        return SimNextOut(ai_line=ai_line, finished=False, final_feedback=None)
    except Exception as e:
        raise HTTPException(500, f"sim_next failed: {e}")
