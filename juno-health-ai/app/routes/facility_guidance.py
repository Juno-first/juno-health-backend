import io
import logging
from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import StreamingResponse

from auth import TokenPayload, get_current_user
from app.schemas.facility_guidance import FacilityGuidanceRequest
from app.services.gemini_service import generate_facility_guidance
from app.services.tts_service import synthesize_audio

router = APIRouter()
logger = logging.getLogger("juno-ai-audio")


def _resolve_user_name(payload: FacilityGuidanceRequest, user: TokenPayload) -> str:
    if payload.userName and payload.userName.strip():
        return payload.userName.strip().split()[0]
    for attr in ["first_name", "name", "preferred_name"]:
        val = getattr(user, attr, None)
        if isinstance(val, str) and val.strip():
            return val.strip().split()[0]
    return "there"


@router.post("/facility/guidance")
async def facility_guidance(
        data: FacilityGuidanceRequest,
        user: TokenPayload = Depends(get_current_user),
):
    if not data.facilities:
        raise HTTPException(status_code=400, detail="No facilities provided")

    user_name = _resolve_user_name(data, user)

    try:
        text = generate_facility_guidance(
            facilities=data.facilities,
            user_name=user_name,
            history=data.history,
            prompt=data.prompt,
            symptom_description=data.symptomDescription,
        )

        audio_bytes = synthesize_audio(text)
        audio_stream = io.BytesIO(audio_bytes)

        return StreamingResponse(
            audio_stream,
            media_type="audio/mpeg",
            headers={
                "Content-Disposition": 'inline; filename="juno-guidance.mp3"',
                "Cache-Control": "no-store",
                "X-Juno-Text": text,
                "Access-Control-Expose-Headers": "X-Juno-Text",
            },
        )

    except Exception as e:
        logger.exception("Failed to generate facility guidance")
        raise HTTPException(status_code=500, detail=str(e))