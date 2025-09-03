# workbook/main.py
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from workbook.models import SimulateRequest, SimulateResponse, WorkbookActivity
from workbook.run_workbook_simulation import run_workbook_simulation
import os

app = FastAPI(title="Workbook Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 필요 시 프런트 도메인으로 제한
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 데이터 파일 경로 (ocr_paragraphs.csv)
BASE_DIR = os.path.dirname(os.path.abspath(__file__))          # .../python-analyzer/workbook
CSV_PATH = os.path.normpath(os.path.join(BASE_DIR, "../data/ocr_paragraphs.csv"))

@app.post("/workbook/simulate", response_model=SimulateResponse)
def simulate(req: SimulateRequest):
    if not os.path.exists(CSV_PATH):
        raise HTTPException(status_code=500, detail=f"CSV not found: {CSV_PATH}")

    try:
        activities = run_workbook_simulation(
            csv_path=CSV_PATH,
            topic=req.topic,
            user=req.user.model_dump(),   # pydantic → dict
            max_chunks=2                  # 비용/속도 조절
        )
        # pydantic 모델로 변환하여 응답
        typed = [WorkbookActivity(**a) for a in activities]
        return SimulateResponse(activities=typed)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"simulate failed: {e}")
