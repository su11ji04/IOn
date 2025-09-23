from typing import Optional, Dict, Any
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from embedding_retrieval import retrieve_relevant_context
from generate_response import generate_chat_response
from openai import OpenAI

class UserProfile(BaseModel):
    child_age: Optional[int] = None
    parenting_style: Optional[str] = None
    parenting_goal: Optional[str] = None
    child_traits: Optional[str] = None
    preferred_tone: Optional[str] = None
    language: Optional[str] = "ko"
    health_issues: Optional[str] = None

    def to_dict(self) -> Dict[str, Any]:
        return {
            "child_age": self.child_age,
            "parenting_style": self.parenting_style,
            "parenting_goal": self.parenting_goal,
            "child_traits": self.child_traits,
            "preferred_tone": self.preferred_tone,
            "language": self.language,
            "health_issues": self.health_issues,
        }


class ChatAskPayload(BaseModel):
    question: str
    user: UserProfile
    user_id: Optional[str] = None
    k: int = 2


class ChatAnswerResponse(BaseModel):
    answer: str
    used_user_id: Optional[str] = None
    meta: Dict[str, Any] = {}


app = FastAPI(title="Parenting Chatbot API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/chat/ask", response_model=ChatAnswerResponse)
def chat_ask(payload: ChatAskPayload):
    # 1) 사용자 정보 dict 변환
    user_info = payload.user.to_dict()

    # 2) 문서 검색
    csv_path = os.environ.get("CHATBOT_OCR_CSV", "data/ocr_paragraphs.csv")
    try:
        context = retrieve_relevant_context(payload.question, csv_path, payload.k)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"retrieve_relevant_context error: {e}")

    # 3) LLM 응답 생성
    try:
        answer = generate_chat_response(payload.question, context, user_info)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"generate_chat_response error: {e}")

    return ChatAnswerResponse(
        answer=answer,
        used_user_id=payload.user_id,
        meta={"top_k": payload.k, "csv_path": csv_path},
    )


if __name__ == "__main__":
    import uvicorn

    # OpenAI API key
    try:
        api_key = open("keys/openai_key.txt", "r", encoding="utf-8").read().strip()
        client = OpenAI(api_key=api_key)

        client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[{"role": "user", "content": "ping"}],
            max_tokens=5,
        )
        print("[OPENAI] startup sanity: OK")
    except Exception as e:
        print(f"[OPENAI] startup sanity: FAIL -> {e}")

    uvicorn.run("main:app", host="0.0.0.0", port=int(os.environ.get("PORT", 8082)), reload=True)
