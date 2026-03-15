from typing import Optional
from pydantic import BaseModel, model_validator

class AdminCheckRequest(BaseModel):
    departmentId: str
    question: Optional[str] = None   # use directly, skips AI generation
    intent: Optional[str] = None     # AI generates the question from this

    @model_validator(mode="after")
    def require_one(self):
        if not self.question and not self.intent:
            raise ValueError("Provide either 'question' or 'intent'")
        return self