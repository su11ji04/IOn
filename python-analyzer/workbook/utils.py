# workbook/utils.py
import os
import pathlib

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
CSV_PATH = os.path.normpath(os.path.join(BASE_DIR, "../data/ocr_paragraphs.csv"))

# OPENAI KEY LOAD
def load_openai_api_key() -> str:
    # 환경 변수
    env = os.getenv("OPENAI_API_KEY")
    if env and env.strip():
        return env.strip()

    # CSV FILE
    p = pathlib.Path(__file__).resolve().parents[1] / "keys" / "openai_key.txt"
    if p.exists():
        key = p.read_text(encoding="utf-8").strip()
        if key:
            return key

    # ERROR_
    raise RuntimeError(
        "OPENAI_API_KEY not found. Set env var or create keys/openai_key.txt"
    )