from google.cloud import texttospeech_v1beta1 as texttospeech

from app.core.clients import tts_client
from app.core.config import TTS_LANGUAGE_CODE, TTS_SPEAKING_RATE, TTS_VOICE_NAME

def synthesize_audio(script: str) -> bytes:
    synthesis_input = texttospeech.SynthesisInput(text=script)

    voice = texttospeech.VoiceSelectionParams(
        language_code=TTS_LANGUAGE_CODE,
        name=TTS_VOICE_NAME,
    )

    audio_config = texttospeech.AudioConfig(
        audio_encoding=texttospeech.AudioEncoding.MP3,
        speaking_rate=TTS_SPEAKING_RATE,
    )

    response = tts_client.synthesize_speech(
        input=synthesis_input,
        voice=voice,
        audio_config=audio_config,
    )

    if not response.audio_content:
        raise ValueError("TTS returned empty audio")

    return response.audio_content