from pydantic import BaseModel, Field

class PatientDiscomfortPayload(BaseModel):
    visitId: str
    departmentId: str
    message: str = Field(..., min_length=2, max_length=1000)