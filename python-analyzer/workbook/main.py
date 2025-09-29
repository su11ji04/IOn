from typing import List
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from workbook.simulation_feedback import run_feedback_core

# ROUTER
from workbook.sequence_api import (
    sequence_start,
    sequence_next_writing,
    sim_start,
    sim_next,
    create_workbook,
    router as sequence_router,
)
from workbook.simulation_feedback import feedback, router as feedback_router

# DTO
from workbook.models import (
    UserInput,
    SequenceStartIn, McqOut,
    SequenceNextWritingIn, WritingOut,
    SimStartIn, SimStartOut,
    SimNextIn, SimNextOut,
    FeedbackIn, FeedbackOut,
    Turn,
    PipelineIn, PipelineOut,
    SimulateStartSimIn, SimulateNextWritingIn, SimulateNextTurnIn,
    CreateWorkbookIn,
)

# [개발용] TRACE
try:
    from workbook.activity_builder import LAST_TRACE
except Exception:
    LAST_TRACE = {"error": "LAST_TRACE not available"}

app = FastAPI(title="Workbook Service")

# CORS (개발용)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 라우터 노출
app.include_router(sequence_router)
app.include_router(feedback_router)

# Health
@app.get("/ping")
def ping():
    return {"ok": True}

# Router 확인
@app.get("/__routes")
def list_routes():
    return [r.path for r in app.routes]

@app.get("/__sim/trace")
def sim_trace():
    return LAST_TRACE

@app.post("/simulate/start", response_model=McqOut)
def simulate_start(req: SequenceStartIn):
    try:
        return sequence_start(req)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(500, f"simulate_start failed: {e}")

@app.post("/simulate/next/writing", response_model=WritingOut)
def simulate_next_writing_facade(req: SimulateNextWritingIn):
    try:
        return sequence_next_writing(SequenceNextWritingIn(token=req.token, selected_option=req.selected_option))
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(500, f"simulate_next_writing failed: {e}")

@app.post("/simulate/sim/start", response_model=SimStartOut)
def simulate_sim_start(req: SimulateStartSimIn):
    try:
        return sim_start(SimStartIn(token=req.token, writing_answer=req.writing_answer))
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(500, f"simulate_sim_start failed: {e}")

@app.post("/simulate/sim/next", response_model=SimNextOut)
def simulate_sim_next(req: SimulateNextTurnIn):
    try:
        return sim_next(SimNextIn(topic=req.topic, situation=req.situation, history=req.history, parent_reply=req.parent_reply))
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(500, f"simulate_sim_next failed: {e}")

@app.post("/simulate/feedback", response_model=FeedbackOut)
def simulate_feedback(req: FeedbackIn):
    try:
        return run_feedback_core(req)
    except Exception as e:
        raise HTTPException(500, f"simulate_feedback failed: {e}")

@app.post("/simulate/pipeline", response_model=PipelineOut)
def workbook_pipeline(req: PipelineIn):
    try:
        # 1) 워크북 생성
        create_out = create_workbook(CreateWorkbookIn(topic=req.topic, user=req.user))

        # 2) MCQ 시작
        mcq_out = sequence_start(SequenceStartIn(activity=create_out))
        token = mcq_out.token
        mcq_payload = mcq_out.mcq

        selected = req.mcq_selected or mcq_payload.get("optimal_option")
        writing_out = sequence_next_writing(SequenceNextWritingIn(token=token, selected_option=selected))

        # 3) Writing
        writing_payload = writing_out.writing
        writing_answer = req.writing_answer or writing_payload.get("example_answer") or "예시 답변"

        sim_start_out = sim_start(SimStartIn(token=token, writing_answer=writing_answer))

        # 4) Simulation start
        situation = sim_start_out.situation
        ai_first = sim_start_out.ai_first_line
        sim_hist: List[Turn] = [Turn(role="ai", text=ai_first)]
        finished = False

        for pr in (req.parent_replies or [])[:2]:
            nxt = sim_next(SimNextIn(topic=req.topic, situation=situation, history=sim_hist, parent_reply=pr))
            sim_hist.append(Turn(role="user", text=pr))
            if nxt.finished:
                finished = True
                break
            if nxt.ai_line:
                sim_hist.append(Turn(role="ai", text=nxt.ai_line))

        # 5) Feedback
        fb = feedback(
            FeedbackIn(
                topic=req.topic,
                mcq=[{
                    "question": mcq_payload.get("instruction"),
                    "selected": selected,
                    "optimal": mcq_payload.get("optimal_option"),
                    "correct": selected == mcq_payload.get("optimal_option"),
                }],
                writing=[{
                    "question": writing_payload.get("instruction"),
                    "answer": writing_answer,
                    "example": writing_payload.get("example_answer"),
                }],
                sim_history=[{"role": t.role, "text": t.text} for t in sim_hist],
            )
        )

        return PipelineOut(token=token, mcq=mcq_payload, writing=writing_payload, sim_history=sim_hist, finished=finished, feedback=fb)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(500, f"simulate_pipeline failed: {e}")
