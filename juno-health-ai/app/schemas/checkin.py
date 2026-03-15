from typing import List, Optional
from pydantic import BaseModel, Field, field_validator

class CheckInPayload(BaseModel):
    method: str
    token: str
    presentingComplaint: str = Field(..., min_length=1, max_length=500)
    symptomSeverity: str
    painLevel: int = Field(..., ge=0, le=10)
    symptomCategories: List[str] = Field(default_factory=list)
    symptomDuration: str
    additionalNotes: Optional[str] = Field(default=None, max_length=1000)
    visitType: str

    position: Optional[int] = Field(default=None, ge=1)
    queueDepth: Optional[int] = Field(default=None, ge=0)
    estimatedWaitMinutes: Optional[int] = Field(default=None, ge=0)
    averageWaitMinutes: Optional[int] = Field(default=None, ge=0)

    facilityName: Optional[str] = None
    departmentName: Optional[str] = None
    checkinCode: Optional[str] = None
    checkedInAt: Optional[str] = None

    priorityTier: Optional[str] = None
    aiPriorityScore: Optional[float] = None

    patientFirstName: Optional[str] = None
    preferredName: Optional[str] = None

    @field_validator("method", "symptomSeverity", "symptomDuration", "visitType", mode="before")
    @classmethod
    def normalize_upper(cls, v):
        if isinstance(v, str):
            return v.strip().upper()
        return v

    @field_validator("presentingComplaint", "additionalNotes", "facilityName", "departmentName", mode="before")
    @classmethod
    def strip_strings(cls, v):
        if isinstance(v, str):
            value = v.strip()
            return value or None
        return v