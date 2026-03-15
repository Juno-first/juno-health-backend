import os
from dotenv import load_dotenv

load_dotenv()

GOOGLE_APPLICATION_CREDENTIALS = os.getenv("GOOGLE_APPLICATION_CREDENTIALS", "credentials.json")
GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-2.5-flash-lite")
TTS_VOICE_NAME = os.getenv("TTS_VOICE_NAME", "en-US-Chirp3-HD-Kore")
TTS_LANGUAGE_CODE = os.getenv("TTS_LANGUAGE_CODE", "en-US")
TTS_SPEAKING_RATE = float(os.getenv("TTS_SPEAKING_RATE", "1.0"))

KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
KAFKA_QUEUE_TOPIC = os.getenv("KAFKA_QUEUE_TOPIC", "department.queue.snapshot")