#app/services/gemini_service.py
import json
from google.genai import types
from collections import Counter
from app.core.clients import gemini_client
from app.core.config import GEMINI_MODEL
from app.schemas.queue_snapshot import DepartmentQueueSnapshotEvent
from app.core.insight_types import INSIGHT_TYPES, INSIGHT_SEVERITIES
import logging
import re

logger = logging.getLogger("juno-ai-audio")

def extract_json_object(text: str) -> str:
    text = text.strip()

    if text.startswith("{") and text.endswith("}"):
        return text

    fenced = re.search(r"```json\s*(\{.*\})\s*```", text, re.DOTALL)
    if fenced:
        return fenced.group(1)

    start = text.find("{")
    end = text.rfind("}")
    if start != -1 and end != -1 and end > start:
        return text[start:end + 1]

    raise ValueError("No complete JSON object found in Gemini response")

def fallback_insights(snapshot: DepartmentQueueSnapshotEvent) -> dict:
    available_room_count = sum(1 for r in snapshot.availableRooms if r.available)
    on_duty_staff_count = sum(1 for s in snapshot.availableStaff if s.onDuty)

    return {
        "departmentId": str(snapshot.departmentId),
        "facilityName": snapshot.facilityName,
        "departmentName": snapshot.departmentName,
        "eventType": snapshot.eventType,
        "generatedAt": snapshot.generatedAt.isoformat(),
        "queueDepth": snapshot.queueDepth,
        "insights": [
            {
                "type": "STATUS_UPDATE",
                "severity": "MODERATE",
                "title": "Queue snapshot updated",
                "message": (
                    f"{snapshot.queueDepth} active entries, "
                    f"{available_room_count} rooms available, "
                    f"{on_duty_staff_count} staff on duty."
                ),
                "confidence": 0.5,
            }
        ],
    }

def generate_script(prompt: str) -> str:
    response = gemini_client.models.generate_content(
        model=GEMINI_MODEL,
        contents=prompt,
        config=types.GenerateContentConfig(
            temperature=0.45,
            top_p=0.9,
            max_output_tokens=1000,
        ),
    )
    text = (response.text or "").strip()
    if not text:
        raise ValueError("Model returned an empty script")
    return " ".join(text.split())

def build_queue_metrics(snapshot: DepartmentQueueSnapshotEvent) -> dict:
    entries = snapshot.entries
    available_rooms = snapshot.availableRooms
    available_staff = snapshot.availableStaff

    severe_count = sum(1 for e in entries if e.symptomSeverity == "SEVERE")
    emergency_count = sum(1 for e in entries if e.symptomSeverity == "EMERGENCY")
    high_pain_count = sum(
        1 for e in entries
        if e.painLevel is not None and e.painLevel >= 8
    )

    waiting_count = sum(1 for e in entries if e.visitStatus == "CHECKED_IN")
    called_count = sum(1 for e in entries if e.visitStatus == "CALLED")

    symptom_counter = Counter()
    for e in entries:
        symptom_counter.update(e.symptomCategories or [])
    top_symptoms = symptom_counter.most_common(5)

    available_room_count = sum(1 for r in available_rooms if r.available)
    on_duty_staff_count = sum(1 for s in available_staff if s.onDuty)
    active_staff_count = sum(1 for s in available_staff if s.active)

    long_wait_count = sum(
        1 for e in entries
        if e.timeInQueueMinutes is not None and e.timeInQueueMinutes >= 20
    )

    stuck_count = sum(
        1 for e in entries
        if e.timeInQueueMinutes is not None
        and e.lastMovedUpAt is None
        and e.timeInQueueMinutes >= 15
    )

    room_assigned_count = sum(1 for e in entries if e.roomName)
    staff_assigned_count = sum(1 for e in entries if e.assignedStaffName)

    queue_pressure_ratio = None
    if snapshot.departmentCapacity and snapshot.departmentCapacity > 0:
        queue_pressure_ratio = round(snapshot.queueDepth / snapshot.departmentCapacity, 2)

    return {
        "queue_depth": snapshot.queueDepth,
        "average_wait": snapshot.averageEstimatedWaitMinutes,
        "department_capacity": snapshot.departmentCapacity,
        "queue_pressure_ratio": queue_pressure_ratio,
        "severe_count": severe_count,
        "emergency_count": emergency_count,
        "high_pain_count": high_pain_count,
        "waiting_count": waiting_count,
        "called_count": called_count,
        "long_wait_count": long_wait_count,
        "stuck_count": stuck_count,
        "available_room_count": available_room_count,
        "on_duty_staff_count": on_duty_staff_count,
        "active_staff_count": active_staff_count,
        "room_assigned_count": room_assigned_count,
        "staff_assigned_count": staff_assigned_count,
        "top_symptoms": top_symptoms,
    }
    
