from pydantic import BaseModel
from typing import Optional, List

class UserProfile(BaseModel):
    child_age: Optional[int] = None
    parenting_style: Optional[str] = None
    parenting_goal: Optional[str] = None
    child_traits: Optional[str] = None
    preferred_tone: Optional[str] = None
    language: Optional[str] = "ko"
    allergies_or_health_issues: Optional[str] = None

class SimulateRequest(BaseModel):
    topic: str
    user: UserProfile

class ActivityItem(BaseModel):
    type: str
    instruction: str
    example_answer: Optional[str] = None
    options: Optional[List[str]] = None
    optimal_option: Optional[str] = None
    situation: Optional[str] = None
    your_response: Optional[str] = None
    ai_optimal_response: Optional[str] = None

class WorkbookActivity(BaseModel):
    activity_title: str
    activities: List[ActivityItem]

class SimulateResponse(BaseModel):
    activities: List[WorkbookActivity]
