from datetime import datetime
from typing import List, Optional
from uuid import UUID
from pydantic import BaseModel, Field


# ─────────────────────────────────────────────
# Room snapshot
# ─────────────────────────────────────────────

class AvailableRoomSnapshot(BaseModel):
    roomId: UUID
    roomName: str
    description: Optional[str] = None
    available: bool
    assignedStaffId: Optional[UUID] = None
    assignedStaffName: Optional[str] = None


# ─────────────────────────────────────────────
# Staff snapshot
# ─────────────────────────────────────────────

class AvailableStaffSnapshot(BaseModel):
    staffId: UUID
    fullName: str
    role: str
    specialty: Optional[str] = None
    onDuty: bool
    active: bool


# ─────────────────────────────────────────────
# Patient queue snapshot
# ─────────────────────────────────────────────

class QueuePatientSnapshot(BaseModel):
    queueEntryId: UUID
    visitId: UUID
    patientId: UUID
    patientName: str

    position: int
    priorityTier: str
    aiPriorityScore: Optional[int] = None

    symptomSeverity: Optional[str] = None
    painLevel: Optional[int] = None
    symptomCategories: List[str] = Field(default_factory=list)
    symptomDuration: Optional[str] = None

    presentingComplaint: Optional[str] = None
    additionalNotes: Optional[str] = None

    visitStatus: str

    checkedInAt: datetime
    timeInQueueMinutes: Optional[int] = None
    estimatedWaitMinutes: Optional[int] = None

    lastMovedUpAt: Optional[datetime] = None

    roomName: Optional[str] = None
    assignedStaffName: Optional[str] = None
    assignedStaffRole: Optional[str] = None


# ─────────────────────────────────────────────
# Department snapshot event (Kafka payload)
# ─────────────────────────────────────────────

class DepartmentQueueSnapshotEvent(BaseModel):
    departmentId: UUID
    facilityName: str
    departmentName: str

    eventType: str
    generatedAt: datetime

    queueDepth: int
    averageEstimatedWaitMinutes: int
    departmentCapacity: Optional[int] = None

    availableRooms: List[AvailableRoomSnapshot] = Field(default_factory=list)
    availableStaff: List[AvailableStaffSnapshot] = Field(default_factory=list)

    entries: List[QueuePatientSnapshot] = Field(default_factory=list)