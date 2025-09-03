# workbook/prompt_simulation.py
from openai import OpenAI
from workbook.utils import load_openai_api_key

api_key = load_openai_api_key()
client = OpenAI(api_key=api_key)

def generate_prompt(text_chunk: str, topic: str, user: dict) -> str:
    child_age = user.get("child_age")
    parenting_style = user.get("parenting_style")
    parenting_goal = user.get("parenting_goal")
    child_traits = user.get("child_traits")
    health_issues = user.get("allergies_or_health_issues") or user.get("health_issues")
    preferred_tone = user.get("preferred_tone")

    return f"""
다음은 부모 교육 워크북을 위한 참고 이론입니다:

\"\"\"{text_chunk}\"\"\"

유저 정보:
- 아이 나이: {child_age}세
- 부모 육아 성향: {parenting_style}
- 육아 목표: {parenting_goal}
- 아이 특성: {child_traits}
- 건강/알러지 관련 사항: {health_issues}
- 선호 말투: {preferred_tone}

위 이론과 유저 정보를 기반으로, '{topic}' 주제에 대해 다음과 같이 구성된 워크북 액티비티 하나를 JSON 형식으로 생성하세요:

1. 활동 제목 (activity_title)
2. activities: [작성형 1개, 선택형 1개(옵션≥4, optimal 1개), 시뮬레이션 1개(2~3턴 예시 포함)]

출력은 반드시 JSON 구조만:
{{
  "activity_title": "...",
  "activities": [
    {{
      "type": "작성형",
      "instruction": "...",
      "example_answer": "..."
    }},
    {{
      "type": "선택형",
      "instruction": "...",
      "options": ["...","...","...","..."],
      "optimal_option": "..."
    }},
    {{
      "type": "시뮬레이션",
      "instruction": "...",
      "situation": "...",
      "your_response": "___",
      "ai_optimal_response": "부모: ...\\n아이: ..."
    }}
  ]
}}
반드시 JSON 형식으로만 응답하세요.
"""

def generate_simulation(prompt: str) -> str:
    resp = client.chat.completions.create(
        model="gpt-4",
        messages=[
            {"role": "system", "content": "너는 부모교육 코치야. 실제 상황 기반의 실용적인 워크북을 만든다."},
            {"role": "user", "content": prompt}
        ],
        temperature=0.7,
    )
    return resp.choices[0].message.content
