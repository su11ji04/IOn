# workbook/sequence_api.py
from fastapi import APIRouter, HTTPException
from uuid import uuid4
from typing import Any, Dict
from openai import OpenAI
from workbook.utils import load_openai_api_key
from workbook.activity_builder import _build_one_activity
from workbook.models import (
    McqOut, SequenceStartIn, SequenceNextWritingIn, WritingOut,
    SequenceToken, SimStartIn, SimStartOut, SimNextIn, SimNextOut
)

router = APIRouter(prefix="/workbook/sequence", tags=["sequence"])

def _client():
    return OpenAI(api_key=load_openai_api_key())

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
        options = mcq.get("options") or []
        optimal = mcq.get("optimal_option")

        if len(options) < 4:
            raise HTTPException(500, "MCQ options < 4")
        if optimal not in options:
            raise HTTPException(500, "MCQ optimal_option must be one of options")

        try:
            wr = _pick(activity, "WRITING")
            writing_cache = {
                "instruction": wr.get("instruction"),
                "example_answer": wr.get("example_answer"),
            }
        except Exception:
            writing_cache = None

        tok = SequenceToken(
            id=str(uuid4()),
            payload={
                "topic": req.topic,
                "user": req.user.dict(),
                "activity_title": activity.get("activity_title"),
                "activity_full": activity,
                "mcq": {
                    "instruction": mcq.get("instruction"),
                    "options": options,
                    "optimal_option": optimal,
                },
                "writing": writing_cache,
            },
        )
        return McqOut(token=tok, mcq=tok.payload["mcq"])
    except HTTPException:
        raise
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
            raise HTTPException(409, "activity not initialized. Call /workbook/sequence/start first")

        writing = _pick(activity, "WRITING")
        payload["writing"] = {
            "instruction": writing.get("instruction"),
            "example_answer": writing.get("example_answer"),
        }
        return WritingOut(token=req.token, writing=payload["writing"])
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(500, f"sequence_next_writing failed: {e}")

@router.post("/sim/start", response_model=SimStartOut)
def sim_start(req: SimStartIn):
    try:
        payload = req.token.payload
        if "writing" not in payload:
            raise HTTPException(400, "writing step not completed")
        payload["writing_answer"] = req.writing_answer

        activity = payload.get("activity_full")
        if not activity:
            raise HTTPException(409, "activity not initialized. Call /workbook/sequence/start first")

        sim = _pick(activity, "SIMULATION")
        situation = sim.get("situation")
        ai_first = sim.get("ai_first_line")
        if not ai_first:
            raise HTTPException(500, "SIMULATION.ai_first_line missing")

        payload["sim"] = {
            "situation": situation,
            "history": [{"role": "ai", "text": ai_first}],
        }
        return SimStartOut(token=req.token, situation=situation, ai_first_line=ai_first)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(500, f"sim_start failed: {e}")

@router.post("/sim/next", response_model=SimNextOut)
def sim_next(req: SimNextIn):
    try:
        user_turns = sum(1 for t in req.history if t.role == "user") + (1 if (req.parent_reply or "").strip() else 0)
        if user_turns >= 2:
            return SimNextOut(ai_line="", finished=True, final_feedback=None)

        client = _client()
        messages = [
            {"role": "system", "content": "너는 아이 역할로 대화해. 한 문장만, 짧고 자연스럽게 한국어. 상황과 이전 대사를 고려해."},
            {"role": "system", "content": f"상황: {req.situation}\n주제: {req.topic}"},
        ]
        for t in req.history:
            messages.append({"role": "assistant" if t.role == "ai" else "user", "content": t.text})
        if (req.parent_reply or "").strip():
            messages.append({"role": "user", "content": req.parent_reply})

        resp = client.chat.completions.create(model="gpt-4", messages=messages, temperature=0.7)
        ai_line = (resp.choices[0].message.content or "").strip()
        return SimNextOut(ai_line=ai_line, finished=False, final_feedback=None)
    except Exception as e:
        raise HTTPException(500, f"sim_next failed: {e}")