def build_queue_insight_prompt(snapshot: DepartmentQueueSnapshotEvent) -> str:
    metrics = build_queue_metrics(snapshot)

    entries_lines = []
    for e in snapshot.entries[:25]:
        entries_lines.append(
            (
                f"Position={e.position}; "
                f"PriorityTier={e.priorityTier}; "
                f"Severity={e.symptomSeverity}; "
                f"Pain={e.painLevel}; "
                f"TimeInQueue={e.timeInQueueMinutes}; "
                f"EstimatedWait={e.estimatedWaitMinutes}; "
                f"LastMovedUpAt={e.lastMovedUpAt.isoformat() if e.lastMovedUpAt else 'None'}; "
                f"Status={e.visitStatus}; "
                f"Complaint={e.presentingComplaint or 'None'}; "
                f"Symptoms={', '.join(e.symptomCategories) if e.symptomCategories else 'None'}; "
                f"Room={e.roomName or 'None'}; "
                f"Staff={e.assignedStaffName or 'None'}; "
                f"CheckedInAt={e.checkedInAt.isoformat() if e.checkedInAt else 'None'}"
            )
        )

    rooms_lines = []
    for r in snapshot.availableRooms[:20]:
        rooms_lines.append(
            f"RoomName={r.roomName}; Available={r.available}; AssignedStaff={r.assignedStaffName or 'None'}"
        )
    rooms_block = "\n".join(rooms_lines) if rooms_lines else "No room data"

    staff_lines = []
    for s in snapshot.availableStaff[:20]:
        staff_lines.append(
            f"Name={s.fullName}; Role={s.role}; Specialty={s.specialty or 'None'}; OnDuty={s.onDuty}; Active={s.active}"
        )
    staff_block = "\n".join(staff_lines) if staff_lines else "No staff data"

    entries_block = "\n".join(entries_lines) if entries_lines else "No active entries"

    allowed_types = ", ".join(INSIGHT_TYPES)
    allowed_severities = ", ".join(INSIGHT_SEVERITIES)
    top_symptoms = ", ".join(f"{name}:{count}" for name, count in metrics["top_symptoms"]) or "None"

    return f"""
You are an emergency department operations intelligence assistant.

Your task is to analyze the current queue snapshot and generate useful operational insights for a hospital staff dashboard.

You must choose the most appropriate type for each insight from this allowed set only:
{allowed_types}

You must choose the severity from this allowed set only:
{allowed_severities}

Rules:
1. Return JSON only.
2. Return between 1 and 3 insights.
3. Prefer 2 insights unless the queue strongly supports more.
4. Only include genuinely useful insights for staff.
5. Do not include filler observations that simply repeat obvious raw data.
6. Do not diagnose patients.
7. Do not recommend treatment.
8. Focus on operational, queue, wait-time, flow, staffing, room availability, capacity, symptom-pattern, stagnation, and situational insights.
9. Each insight must be materially different from the others.
10. Keep titles under 5 words.
11. Keep messages under 300 characters.
12. Use natural, human, assistant-like wording rather than robotic dashboard wording.
13. When referring to a specific patient, identify them by queue position, not by name.
14. Do not say "a patient" if queue position is available.
15. Use type "SUGGESTION" only when the queue data supports a practical operational next step.
16. If a patient has been in queue a long time or has not moved up recently, that may support WAIT_RISK, QUEUE_STAGNATION, PATIENT_RISK, or SUGGESTION insights.
17. If rooms or staff are limited, that may support CAPACITY_WARNING, STAFFING_SHORTAGE, BOTTLENECK, FLOW_IMBALANCE, or SUGGESTION insights.
18. Only use allowed types and allowed severities.
19. Do not invent facts not supported by the snapshot.
20. Return one complete JSON object only.
21. Do not include markdown fences.
22. Do not include any text before or after the JSON.
23. If an insight is about a specific patient, include subjectPosition.
24. Suggestions must be cautious and operational, not clinical.

Required JSON format:
{{
  "insights": [
    {{
      "type": "one of the allowed types",
      "severity": "one of the allowed severities",
      "title": "short title",
      "message": "one concise explanation sentence",
      "confidence": 0.0,
      "subjectPosition": 0
    }}
  ]
}}

Use subjectPosition only when the insight refers to a specific patient.

QUEUE SNAPSHOT
Facility: {snapshot.facilityName}
Department: {snapshot.departmentName}
Event type: {snapshot.eventType}
Queue depth: {snapshot.queueDepth}
Average estimated wait minutes: {snapshot.averageEstimatedWaitMinutes}
Department capacity: {snapshot.departmentCapacity if snapshot.departmentCapacity is not None else 'Unknown'}

DERIVED METRICS
Waiting count: {metrics["waiting_count"]}
Called count: {metrics["called_count"]}
Severe count: {metrics["severe_count"]}
Emergency count: {metrics["emergency_count"]}
High pain count: {metrics["high_pain_count"]}
Long wait count (20+ min): {metrics["long_wait_count"]}
Stuck count (15+ min without move-up): {metrics["stuck_count"]}
Available room count: {metrics["available_room_count"]}
On-duty staff count: {metrics["on_duty_staff_count"]}
Active staff count: {metrics["active_staff_count"]}
Assigned room count: {metrics["room_assigned_count"]}
Assigned staff count: {metrics["staff_assigned_count"]}
Queue pressure ratio: {metrics["queue_pressure_ratio"] if metrics["queue_pressure_ratio"] is not None else 'Unknown'}
Top symptoms: {top_symptoms}

AVAILABLE ROOMS
{rooms_block}

AVAILABLE STAFF
{staff_block}

QUEUE ENTRIES
{entries_block}
""".strip()

