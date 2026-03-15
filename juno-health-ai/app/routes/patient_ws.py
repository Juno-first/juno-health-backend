import json
import logging

from fastapi import APIRouter, WebSocket, WebSocketDisconnect, WebSocketException, status

from app.services.gemini_service import evaluate_patient_response
from app.services.patient_check_service import clear_pending_check, get_pending_check
from app.websocket.insight_manager import insight_manager
from app.websocket.patient_manager import patient_manager
from auth import get_current_user_from_ws_token

router = APIRouter()
logger = logging.getLogger("juno-ai-audio")


@router.websocket("/ws/patient/{visit_id}")
async def patient_ws(websocket: WebSocket, visit_id: str):
    token = websocket.query_params.get("token")

    try:
        user = get_current_user_from_ws_token(token)

        if user.accountType != "PATIENT":
            raise WebSocketException(
                code=status.WS_1008_POLICY_VIOLATION,
                reason="Not authorized for patient channel",
            )

        await patient_manager.connect(visit_id, websocket)

        while True:
            raw = await websocket.receive_text()

            try:
                data = json.loads(raw)
            except json.JSONDecodeError:
                continue

            msg_type = data.get("type")

            if msg_type == "CHECK_IN_ANSWER":
                question_id = data.get("questionId")
                answer = data.get("answer", "").strip()

                if not question_id or not answer:
                    continue

                pending = get_pending_check(question_id)
                if not pending:
                    logger.warning("Received answer for unknown questionId=%s", question_id)
                    continue

                try:
                    insight_payload = evaluate_patient_response(
                        pending["entry"], answer, pending
                    )
                    await insight_manager.broadcast(
                        pending["department_id"],
                        {
                            "type": "QUEUE_INSIGHTS",
                            "data": insight_payload,
                        },
                    )
                    clear_pending_check(question_id)
                    await websocket.send_json({"type": "CHECK_IN_ANSWER_ACK"})

                except Exception:
                    logger.exception(
                        "Failed to evaluate patient response questionId=%s", question_id
                    )

    except WebSocketDisconnect:
        patient_manager.disconnect(visit_id)
    except WebSocketException:
        await websocket.close(code=status.WS_1008_POLICY_VIOLATION)
    except Exception:
        patient_manager.disconnect(visit_id)
        await websocket.close(code=status.WS_1011_INTERNAL_ERROR)