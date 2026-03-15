from app.schemas.checkin import CheckInPayload
from app.schemas.queue_update import QueueUpdatePayload

HIGH_RISK_CATEGORIES = {
    "CHEST_PAIN",
    "DIFFICULTY_BREATHING",
    "BLEEDING",
    "ALLERGIC_REACTION",
}

HIGH_RISK_SEVERITIES = {"SEVERE", "EMERGENCY"}


def safe_name(payload: CheckInPayload, user) -> str:
    for candidate in [
        payload.preferredName,
        payload.patientFirstName,
        getattr(user, "first_name", None),
        getattr(user, "name", None),
    ]:
        if isinstance(candidate, str) and candidate.strip():
            return candidate.strip().split()[0]
    return "there"


def is_high_risk(payload: CheckInPayload) -> bool:
    severity_is_high = payload.symptomSeverity in HIGH_RISK_SEVERITIES
    category_is_high = any(cat in HIGH_RISK_CATEGORIES for cat in payload.symptomCategories)
    pain_is_high = payload.painLevel >= 8
    return severity_is_high or category_is_high or pain_is_high


def build_context_block(payload: CheckInPayload, user) -> str:
    lines = [
        f"Patient first name: {safe_name(payload, user)}",
        f"Presenting complaint: {payload.presentingComplaint}",
        f"Symptom severity: {payload.symptomSeverity}",
        f"Pain level: {payload.painLevel}/10",
        f"Symptom categories: {', '.join(payload.symptomCategories) if payload.symptomCategories else 'Not specified'}",
        f"Symptom duration: {payload.symptomDuration}",
        f"Visit type: {payload.visitType}",
        f"Additional notes: {payload.additionalNotes or 'None'}",
        f"Facility name: {payload.facilityName or 'Not provided'}",
        f"Department name: {payload.departmentName or 'Not provided'}",
        f"Queue position: {payload.position if payload.position is not None else 'Unknown'}",
        f"Queue depth: {payload.queueDepth if payload.queueDepth is not None else 'Unknown'}",
        f"Estimated wait minutes: {payload.estimatedWaitMinutes if payload.estimatedWaitMinutes is not None else 'Unknown'}",
        f"Average wait minutes: {payload.averageWaitMinutes if payload.averageWaitMinutes is not None else 'Unknown'}",
        f"Priority tier: {payload.priorityTier or 'Unknown'}",
        f"AI priority score: {payload.aiPriorityScore if payload.aiPriorityScore is not None else 'Unknown'}",
        f"Check-in code: {payload.checkinCode or 'Not provided'}",
        f"Checked in at: {payload.checkedInAt or 'Not provided'}",
        f"Method: {payload.method}",
        f"High-risk case: {'YES' if is_high_risk(payload) else 'NO'}",
    ]
    return "\n".join(lines)


def build_checkin_prompt(payload: CheckInPayload, user) -> str:
    first_name = safe_name(payload, user)
    high_risk = is_high_risk(payload)

    return f"""
You are Juno, a calm, empathetic AI medical queue assistant speaking directly to a patient who has just checked in.

PATIENT CONTEXT
{build_context_block(payload, user)}

TASK
Write one short spoken welcome message that will be converted to speech and played immediately after check-in.

REQUIREMENTS
1. Keep it to 2 to 4 short sentences.
2. Speak directly to the patient in a warm, calm, reassuring tone.
3. Use the patient's first name only if available: "{first_name}".
4. If queue position and wait time are available, mention them naturally.
5. If department or facility name is available, mention it naturally.
6. If the case is high-risk ({'YES' if high_risk else 'NO'}), calmly say that the care team is being alerted or prioritised and ask the patient to remain seated and close to staff.
7. Do not diagnose.
8. Do not mention AI, model names, internal scoring, or backend systems.
9. Do not use markdown, bullet points, emojis, or special formatting.
10. Output plain text only, suitable for speech synthesis.
""".strip()


def build_queue_update_prompt(payload: QueueUpdatePayload, user) -> str:
    first_name = getattr(user, "first_name", None) or getattr(user, "name", None) or "there"
    first_name = first_name.strip().split()[0] if isinstance(first_name, str) and first_name.strip() else "there"

    return f"""
You are Juno, a calm, clear medical queue assistant speaking directly to a patient already checked into the emergency queue.

PATIENT FIRST NAME
{first_name}

QUEUE UPDATE CONTEXT
Event type: {payload.eventType}
Facility name: {payload.facilityName}
Department name: {payload.departmentName}
Queue position: {payload.position}
Queue depth: {payload.queueDepth}
Estimated wait minutes: {payload.estimatedWaitMinutes}
Check-in code: {payload.checkinCode}
Priority tier: {payload.priorityTier}
Room name: {payload.roomName or 'Not assigned'}
Assigned staff name: {payload.assignedStaffName or 'Not assigned'}
Assigned staff role: {payload.assignedStaffRole or 'Not assigned'}

TASK
Write one short spoken queue update message for text-to-speech.

REQUIREMENTS
1. Keep it to 1 to 3 short sentences.
2. Use a calm, reassuring, natural tone.
3. Speak directly to the patient.
4. If the event type is CALLED, clearly tell the patient it is their turn and where to go if room information is available.
5. If staff information is available, mention it naturally.
6. Do not diagnose.
7. Do not mention AI, models, internal systems, or scoring.
8. Do not use markdown, bullet points, emojis, or special formatting.
9. Output plain text only, suitable for speech synthesis.
""".strip()


def evaluate_patient_response(entry, answer):

    prompt = f"""
A patient in an emergency queue responded to a check-in question.

Queue position: {entry.position}
Severity: {entry.symptomSeverity}
Pain level: {entry.painLevel}

Patient answer: {answer}

Determine if this response suggests increased risk.

Return JSON:

{{
 "risk": "LOW | MODERATE | HIGH",
 "insight": "short explanation"
}}
"""