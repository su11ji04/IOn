from pydantic import BaseModel, Field, constr
from typing import Optional, List, Literal, Dict, Any, Union

# USER INFORMATION
class UserProfile(BaseModel):
    child_age: Optional[int] = None
    parenting_style: Optional[str] = None
    parenting_goal: Optional[str] = None
    child_traits: Optional[str] = None
    preferred_tone: Optional[str] = None
    language: Optional[str] = "ko"
    allergies_or_health_issues: Optional[str] = None

class UserInput(UserProfile):
    user_id: constr(strip_whitespace=True, min_length=1)

class SimulateRequest(BaseModel):
    topic: constr(strip_whitespace=True, min_length=1)
    user: UserInput

ActivityType = Literal["MCQ", "WRITING", "SIMULATION"]

class ActivityItem(BaseModel):
    type: ActivityType
    instruction: str
    # MCQ
    options: Optional[List[str]] = None
    optimal_option: Optional[str] = None
    # WRITING
    example_answer: Optional[str] = None
    # SIM
    situation: Optional[str] = None
    ai_first_line: Optional[str] = None

class WorkbookActivity(BaseModel):
    activity_title: str
    activities: Union[List[ActivityItem], List[Dict[str, Any]]]

class SimulateResponse(BaseModel):
    activities: List[WorkbookActivity]

class SequenceToken(BaseModel):
    id: str
    payload: Dict[str, Any] = Field(default_factory=dict)

class CreateWorkbookIn(BaseModel):
    topic: constr(strip_whitespace=True, min_length=1)
    user: UserInput

class CreateWorkbookOut(WorkbookActivity):
    pass

class SequenceStartIn(BaseModel):
    activity: Optional[WorkbookActivity] = None
    topic: Optional[constr(strip_whitespace=True, min_length=1)] = None
    user: Optional[UserInput] = None

class McqOut(BaseModel):
    token: SequenceToken
    mcq: Dict[str, Any]

class SequenceNextWritingIn(BaseModel):
    token: SequenceToken
    selected_option: str

class WritingOut(BaseModel):
    token: SequenceToken
    writing: Dict[str, Any]

class Turn(BaseModel):
    role: Literal["ai", "user"]
    text: str

class SimStartIn(BaseModel):
    token: SequenceToken
    writing_answer: str

class SimStartOut(BaseModel):
    token: SequenceToken
    situation: str
    ai_first_line: str

class SimNextIn(BaseModel):
    topic: str
    situation: str
    history: List[Turn] = Field(default_factory=list)
    parent_reply: str

class SimNextOut(BaseModel):
    ai_line: str
    finished: bool = False
    final_feedback: Optional[str] = None

# ---- Final Feedback DTOs ----
class McqItem(BaseModel):
    question: Optional[str] = None
    selected: Optional[str] = None
    optimal: Optional[str] = None
    correct: Optional[bool] = None

class WritingItem(BaseModel):
    question: Optional[str] = None
    answer: Optional[str] = None
    example: Optional[str] = None

class SimTurn(BaseModel):
    role: Literal["ai", "user"]
    text: str

class FeedbackIn(BaseModel):
    topic: str
    mcq: List[McqItem] = Field(default_factory=list)
    writing: List[WritingItem] = Field(default_factory=list)
    sim_history: List[SimTurn] = Field(default_factory=list)

class FeedbackOut(BaseModel):
    overall_comment: str
    tips: List[str] = Field(default_factory=list)

# ---- Facade/Pipeline DTOs ----
class PipelineIn(BaseModel):
    topic: constr(strip_whitespace=True, min_length=1)
    user: UserInput
    mcq_selected: Optional[str] = None
    writing_answer: Optional[str] = None
    parent_replies: List[str] = Field(default_factory=list)

class PipelineOut(BaseModel):
    token: SequenceToken
    mcq: dict
    writing: dict
    sim_history: List[Turn]
    finished: bool
    feedback: FeedbackOut

class SimulateStartSimIn(BaseModel):
    token: SequenceToken
    writing_answer: constr(strip_whitespace=True, min_length=1)

class SimulateNextWritingIn(BaseModel):
    token: SequenceToken
    selected_option: str

class SimulateNextTurnIn(BaseModel):
    topic: str
    situation: str
    history: List[Turn] = Field(default_factory=list)
    parent_reply: constr(strip_whitespace=True, min_length=1)
