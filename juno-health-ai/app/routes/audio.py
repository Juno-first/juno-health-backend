import io
import logging
from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import StreamingResponse

from auth import TokenPayload, get_current_user
from app.schemas.checkin import CheckInPayload
from app.schemas.queue_update import QueueUpdatePayload
from app.services.prompt_service import build_checkin_prompt, build_queue_update_prompt
from app.services.gemini_service import generate_script
from app.services.tts_service import synthesize_audio

router = APIRouter()
logger = logging.getLogger("juno-ai-audio")

@router.post("/check-in")
async def process_check_in(
    data: CheckInPayload,
    user: TokenPayload = Depends(get_current_user),
):
    try:
        prompt = build_checkin_prompt(data, user)
        script = generate_script(prompt)
        audio_bytes = synthesize_audio(script)

        audio_stream = io.BytesIO(audio_bytes)

        return StreamingResponse(
            audio_stream,
            media_type="audio/mpeg",
            headers={
                "Content-Disposition": 'inline; filename="juno-checkin.mp3"',
                "Cache-Control": "no-store",
                "X-Generated-Script": script,
                "X-Audio-Title": "Juno Check-In Welcome",
                "X-Queue-Position": str(data.position) if data.position is not None else "",
                "X-Estimated-Wait": str(data.estimatedWaitMinutes) if data.estimatedWaitMinutes is not None else "",
                "X-Department-Name": data.departmentName or "",
                "X-Facility-Name": data.facilityName or "",
            },
        )
    except HTTPException:
        raise
    except Exception as e:
        logger.exception("Failed to process check-in audio")
        raise HTTPException(status_code=500, detail=f"Failed to process check-in audio: {str(e)}")

@router.post("/queue-update")
async def queue_update_audio(
    data: QueueUpdatePayload,
    user: TokenPayload = Depends(get_current_user),
):
    try:
        prompt = build_queue_update_prompt(data, user)
        script = generate_script(prompt)
        audio_bytes = synthesize_audio(script)

        audio_stream = io.BytesIO(audio_bytes)

        return StreamingResponse(
            audio_stream,
            media_type="audio/mpeg",
            headers={
                "Content-Disposition": 'inline; filename="juno-queue-update.mp3"',
                "Cache-Control": "no-store",
                "X-Generated-Script": script,
                "X-Audio-Title": "Juno Queue Update",
                "X-Queue-Position": str(data.position),
                "X-Estimated-Wait": str(data.estimatedWaitMinutes),
                "X-Department-Name": data.departmentName or "",
                "X-Facility-Name": data.facilityName or "",
            },
        )
    except HTTPException:
        raise
    except Exception as e:
        logger.exception("Failed to process queue update audio")
        raise HTTPException(status_code=500, detail=f"Failed to process queue update audio: {str(e)}")

@router.post("/patient-update")
async def patient_update_audio(
    data: CheckInPayload,
    user: TokenPayload = Depends(get_current_user),
):
    return await process_check_in(data, user)