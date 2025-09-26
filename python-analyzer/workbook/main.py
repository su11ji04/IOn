# workbook/main.py
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import os

from workbook.models import (
    WorkbookActivity,
    SimulateRequest,
    SimulateResponse,
)
from workbook.settings import CSV_PATH, HAVE_SIM_ENGINE
from workbook.sequence_api import router as sequence_router
from workbook.sim_api import router as sim_router
from workbook.feedback_api import router as feedback_router

app = FastAPI(title="Workbook Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 개발용
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Router
app.include_router(sequence_router)
app.include_router(sim_router)
app.include_router(feedback_router)

# HEALTH CHECK
@app.get("/ping")
def ping():
    return {"ok": True}

# ROUTING CHECK
@app.get("/__routes")
def list_routes():
    return [r.path for r in app.routes]

# WORKBOOK
@app.post("/workbook/simulate", response_model=SimulateResponse)
def simulate(req: SimulateRequest):
    if HAVE_SIM_ENGINE and not os.path.exists(CSV_PATH):
        raise HTTPException(status_code=500, detail=f"CSV not found: {CSV_PATH}")

    if not HAVE_SIM_ENGINE:
        demo = [
            WorkbookActivity(
                activity_title="데모 활동 1",
                activities=[{
                    "type": "MCQ",
                    "instruction": "예시 보기문항",
                    "options": ["A","B","C","D"],
                    "optimal_option": "A"
                }]
            )
        ]
        return SimulateResponse(activities=demo)

    from workbook.run_workbook_simulation import run_workbook_simulation

    try:
        acts = run_workbook_simulation(
            csv_path=CSV_PATH,
            topic=req.topic,
            user=req.user.dict(),   # ✅ dict 변환
            max_chunks=2
        )
    except Exception as e:
        raise HTTPException(500, f"simulate failed: {e}")

    typed = [WorkbookActivity(**a) for a in acts]
    return SimulateResponse(activities=typed)


# ===== 디버그 트레이스 보기 =====
try:
    from workbook.run_workbook_simulation import LAST_TRACE
except Exception:
    LAST_TRACE = {"error": "LAST_TRACE not available"}

@app.get("/__sim/trace")
def sim_trace():
    return LAST_TRACE
