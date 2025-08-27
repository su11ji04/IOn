# python-analyzer/claude_request.py
import os
import json
import re
import requests

# Claude에게 보낼 프롬프트 구성
def build_prompt(dialogue: str, user_info: dict) -> str:
    # f-string 안에서 JSON { } 는 반드시 {{ }} 로 이스케이프
    return f"""
당신은 초보 부모를 위한 감정·대화 분석 어시스턴트입니다.
주어진 대화(transcript)를 분석하여 **아래 JSON 스키마와 정확히 일치하는 결과만** 출력하세요.
불필요한 텍스트, 설명, 마크다운, 백틱은 절대 포함하지 마세요. 오직 JSON만 출력하세요.

[사용자 정보]
- child_age: {user_info.get('child_age')}
- parenting_style: {user_info.get('parenting_style')}
- parenting_goal: {user_info.get('parenting_goal')}
- child_traits: {user_info.get('child_traits')}
- preferred_tone: {user_info.get('preferred_tone')}
- language: {user_info.get('language')}
- health_issues: {user_info.get('health_issues')}

[분석 지침]
- 대화 속 화자(SPEAKER_00, SPEAKER_01)의 역할(부모/아이)을 자연스럽게 추정해 반영.
- 가능한 한 **구체적**이고 **행동 지향적인** 피드백을 제공.
- 수치값은 범위를 지켜라: frequency.*는 0~100의 정수(%) 또는 null.
- 시간 포맷은 "MM:SS" (예: "00:45"). 추정 어려우면 timeline은 빈 배열([]) 또는 null.
- 알 수 없는 항목은 null 또는 빈 배열로 채워라.
- 모든 자연어는 한국어로.

[출력 JSON 스키마]
{{
  "subTitle": string | null,
  "day": string | null,
  "conversationSummary": string | null,
  "length": integer | null,
  "overallFeedback": string | null,

  "frequency": {{
    "parentFrequency": integer | null,
    "kidFrequency": integer | null,
    "frequencyFeedback": string | null
  }} | null,

  "expression": {{
    "parentExpression": string | null,
    "kidExpression": string | null,
    "parentConditions": string | null,
    "kidConditions": string | null,
    "expressionFeedback": string | null
  }} | null,

  "emotion": {{
    "timeline": [
      {{"time": "MM:SS", "momentEmotion": string}}
    ] | [] | null,
    "emotionFeedback": string | null
  }} | null,

  "kidAttitude": string | null,

  "changeProposal": [
    {{"existingExpression": string, "proposalExpression": string}}
  ] | [] | null,

  "pattern": string | null,
  "strength": string | null
}}

[대화 원문]
{dialogue}

[중요]
- 출력은 반드시 위 JSON 형태 하나만. 앞뒤로 설명/코드블록/텍스트 금지.
"""

# System 메시지: JSON만 출력 강제
_SYSTEM_JSON_ONLY = (
    "You are a JSON-only generator. Respond with a single valid JSON object and nothing else. "
    "No markdown, no code fences, no commentary."
)

# 로컬 파일에서 Claude API 키 읽기
def load_claude_key_from_file(path=None):
    if path is None:
        base_dir = os.path.dirname(os.path.abspath(__file__))
        path = os.path.join(base_dir, "keys", "claude_key.txt")
    with open(path, "r", encoding="utf-8") as f:
        return f.read().strip()

# Claude API 요청
def request_claude(prompt: str) -> dict:
    api_key = os.getenv("ANTHROPIC_API_KEY") or load_claude_key_from_file()
    api_url = "https://api.anthropic.com/v1/messages"
    headers = {
        "x-api-key": api_key,
        "anthropic-version": "2023-06-01",
        "Content-Type": "application/json"
    }
    data = {
        "model": "claude-3-haiku-20240307",   # 모델명은 필요 시 바꿀 수 있음
        "max_tokens": 1200,
        "system": _SYSTEM_JSON_ONLY,
        "messages": [{"role": "user", "content": prompt}],
    }

    r = requests.post(api_url, headers=headers, json=data, timeout=120)
    r.raise_for_status()

    blocks = r.json().get("content", [])
    text = "\n".join(b.get("text", "") for b in blocks if isinstance(b, dict)).strip()

    # JSON 파싱
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        # 혹시 모델이 JSON 외 텍스트를 섞어버렸을 경우, 중괄호만 추출
        m = re.search(r"\{.*\}\s*$", text, re.DOTALL)
        if not m:
            raise
        return json.loads(m.group(0))
