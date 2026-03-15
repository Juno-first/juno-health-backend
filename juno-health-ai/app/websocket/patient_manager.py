from fastapi import WebSocket
import logging

logger = logging.getLogger("juno-ai-audio")

class PatientConnectionManager:
    def __init__(self):
        self.connections: dict[str, WebSocket] = {}

    async def connect(self, visit_id: str, websocket: WebSocket):
        await websocket.accept()
        self.connections[visit_id] = websocket
        logger.info("Patient connected: visit_id=%s", visit_id)

    def disconnect(self, visit_id: str):
        self.connections.pop(visit_id, None)
        logger.info("Patient disconnected: visit_id=%s", visit_id)

    def is_connected(self, visit_id: str) -> bool:
        return visit_id in self.connections

    async def send_to_patient(self, visit_id: str, message: dict):
        ws = self.connections.get(visit_id)
        if not ws:
            return
        try:
            await ws.send_json(message)
        except Exception:
            logger.exception("Failed to send to patient visit_id=%s", visit_id)
            self.disconnect(visit_id)

patient_manager = PatientConnectionManager()