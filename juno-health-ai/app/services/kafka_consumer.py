import asyncio
import json
import logging
from confluent_kafka import Consumer

from app.core.config import KAFKA_BOOTSTRAP_SERVERS, KAFKA_QUEUE_TOPIC
from app.schemas.queue_snapshot import DepartmentQueueSnapshotEvent
from app.services.gemini_service import generate_queue_insights
from app.services.patient_check_service import trigger_patient_checks  # NEW
from app.websocket.insight_manager import insight_manager

logger = logging.getLogger("juno-ai-audio")

def build_kafka_consumer() -> Consumer:
    return Consumer({
        "bootstrap.servers": KAFKA_BOOTSTRAP_SERVERS,
        "group.id": "juno-ai-insights",
        "auto.offset.reset": "latest",
    })

async def consume_queue_snapshots():
    consumer = build_kafka_consumer()
    consumer.subscribe([KAFKA_QUEUE_TOPIC])
    logger.info("Subscribed to Kafka topic: %s", KAFKA_QUEUE_TOPIC)

    try:
        while True:
            msg = consumer.poll(1.0)

            if msg is None:
                await asyncio.sleep(0.1)
                continue

            if msg.error():
                logger.error("Kafka consumer error: %s", msg.error())
                continue

            try:
                payload = json.loads(msg.value().decode("utf-8"))
                snapshot = DepartmentQueueSnapshotEvent.model_validate(payload)

                insights = generate_queue_insights(snapshot)

                await insight_manager.broadcast(
                    str(snapshot.departmentId),
                    {
                        "type": "QUEUE_INSIGHTS",
                        "data": insights,
                    },
                )

                await trigger_patient_checks(snapshot)  # NEW

                logger.info(
                    "Generated insights for department=%s event=%s depth=%s",
                    snapshot.departmentId,
                    snapshot.eventType,
                    snapshot.queueDepth,
                )

            except Exception:
                logger.exception("Failed to process queue snapshot message")
    finally:
        consumer.close()

async def start_kafka_consumer():
    asyncio.create_task(consume_queue_snapshots())