def generate_queue_insights(snapshot: DepartmentQueueSnapshotEvent) -> dict:
    prompt = build_queue_insight_prompt(snapshot)

    response = gemini_client.models.generate_content(
        model=GEMINI_MODEL,
        contents=prompt,
        config=types.GenerateContentConfig(
            temperature=0.2,
            top_p=0.9,
            max_output_tokens=1000,
            response_mime_type="application/json",
        ),
    )

    text = (response.text or "").strip()
    if not text:
        raise ValueError("Gemini returned empty insights")

    logger.info("Raw Gemini insight response: %s", text)

    try:
        json_text = extract_json_object(text)
        print(json_text)
        parsed = json.loads(json_text)

        if not isinstance(parsed, dict):
            raise ValueError("Gemini response was not a JSON object")

        insights = parsed.get("insights", [])
        if not isinstance(insights, list):
            raise ValueError("'insights' must be a list")

        return {
            "departmentId": str(snapshot.departmentId),
            "facilityName": snapshot.facilityName,
            "departmentName": snapshot.departmentName,
            "eventType": snapshot.eventType,
            "generatedAt": snapshot.generatedAt.isoformat(),
            "queueDepth": snapshot.queueDepth,
            "insights": insights,
        }

    except Exception:
        logger.exception("Failed to parse Gemini insights, using fallback")
        return fallback_insights(snapshot)
    
    
    
def generate_patient_question(entry) -> str:
    prompt = f"""
You are a calm hospital assistant checking on a patient in an emergency queue.

Patient context:
Position: {entry.position}
Time waiting minutes: {entry.timeInQueueMinutes}
Severity: {entry.symptomSeverity}
Pain level: {entry.painLevel}
Symptoms: {", ".join(entry.symptomCategories)}

Generate ONE short check-in question to confirm current condition.

Rules:
1. Must be yes/no or multiple choice.
2. Under 15 words.
3. Sound calm and natural.
4. Do not diagnose or recommend treatment.

Return JSON only:
{{
  "question": "string",
  "type": "YES_NO or MCQ",
  "options": ["only for MCQ"]
}}
""".strip()

    response = gemini_client.models.generate_content(
        model=GEMINI_MODEL,
        contents=prompt,
        config=types.GenerateContentConfig(
            temperature=0.3,
            max_output_tokens=200,
            response_mime_type="application/json",
        ),
    )
    return (response.text or "").strip()


