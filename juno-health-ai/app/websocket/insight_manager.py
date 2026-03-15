from collections import defaultdict
from fastapi import WebSocket

class InsightConnectionManager:
    def __init__(self):
        self.connections: dict[str, list[WebSocket]] = defaultdict(list)

    async def connect(self, department_id: str, websocket: WebSocket):
        await websocket.accept()
        self.connections[department_id].append(websocket)

    def disconnect(self, department_id: str, websocket: WebSocket):
        if department_id in self.connections:
            self.connections[department_id] = [
                ws for ws in self.connections[department_id] if ws != websocket
            ]
            if not self.connections[department_id]:
                del self.connections[department_id]

    async def broadcast(self, department_id: str, message: dict):
        dead = []
        for ws in self.connections.get(department_id, []):
            try:
                await ws.send_json(message)
            except Exception:
                dead.append(ws)

        for ws in dead:
            self.disconnect(department_id, ws)

insight_manager = InsightConnectionManager()