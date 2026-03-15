import logging
from fastapi import APIRouter, Depends, HTTPException

from auth import TokenPayload, get_current_user
from app.schemas.admin_check import AdminCheckRequest
from app.schemas.queue_snapshot import QueuePatientSnapshot
from app.services.patient_check_service import trigger_manual_check
from app.websocket.patient_manager import patient_manager

router = APIRouter()
logger = logging.getLogger("juno-ai-audio")


@router.post("/admin/patient-check/{visit_id}")
async def admin_trigger_patient_check(
    visit_id: str,
    data: AdminCheckRequest,
    user: TokenPayload = Depends(get_current_user),
):
    if user.accountType != "STAFF":
        raise HTTPException(status_code=403, detail="Staff only")

    if not patient_manager.is_connected(visit_id):
        raise HTTPException(
            status_code=409,
            detail="Patient is not currently connected — cannot deliver question",
        )

    # The route needs a minimal entry object to pass context to the AI.
    # In a real setup you'd fetch the live queue entry from your queue service/cache.
    # For now we construct a lightweight stand-in from what the admin sent.
    class _Entry:
        position = None
        symptomSeverity = "UNKNOWN"
        painLevel = None
        symptomCategories = []
        timeInQueueMinutes = None
        queueEntryId = visit_id

    try:
        question_id = await trigger_manual_check(
            visit_id=visit_id,
            entry=_Entry(),
            department_id=data.departmentId,
            department_name="",
            facility_name="",
            question_override=data.question,
            intent=data.intent,
        )
        return {"status": "sent", "questionId": question_id}

    except Exception as e:
        logger.exception("Admin check failed for visit_id=%s", visit_id)
        raise HTTPException(status_code=500, detail=str(e))