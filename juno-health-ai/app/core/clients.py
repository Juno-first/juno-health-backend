import os
from google import genai
from google.cloud import texttospeech_v1beta1 as texttospeech

from app.core.config import GOOGLE_APPLICATION_CREDENTIALS

os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = GOOGLE_APPLICATION_CREDENTIALS

gemini_client = genai.Client()
tts_client = texttospeech.TextToSpeechClient()