# workbook/run_workbook_simulation.py
from workbook.preprocess_data import load_and_clean_text, split_text
from workbook.prompt_simulation import generate_prompt, generate_simulation
import json

def run_workbook_simulation(csv_path: str, topic: str, user: dict, max_chunks: int = 2):
    text = load_and_clean_text(csv_path)
    chunks = split_text(text, max_tokens=1200)
    workbook_activities = []

    for i, chunk in enumerate(chunks[:max_chunks]):
        prompt = generate_prompt(chunk, topic, user)
        try:
            raw_output = generate_simulation(prompt)
            gpt_json = json.loads(raw_output)

            activity = {
                "activity_title": gpt_json["activity_title"],
                "activities": gpt_json["activities"],
            }
            workbook_activities.append(activity)
        except Exception as e:
            # 실패한 청크는 스킵하고 계속
            print(f"[WORKBOOK] GPT 처리 실패 (chunk {i}): {e}")

    return workbook_activities
