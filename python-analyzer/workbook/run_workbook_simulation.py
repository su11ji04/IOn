import json, re
from workbook.preprocess_data import load_and_clean_text, split_text
from workbook.prompt_simulation import generate_prompt, generate_simulation

ALLOWED_TYPES = {"MCQ", "WRITING", "SIMULATION"}

# ✅ 마지막 실행 트레이스 전역 변수
LAST_TRACE = {
    "topic": None,
    "user": None,
    "chunk_index": None,
    "prompt_head": "",
    "raw_head": "",
}


def _extract_json(text: str):
    """응답에서 첫 번째 JSON 오브젝트를 안전하게 추출"""
    try:
        return json.loads(text)
    except Exception:
        pass
    m = re.search(r'\{.*\}', text, flags=re.S)
    if not m:
        raise ValueError("No JSON object found in response")
    return json.loads(m.group(0))


def run_workbook_simulation(csv_path: str, topic: str, user: dict, max_chunks: int = 2):
    text = load_and_clean_text(csv_path)
    chunks = split_text(text, max_tokens=1200)

    if not chunks:
        raise ValueError(f"No text chunks from CSV: {csv_path}")

    workbook_activities = []
    for i, chunk in enumerate(chunks[:max_chunks]):
        prompt = generate_prompt(chunk, topic, user)

        # ✅ 트레이스 저장
        LAST_TRACE["topic"] = topic
        LAST_TRACE["user"] = user
        LAST_TRACE["chunk_index"] = i
        LAST_TRACE["prompt_head"] = (prompt or "")[:800]

        raw_output = generate_simulation(prompt)
        LAST_TRACE["raw_head"] = (raw_output or "")[:800]

        try:
            gpt_json = _extract_json(raw_output)
        except Exception as e:
            print(f"[WORKBOOK][ERR] JSON parse failed (chunk {i}): {e}")
            print(f"[WORKBOOK][PROMPT HEAD]\n{prompt[:400]}")
            print(f"[WORKBOOK][RAW HEAD]\n{(raw_output or '')[:400]}")
            raise

        activity_title = gpt_json.get("activity_title") or gpt_json.get("title") or "워크북 활동"
        activities = gpt_json.get("activities") or []

        # 타입/갯수 검증
        types = [a.get("type") for a in activities if isinstance(a, dict)]
        required = {"MCQ", "WRITING", "SIMULATION"}
        if set(types) != required or len(activities) != 3:
            raise ValueError(f"Invalid activities set (need exactly MCQ/WRITING/SIMULATION one each). got={types}")

        workbook_activities.append({
            "activity_title": activity_title,
            "activities": activities
        })

    if not workbook_activities:
        raise ValueError("Engine produced empty activities list")

    return workbook_activities
