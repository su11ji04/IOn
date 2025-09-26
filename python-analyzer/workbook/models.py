from pydantic import BaseModel, Field, constr
from typing import Optional, List, Literal, Dict, Any, Union

# ===== 공용 유저 프로필 =====
class UserProfile(BaseModel):
    child_age: Optional[int] = None
    parenting_style: Optional[str] = None
    parenting_goal: Optional[str] = None
    child_traits: Optional[str] = None
    preferred_tone: Optional[str] = None
    language: Optional[str] = "ko"
    allergies_or_health_issues: Optional[str] = None

# ✅ 클라이언트가 반드시 제공해야 하는 사용자 입력 (user_id 필수)
class UserInput(UserProfile):
    user_id: constr(strip_whitespace=True, min_length=1)

# ===== 시뮬레이션용 =====
class SimulateRequest(BaseModel):
    topic: constr(strip_whitespace=True, min_length=1)
    user: UserInput   # ✅ 반드시 받아야 함 (없으면 422)

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
    ai_optimal_response: Optional[str] = None

class WorkbookActivity(BaseModel):
    activity_title: str
    # 엔진이 아직 dict로 줄 수도 있다면 아래 Union 유지
    activities: Union[List[ActivityItem], List[Dict[str, Any]]]

class SimulateResponse(BaseModel):
    activities: List[WorkbookActivity]

# ===== 단계 진행용 토큰 =====
class SequenceToken(BaseModel):
    id: str
    payload: Dict[str, Any] = Field(default_factory=dict)

# ===== MCQ/WRITING 입력·출력 =====
class McqOut(BaseModel):
    token: SequenceToken
    mcq: Dict[str, Any]  # {question?, instruction, options[], optimal_option}

# ✅ 시퀀스 시작 시에도 사용자 정보를 반드시 받도록 강제
class SequenceStartIn(BaseModel):
    topic: constr(strip_whitespace=True, min_length=1)
    user: UserInput   # ✅ 필수

class SequenceNextWritingIn(BaseModel):
    token: SequenceToken
    selected_option: str

class WritingOut(BaseModel):
    token: SequenceToken
    writing: Dict[str, Any]  # {question?, instruction, example_answer}

# ===== SIM 진행 =====
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
    history: List[Turn] = []
    parent_reply: str

class SimNextOut(BaseModel):
    ai_line: str
    finished: bool = False
    final_feedback: Optional[str] = None

# ===== 최종 피드백 =====
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
    role: Literal["ai","user"]
    text: str

class FeedbackIn(BaseModel):
    topic: str
    mcq: List[McqItem] = []
    writing: List[WritingItem] = []
    sim_history: List[SimTurn] = []

class FeedbackOut(BaseModel):
    overall_comment: str
    tips: List[str] = []