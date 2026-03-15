import logging
from fastapi import APIRouter, Depends, HTTPException

from auth import TokenPayload, get_current_user
from app.schemas.patient_discomfort import PatientDiscomfortPayload
from app.services.gemini_service import evaluate_patient_discomfort
from app.websocket.insight_manager import insight_manager

router = APIRouter()
logger = logging.getLogger("juno-ai-audio")


@router.post("/patient/discomfort")
async def report_discomfort(
        data: PatientDiscomfortPayload,
        user: TokenPayload = Depends(get_current_user),
):
    if user.accountType != "PATIENT":
        raise HTTPException(status_code=403, detail="Patients only")

    try:
        insight = evaluate_patient_discomfort(data.visitId, data.message)

        await insight_manager.broadcast(
            data.departmentId,
            {
                "type": "QUEUE_INSIGHTS",
                "data": {
                    "departmentId": data.departmentId,
                    "source": "PATIENT_REPORT",
                    "insights": [insight],
                },
            },
        )

        logger.info(
            "Patient discomfort report broadcast visit_id=%s dept=%s",
            data.visitId,
            data.departmentId,
        )

        return {"status": "received"}

    except Exception as e:
        logger.exception("Failed to process patient discomfort report")
        raise HTTPException(status_code=500, detail=str(e))


