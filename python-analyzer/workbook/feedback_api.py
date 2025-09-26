# workbook/feedback_api.py
from fastapi import APIRouter, HTTPException, Request
from typing import Any, Dict
import json

from openai import OpenAI
from workbook.utils import load_openai_api_key
from workbook.models import FeedbackIn, FeedbackOut

router = APIRouter(prefix="/workbook", tags=["feedback"])

def _client():
    return OpenAI(api_key=load_openai_api_key())

@router.post("/feedback", response_model=FeedbackOut)
def feedback(req: FeedbackIn):
    try:
        prompt_dict: Dict[str, Any] = {
            "topic": req.topic,
            "mcq": [i.model_dump() for i in req.mcq],
            "writing": [i.model_dump() for i in req.writing],
            "sim_history": [t.model_dump() for t in req.sim_history]
        }
        messages = [
            {"role":"system","content":"You are a JSON-only generator. Always return a single valid JSON object with keys: overall_comment (string, 6~10 Korean sentences), tips (array of 2~4 concise Korean tips). No extra text."},
            {"role":"system","content":"너는 부모 코치다. 공감적이고 실용적인 한국어 피드백을 준다."},
            {"role":"user","content": f"입력: {prompt_dict}\n스키마: {{\"overall_comment\":\"...\",\"tips\":[\"...\",\"...\"]}}"}
        ]
        client = _client()
        resp = client.chat.completions.create(
            model="gpt-4",
            messages=messages,
            temperature=0.7
        )
        data = json.loads(resp.choices[0].message.content)
        return FeedbackOut(
            overall_comment=(data.get("overall_comment") or "").strip(),
            tips=data.get("tips") or []
        )
    except Exception as e:
        raise HTTPException(500, f"feedback failed: {e}")

@router.post("/feedback/debug")
async def feedback_debug(request: Request):
    data = await request.json()
    try:
        type_map = {k: type(v).__name__ for k, v in data.items()}
    except Exception:
        type_map = {}
    return {"keys": list(data.keys()), "types": type_map, "sample": str(data)[:500]}
