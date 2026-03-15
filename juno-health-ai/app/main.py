#main.py
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.routes.health import router as health_router
from app.routes.audio import router as audio_router
from app.routes.insights import router as insights_router
from app.services.kafka_consumer import start_kafka_consumer
from app.routes.patient_ws import router as patient_ws_router
from app.routes.admin_check import router as admin_check_router

app = FastAPI(title="Juno AI Audio Service", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:5173",
        "http://127.0.0.1:5173",
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
    expose_headers=[
        "X-Generated-Script",
        "X-Audio-Title",
        "X-Queue-Position",
        "X-Estimated-Wait",
        "X-Department-Name",
        "X-Facility-Name",
    ],
)

app.include_router(health_router)
app.include_router(audio_router)
app.include_router(insights_router)
app.include_router(patient_ws_router)
app.include_router(admin_check_router)

@app.on_event("startup")
async def startup_event():
    await start_kafka_consumer()