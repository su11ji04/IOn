from openai import OpenAI

# 키 파일 읽기
with open("keys/openai_key.txt", "r", encoding="utf-8") as f:
    api_key = f.read().strip()

print(f"[OPENAI] loaded key prefix={api_key[:8]}..., len={len(api_key)}")

# OpenAI 클라이언트 생성
client = OpenAI(api_key=api_key)

try:
    # 아주 짧은 요청 보내보기
    resp = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[{"role": "user", "content": "ping"}],
        max_tokens=5
    )
    print("[OPENAI] test request OK ✅")
    print("Response:", resp.choices[0].message.content)
except Exception as e:
    print("[OPENAI] test request FAIL ❌")
    print(e)
