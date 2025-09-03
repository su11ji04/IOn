# workbook/utils.py
import os

def load_openai_api_key():
    # 1) 환경변수 우선
    env_key = os.getenv("OPENAI_API_KEY")
    if env_key:
        return env_key.strip()

    # 2) 파일 fallback: 프로젝트 루트/keys/openai_key.txt
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))  # python-analyzer
    filepath = os.path.join(base_dir, "keys", "openai_key.txt")
    with open(filepath, "r", encoding="utf-8") as f:
        return f.read().strip()