def evaluate_patient_response(entry, answer: str, pending: dict) -> dict:
    prompt = f"""
A patient in an emergency queue answered a check-in question.

Queue position: {entry.position}
Severity: {entry.symptomSeverity}
Pain level: {entry.painLevel}
Time in queue (minutes): {entry.timeInQueueMinutes}
Original question: {pending["question"]}
Patient answer: {answer}

Determine if this response suggests a change in risk or provides a useful operational note.

Rules:
1. Return JSON only.
2. Use type: PATIENT_CHECK_RESULT for routine answers, PATIENT_RISK or DETERIORATION_RISK if concerning.
3. Severity: LOW for stable, MODERATE if unclear, HIGH or CRITICAL if the answer suggests worsening.
4. Keep title under 5 words, message under 200 characters.
5. Always include subjectPosition.

Return JSON only:
{{
  "type": "PATIENT_CHECK_RESULT | PATIENT_RISK | DETERIORATION_RISK",
  "severity": "LOW | MODERATE | HIGH | CRITICAL",
  "title": "short title",
  "message": "one concise sentence",
  "confidence": 0.0,
  "subjectPosition": {entry.position}
}}
""".strip()

    response = gemini_client.models.generate_content(
        model=GEMINI_MODEL,
        contents=prompt,
        config=types.GenerateContentConfig(
            temperature=0.2,
            top_p=0.9,
            max_output_tokens=300,
            response_mime_type="application/json",
        ),
    )

    text = (response.text or "").strip()

    try:
        json_text = extract_json_object(text)
        insight = json.loads(json_text)

        return {
            "departmentId": pending.get("department_id"),
            "facilityName": pending.get("facility_name"),
            "departmentName": pending.get("department_name"),
            "source": "PATIENT_CHECK",
            "insights": [insight],
        }

    except Exception:
        logger.exception("Failed to parse evaluate_patient_response, using fallback")
        return {
            "departmentId": pending.get("department_id"),
            "facilityName": pending.get("facility_name"),
            "departmentName": pending.get("department_name"),
            "source": "PATIENT_CHECK",
            "insights": [
                {
                    "type": "PATIENT_CHECK_RESULT",
                    "severity": "LOW",
                    "title": "Patient check received",
                    "message": f"Position {entry.position} responded: {answer[:120]}",
                    "confidence": 0.5,
                    "subjectPosition": entry.position,
                }
            ],
        }
        

def generate_question_from_intent(intent: str, entry) -> str:
    prompt = f"""
You are a calm hospital assistant. A staff member wants to check on a patient.

Staff intent: {intent}

Patient context:
Position: {entry.position}
Severity: {entry.symptomSeverity}
Pain level: {entry.painLevel}
Symptoms: {", ".join(entry.symptomCategories or [])}
Time in queue (minutes): {entry.timeInQueueMinutes}

Turn the staff intent into ONE short patient-facing check-in question.

Rules:
1. Must be yes/no or multiple choice.
2. Under 15 words.
3. Calm and natural — the patient should not feel alarmed.
4. Do not diagnose or recommend treatment.

Return JSON only:
{{
  "question": "string",
  "type": "YES_NO or MCQ",
  "options": ["only for MCQ"]
}}
""".strip()

    response = gemini_client.models.generate_content(
        model=GEMINI_MODEL,
        contents=prompt,
        config=types.GenerateContentConfig(
            temperature=0.3,
            max_output_tokens=200,
            response_mime_type="application/json",
        ),
    )
    return (response.text or "").strip()

