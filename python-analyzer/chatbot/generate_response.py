from openai import OpenAI

def load_api_key_from_file(path="keys/openai_key.txt"):
    with open(path, "r", encoding="utf-8") as f:
        k = f.read().strip()
    print(f"[OPENAI] loaded key prefix={k[:8]}..., len={len(k)}")  # 임시 로그
    return k

api_key = load_api_key_from_file()
client = OpenAI(api_key=api_key)

def generate_chat_response(question: str, combined_context: str, user_info: dict) -> str:
    prompt = f"""
당신은 초보 부모를 위해 육아 관련 정보를 제공하는 조언형 챗봇입니다.

사용자 정보:
- 자녀 나이: {user_info["child_age"]}세
- 양육 스타일: {user_info["parenting_style"]}
- 양육 목표: {user_info["parenting_goal"]}
- 자녀 성향: {user_info["child_traits"]}
- 선호 말투: {user_info["preferred_tone"]}
- 언어: {user_info["language"]}
- 건강 이슈: {user_info["health_issues"]}

사용자의 질문:
"{question}"

참고할 문서 일부:
{combined_context}

다음 기준을 꼭 지켜주세요:
이 문서와 사용자 정보를 바탕으로 다음 조건을 반영하여 답변을 생성해주세요:

1. 사용자 정보를 충분히 반영한 맞춤형 조언을 제공합니다.
   가능하면 답변 중에 자녀의 성향과 부모의 양육 목표에 맞는 방향으로 조언을 구성해주세요.

2. 단정적이거나 강제적인 표현은 피합니다.
   예) "반드시", "해야 합니다" 대신 "도움이 될 수 있습니다", "고려해볼 수 있습니다" 등

3. 전문적인 진단이나 치료 방법은 제시하지 않습니다.
   필요시, 전문가의 상담이나 진료가 필요하다는 안내를 부드럽게 덧붙여주세요.

4. 사용자에게 공감하는 어투로 따뜻하게 말해주세요.

5. 실제 문서 내용을 바탕으로 신뢰할 수 있는 제안을 해주세요.
   문서에서 다룬 정보는 자연스럽게 녹여내되, 직접 인용하지 않고 사용자 친화적으로 설명해주세요.

이 모든 기준을 반영하여, 사용자와 자녀의 특성을 고려한 따뜻하고 신뢰할 수 있는 조언을 제공해주세요.
"""

    response = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[
            {"role": "system", "content": "당신은 육아 상담 챗봇입니다."},
            {"role": "user", "content": prompt}
        ]
    )

    return response.choices[0].message.content