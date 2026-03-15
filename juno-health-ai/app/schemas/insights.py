from typing import List, Literal
from pydantic import BaseModel, Field

InsightType = Literal[
    "CRITICAL",
    "PATIENT_RISK",
    "DETERIORATION_RISK",
    "ESCALATION_REQUIRED",
    "WAIT_RISK",
    "QUEUE_GROWTH",
    "QUEUE_SURGE",
    "QUEUE_STAGNATION",
    "CAPACITY_WARNING",
    "CAPACITY_AVAILABLE",
    "THROUGHPUT_DROP",
    "EFFICIENCY_GAIN",
    "STAFFING_SHORTAGE",
    "STAFFING_AVAILABLE",
    "ROOM_UTILIZATION",
    "ROOM_BLOCKED",
    "PATTERN",
    "SYMPTOM_CLUSTER",
    "TIME_PATTERN",
    "SURGE_PREDICTION",
    "QUEUE_COLLAPSE_RISK",
    "DELAY_FORECAST",
    "FLOW_IMBALANCE",
    "TRANSFER_OPPORTUNITY",
    "BOTTLENECK",
    "INSIGHT",
    "STATUS_UPDATE",
    "TREND",
    "SUGGESTION",
    "PATIENT_CHECK_RESULT",
    "PATIENT_CHECK_REQUEST"
    
]

InsightSeverity = Literal["LOW", "MODERATE", "HIGH", "CRITICAL"]


class QueueInsight(BaseModel):
    type: InsightType
    severity: InsightSeverity
    title: str = Field(..., min_length=3, max_length=80)
    message: str = Field(..., min_length=5, max_length=200)
    confidence: float = Field(..., ge=0.0, le=1.0)


class QueueInsightEnvelope(BaseModel):
    insights: List[QueueInsight] = Field(default_factory=list)