def evaluate_patient_discomfort(visit_id: str, message: str) -> dict:
    prompt = f"""
A patient in an emergency queue has voluntarily reported discomfort or a concern.

Patient visit ID: {visit_id}
Patient message: {message}

Evaluate this message and produce one operational insight for the clinical staff dashboard.

Rules:
1. Return JSON only.
2. Use type PATIENT_RISK or DETERIORATION_RISK if the message suggests worsening condition.
3. Use ESCALATION_REQUIRED if the message suggests immediate attention is needed.
4. Use PATIENT_CHECK_RESULT for general discomfort or minor concerns.
5. Set severity honestly — do not underplay distressing language.
6. Keep title under 5 words, message under 200 characters.
7. Do not diagnose or recommend treatment.
8. Quote key words from the patient's message naturally in the insight message.
9. Always include subjectVisitId.

Return JSON only:
{{
  "type": "PATIENT_RISK | DETERIORATION_RISK | ESCALATION_REQUIRED | PATIENT_CHECK_RESULT",
  "severity": "LOW | MODERATE | HIGH | CRITICAL",
  "title": "short title",
  "message": "one concise sentence summarising what the patient reported",
  "confidence": 0.0,
  "subjectVisitId": "{visit_id}"
}}
""".strip()

    response = gemini_client.models.generate_content(
        model=GEMINI_MODEL,
        contents=prompt,
        config=types.GenerateContentConfig(
            temperature=0.2,
            top_p=0.9,
            max_output_tokens=300,
            response_mime_type="application/json",
        ),
    )

    text = (response.text or "").strip()

    try:
        json_text = extract_json_object(text)
        return json.loads(json_text)
    except Exception:
        logger.exception("Failed to parse discomfort evaluation")
        return {
            "type": "PATIENT_CHECK_RESULT",
            "severity": "MODERATE",
            "title": "Patient reported concern",
            "message": f"Patient submitted: {message[:120]}",
            "confidence": 0.5,
            "subjectVisitId": visit_id,
        }


def build_facility_guidance_prompt(
        facilities: list,
        user_name: str,
        history: list,
        prompt: str,
        symptom_description: str | None,
) -> list[dict]:
    facility_lines = []
    for i, f in enumerate(facilities, 1):
        services = ", ".join(s.name for s in f.services) or "Not listed"
        nhf = "Yes" if f.nhfAccepted else "No"
        facility_lines.append(
            f"{i}. {f.name} ({f.facilityType})\n"
            f"   Address: {f.address}, {f.parish}\n"
            f"   Distance: {f.distanceKm:.1f} km\n"
            f"   Avg wait: {f.avgWaitMinutes:.0f} minutes\n"
            f"   NHF accepted: {nhf}\n"
            f"   Services: {services}\n"
            f"   Phone: {f.phone}"
        )
    facilities_block = "\n\n".join(facility_lines)

    symptom_line = (
        f"Reported symptoms: {symptom_description}"
        if symptom_description
        else "No symptoms provided."
    )

    system = f"""
You are Juno, a calm, helpful medical assistant helping a patient in Jamaica decide
which nearby facility to visit.

Your job is to have a natural back-and-forth conversation, ask clarifying questions
if needed, and ultimately recommend the best facility based on their situation.

Guidelines:
1. Be warm, clear, and concise — responses will be converted to speech.
2. Do not diagnose or prescribe treatment.
3. Consider distance, wait time, NHF acceptance, facility type, and available services.
4. If the patient describes a serious or emergency condition, prioritise urgency over convenience.
5. Ask at most one clarifying question per turn.
6. When you have enough information, give a clear recommendation with a reason.
7. Keep responses under 100 words — they will be read aloud.
8. Never mention internal IDs or technical fields.
9. Address the patient by first name if available.

Patient name: {user_name}
{symptom_line}

NEARBY FACILITIES
{facilities_block}
""".strip()

    messages = [{"role": "system", "content": system}]

    for msg in history:
        messages.append({"role": msg.role, "content": msg.content})

    messages.append({"role": "user", "content": prompt})

    return messages


def generate_facility_guidance(
        facilities: list,
        user_name: str,
        history: list,
        prompt: str,
        symptom_description: str | None,
) -> str:
    messages = build_facility_guidance_prompt(
        facilities, user_name, history, prompt, symptom_description
    )

    # Flatten into a single prompt string for Gemini
    conversation_text = ""
    system_block = ""
    for msg in messages:
        if msg["role"] == "system":
            system_block = msg["content"]
        elif msg["role"] == "user":
            conversation_text += f"\nPatient: {msg['content']}"
        elif msg["role"] == "assistant":
            conversation_text += f"\nJuno: {msg['content']}"

    full_prompt = f"{system_block}\n\nConversation so far:{conversation_text}\n\nJuno:"

    response = gemini_client.models.generate_content(
        model=GEMINI_MODEL,
        contents=full_prompt,
        config=types.GenerateContentConfig(
            temperature=0.4,
            top_p=0.9,
            max_output_tokens=300,
        ),
    )

    text = (response.text or "").strip()
    if not text:
        raise ValueError("Gemini returned empty facility guidance")
    return " ".join(text.split())