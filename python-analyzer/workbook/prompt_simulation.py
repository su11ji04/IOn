# workbook/prompt_simulation.py
from openai import OpenAI
from workbook.utils import load_openai_api_key

def _client():
    return OpenAI(api_key=load_openai_api_key())

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
반드시 아래 스키마의 **단일 JSON 객체**만 출력한다(여는 중괄호부터 닫는 중괄호까지). 추가 텍스트/주석 금지.

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
      "instruction": "시뮬레이션 안내",
      "situation": "상황 설명",
      "ai_optimal_response": "부모의 모범 응답 예시"
    }}
  ]
}}

요구사항:
- activities에는 MCQ, WRITING, SIMULATION **각 1개씩** 포함 (총 3개)
- 아이 나이({child_age}), 성향({child_traits}), 부모 스타일({parenting_style}), 목표({parenting_goal}), 건강이슈({health_issues}), 선호 톤({preferred_tone})을 반영
- 주제: {topic}
- 참고 텍스트 일부(발췌):
\"\"\"{text_chunk[:1500]}\"\"\"
"""

def generate_simulation(prompt: str) -> str:
    client = _client()
    resp = client.chat.completions.create(
        model="gpt-4",
        messages=[
            {"role": "system", "content": "You are a JSON-only generator. Always return a single valid JSON object with no extra text."},
            {"role": "system", "content": "너는 부모교육 코치다. 실전적인 워크북을 만든다."},
            {"role": "user", "content": prompt}
        ],
        temperature=0.7,
    )
    return resp.choices[0].message.content
