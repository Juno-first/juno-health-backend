from typing import Optional
from pydantic import BaseModel, Field, field_validator

class QueueUpdatePayload(BaseModel):
    eventType: str
    facilityName: str
    departmentName: str
    position: int = Field(..., ge=1)
    queueDepth: int = Field(..., ge=0)
    estimatedWaitMinutes: int = Field(..., ge=0)
    checkinCode: str
    priorityTier: str
    roomName: Optional[str] = None
    assignedStaffName: Optional[str] = None
    assignedStaffRole: Optional[str] = None

    @field_validator("eventType", "priorityTier", mode="before")
    @classmethod
    def normalize_upper(cls, v):
        if isinstance(v, str):
            return v.strip().upper()
        return v

    @field_validator(
        "facilityName",
        "departmentName",
        "checkinCode",
        "roomName",
        "assignedStaffName",
        "assignedStaffRole",
        mode="before",
    )
    @classmethod
    def strip_strings(cls, v):
        if isinstance(v, str):
            value = v.strip()
            return value or None
        return v