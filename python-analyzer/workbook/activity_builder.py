# workbook/activity_builder.py
from typing import Any, Dict
from fastapi import HTTPException
import os, json, re
from openai import OpenAI
from workbook.utils import CSV_PATH, load_openai_api_key
from workbook.preprocess_data import load_and_clean_text, split_text

ALLOWED_TYPES = {"MCQ", "WRITING", "SIMULATION"}

# [개발용] DEBUG
LAST_TRACE = {
    "topic": None,
    "user": None,
    "chunk_index": None,
    "prompt_head": "",
    "raw_head": "",
}

def _client():
    return OpenAI(api_key=load_openai_api_key())

# FALLBACK DEMO ACTIVITY
def _demo_activity() -> Dict[str, Any]:
    return {
        "activity_title": "데모 활동",
        "activities": [
            {
                "type": "MCQ",
                "instruction": "아이의 떼쓰기 대응으로 더 적절한 것은?",
                "options": ["무시", "소리치기", "감정명명 후 선택지 제시", "바로 사주기"],
                "optimal_option": "감정명명 후 선택지 제시",
            },
            {
                "type": "WRITING",
                "instruction": "오늘 아이와 있었던 긍정적 순간을 서술하세요.",
                "example_answer": "아이와 블록 놀이를 하며 차분히 기다려줬다.",
            },
            {
                "type": "SIMULATION",
                "instruction": "상황에 맞게 대화하세요.",
                "situation": "마트에서 과자를 사달라고 떼쓰는 상황",
                "ai_first_line": "아이: 싫어! 과자 사줘! 지금 먹고 싶단 말이야!",
            },
        ],
    }

# JSON -> DICT
def _extract_json(text: str):
    if text is None:
        raise ValueError("Empty response")
    cleaned = re.sub(r"^```(?:json)?\s*|\s*```$", "", text.strip(), flags=re.IGNORECASE | re.MULTILINE)
    try:
        return json.loads(cleaned)
    except Exception:
        pass
    m = re.search(r"\{.*?\}", cleaned, flags=re.S)
    if not m:
        raise ValueError("No JSON object found in response")
    return json.loads(m.group(0))

# PROMPT
def generate_prompt(text_chunk: str, topic: str, user: dict) -> str:
    child_age = user.get("child_age")
    parenting_style = user.get("parenting_style")
    parenting_goal = user.get("parenting_goal")
    child_traits = user.get("child_traits")
    health_issues = user.get("allergies_or_health_issues") or user.get("health_issues")
    preferred_tone = user.get("preferred_tone")
    language = user.get("language") or "Korean"

    return f"""
너는 부모교육 워크북을 설계하는 코치다. 한국어로만 답한다.
반드시 아래 스키마의 단일 JSON 객체만 출력한다. 추가 텍스트/주석 금지.

스키마 예:
{{
  "activity_title": "string",
  "activities": [
    {{
      "type": "MCQ",
      "instruction": "질문",
      "options": ["A","B","C","D"],
      "optimal_option": "A"
    }},
    {{
      "type": "WRITING",
      "instruction": "서술형 질문",
      "example_answer": "예시 답"
    }},
    {{
      "type": "SIMULATION",
      "situation": "상황 설명",
      "ai_first_line": "아이의 대사"
    }}
  ]
}}

요구사항:
- activities에는 MCQ, WRITING, SIMULATION 각 1개씩 포함 (총 3개)
- 아이 나이({child_age}), 성향({child_traits}), 부모 스타일({parenting_style}), 목표({parenting_goal}), 건강이슈({health_issues}), 선호 톤({preferred_tone}) 반영
- 주제: {topic}
- 참고 텍스트 일부(발췌):
- ai_first_line은 상황에 맞는 아이의 대사를 출력한다

추가 제약:
- "SIMULATION.situation"은 MCQ/WRITING 맥락과 일관되게 작성(주제와 동일 도메인).
- "SIMULATION.ai_first_line"은 반드시 아이의 말로, 한 문장, 짧고 자연스러운 구어체, 감탄사/부정/요구 표현 가능.
  - 예시: "싫어! 지금 그거 사줘!", "안 먹을래!", "내 거야!"
  - 금지: 어른/코치 톤(예: "조금만 먹어보자", "우리 몸에 좋아", "약속 지키자", "그치만/하지만" 등 훈육 문장)
  - 높임말/설명체/조언체/복문 금지.
- "MCQ.options"는 4개 정확히, "optimal_option"은 options 중 하나의 값과 정확히 동일한 문자열.
- WRITING은 실제 부모가 쓸 법한 한 단락 예시 답을 간결하게.
- 출력은 JSON 한 개만. 그 외 어떤 텍스트도 출력하지 말 것.
\"\"\"{text_chunk[:1500]}\"\"\""""

# WORKBOOK 생성
def _build_one_activity(topic: str, user: Any, max_chunks: int = 1) -> Dict[str, Any]:
    if not isinstance(user, dict):
        try:
            user = user.dict()
        except Exception:
            raise HTTPException(500, f"user is not dict-like: {type(user)}")

    text = ""
    try:
        if os.path.exists(CSV_PATH) and os.path.isfile(CSV_PATH):
            text = load_and_clean_text(CSV_PATH)
    except Exception as e:
        print(f"[WORKBOOK][WARN] CSV load failed: {e} (path={CSV_PATH})")

    try:
        chunks = split_text(text, max_tokens=1200) if text else [""]
        if not chunks:
            chunks = [""]

        for i, chunk in enumerate(chunks[:max_chunks]):
            LAST_TRACE["topic"] = topic
            LAST_TRACE["user"] = user
            LAST_TRACE["chunk_index"] = i

            prompt = generate_prompt(chunk, topic, user)
            LAST_TRACE["prompt_head"] = (prompt or "")[:800]

            client = _client()
            resp = client.chat.completions.create(
                model="gpt-4",
                messages=[
                    {"role": "system", "content": "You are a JSON-only generator. Always return a single valid JSON object with no extra text."},
                    {"role": "system", "content": "너는 0-7세 아이를 둔 초보 부모의 코치다. 주제에 맞는 실전적인 한국어 워크북을 만든다."},
                    {"role": "user", "content": prompt},
                ],
                temperature=0.7,
            )
            raw = resp.choices[0].message.content
            LAST_TRACE["raw_head"] = (raw or "")[:800]

            data = _extract_json(raw)
            activities = data.get("activities") or []
            if not isinstance(activities, list):
                raise ValueError("activities must be a list")

            types = [a.get("type") for a in activities if isinstance(a, dict)]
            required = {"MCQ", "WRITING", "SIMULATION"}
            if set(types) != required or len(activities) != 3:
                raise ValueError(f"Invalid activities set (need exactly MCQ/WRITING/SIMULATION one each). got={types}")

            mcq = next((a for a in activities if a.get("type") == "MCQ"), None)
            if not mcq or not mcq.get("options") or len(mcq["options"]) < 4:
                raise ValueError("MCQ must have at least 4 options")

            return {
                "activity_title": data.get("activity_title") or data.get("title") or "워크북",
                "activities": activities,
            }

        print("[WORKBOOK][WARN] All chunks failed to produce valid activity. Falling back to demo.")
        return _demo_activity()

    except Exception as e:
        print(f"[WORKBOOK][ERR] _build_one_activity failed: {e}")
        return _demo_activity()
