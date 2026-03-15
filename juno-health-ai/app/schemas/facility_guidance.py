from typing import Optional
from pydantic import BaseModel


class FacilityService(BaseModel):
    name: str


class NearbyFacilityInput(BaseModel):
    id: str
    name: str
    description: str
    facilityType: str
    address: str
    parish: str
    latitude: float
    longitude: float
    phone: str
    nhfAccepted: bool
    avgWaitMinutes: float
    services: list[FacilityService]
    distanceKm: float


class ConversationMessage(BaseModel):
    role: str   # "user" or "assistant"
    content: str


class FacilityGuidanceRequest(BaseModel):
    facilities: list[NearbyFacilityInput]
    prompt: str
    history: list[ConversationMessage] = []

    # optional — if not provided, extracted from token
    userName: Optional[str] = None
    symptomDescription: Optional[str] = None