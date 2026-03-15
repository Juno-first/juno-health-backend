import asyncio
import base64
import json
import logging
import re
import uuid
from datetime import datetime, timedelta

from app.schemas.queue_snapshot import DepartmentQueueSnapshotEvent, QueuePatientSnapshot
from app.services.gemini_service import generate_patient_question
from app.services.tts_service import synthesize_audio
from app.websocket.patient_manager import patient_manager

logger = logging.getLogger("juno-ai-audio")

# How long before we re-check the same patient
CHECK_COOLDOWN_MINUTES = 10

# In-memory state — swap for Redis if you need multi-instance
_last_check: dict[str, datetime] = {}       # keyed by str(queueEntryId)
_pending_checks: dict[str, dict] = {}        # keyed by questionId


def should_check_patient(entry: QueuePatientSnapshot) -> bool:
    entry_key = str(entry.queueEntryId)

    if entry_key in _last_check:
        if datetime.utcnow() - _last_check[entry_key] < timedelta(minutes=CHECK_COOLDOWN_MINUTES):
            return False

    is_severe = entry.symptomSeverity in ("SEVERE", "EMERGENCY")
    high_pain = entry.painLevel is not None and entry.painLevel >= 7
    long_wait = entry.timeInQueueMinutes is not None and entry.timeInQueueMinutes >= 20
    stuck = (
        entry.timeInQueueMinutes is not None
        and entry.timeInQueueMinutes >= 15
        and entry.lastMovedUpAt is None
    )

    return is_severe or high_pain or long_wait or stuck


def _parse_question_response(raw: str) -> dict:
    clean = raw.strip()
    if clean.startswith("```"):
        clean = re.sub(r"```json|```", "", clean).strip()
    return json.loads(clean)


async def _send_check_to_patient(
    entry: QueuePatientSnapshot,
    department_id: str,
    department_name: str,
    facility_name: str,
):
    try:
        raw = generate_patient_question(entry)
        question_data = _parse_question_response(raw)

        question_id = str(uuid.uuid4())
        visit_id = str(entry.visitId)

        audio_bytes = synthesize_audio(question_data["question"])
        audio_b64 = base64.b64encode(audio_bytes).decode("utf-8")

        _pending_checks[question_id] = {
            "entry": entry,
            "department_id": department_id,
            "department_name": department_name,
            "facility_name": facility_name,
            "question": question_data["question"],
            "asked_at": datetime.utcnow(),
        }
        _last_check[str(entry.queueEntryId)] = datetime.utcnow()

        await patient_manager.send_to_patient(visit_id, {
            "type": "CHECK_IN_QUESTION",
            "questionId": question_id,
            "question": question_data["question"],
            "questionType": question_data.get("type", "YES_NO"),
            "options": question_data.get("options"),
            "audioBase64": audio_b64,
        })

        logger.info(
            "Sent check-in question to position=%s visit_id=%s",
            entry.position, visit_id,
        )

    except Exception:
        logger.exception(
            "Failed to send check-in question to position=%s", entry.position
        )


async def trigger_patient_checks(snapshot: DepartmentQueueSnapshotEvent):
    department_id = str(snapshot.departmentId)
    for entry in snapshot.entries:
        visit_id = str(entry.visitId)
        if patient_manager.is_connected(visit_id) and should_check_patient(entry):
            asyncio.create_task(
                _send_check_to_patient(
                    entry,
                    department_id,
                    snapshot.departmentName,
                    snapshot.facilityName,
                )
            )


def get_pending_check(question_id: str) -> dict | None:
    return _pending_checks.get(question_id)


def clear_pending_check(question_id: str):
    _pending_checks.pop(question_id, None)
    
    
    
async def trigger_manual_check(
    visit_id: str,
    entry,
    department_id: str,
    department_name: str,
    facility_name: str,
    question_override: str | None = None,
    intent: str | None = None,
):
    """
    Called by the admin POST endpoint. Either uses a direct question from the
    admin or generates one from their stated intent.
    """
    from app.services.gemini_service import generate_question_from_intent

    try:
        if question_override:
            # Admin provided the question verbatim — just TTS it
            question_data = {
                "question": question_override,
                "type": "YES_NO",
                "options": None,
            }
        elif intent:
            raw = generate_question_from_intent(intent, entry)
            question_data = _parse_question_response(raw)
        else:
            raise ValueError("question_override or intent must be provided")

        question_id = str(uuid.uuid4())

        audio_bytes = synthesize_audio(question_data["question"])
        audio_b64 = base64.b64encode(audio_bytes).decode("utf-8")

        _pending_checks[question_id] = {
            "entry": entry,
            "department_id": department_id,
            "department_name": department_name,
            "facility_name": facility_name,
            "question": question_data["question"],
            "asked_at": datetime.utcnow(),
        }
        # Reset cooldown so this manual check always goes through
        _last_check[str(entry.queueEntryId)] = datetime.utcnow()

        await patient_manager.send_to_patient(visit_id, {
            "type": "CHECK_IN_QUESTION",
            "questionId": question_id,
            "question": question_data["question"],
            "questionType": question_data.get("type", "YES_NO"),
            "options": question_data.get("options"),
            "audioBase64": audio_b64,
        })

        logger.info(
            "Manual check sent to position=%s visit_id=%s by admin",
            entry.position, visit_id,
        )
        return question_id

    except Exception:
        logger.exception("Failed to send manual check to visit_id=%s", visit_id)
        raise