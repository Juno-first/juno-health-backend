from fastapi import APIRouter, WebSocket, WebSocketDisconnect, WebSocketException, status
from app.websocket.insight_manager import insight_manager
from auth import get_current_user_from_ws_token

router = APIRouter()

@router.websocket("/ws/insights/{department_id}")
async def insights_ws(websocket: WebSocket, department_id: str):
    token = websocket.query_params.get("token")

    try:
        user = get_current_user_from_ws_token(token)

        # Optional authorization checks
        # Example: only staff can subscribe to department insights
        if user.accountType != "STAFF":
            raise WebSocketException(
                code=status.WS_1008_POLICY_VIOLATION,
                reason="Not authorized for department insights",
            )

        await insight_manager.connect(department_id, websocket)

        while True:
            await websocket.receive_text()

    except WebSocketDisconnect:
        insight_manager.disconnect(department_id, websocket)
    except WebSocketException:
        await websocket.close(code=status.WS_1008_POLICY_VIOLATION)
    except Exception:
        await websocket.close(code=status.WS_1011_INTERNAL_ERROR)