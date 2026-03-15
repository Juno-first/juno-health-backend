from fastapi import Header, HTTPException, WebSocketException, status
from jose import jwt, JWTError
from pydantic import BaseModel
from typing import Optional
from dotenv import load_dotenv
import os

load_dotenv()

JWT_SECRET = os.getenv("JWT_SECRET")
JWT_ALGORITHM = os.getenv("JWT_ALGORITHM", "HS256")


class TokenPayload(BaseModel):
    sub: str
    accountType: Optional[str] = None
    staffRole: Optional[str] = None
    exp: int


def decode_token(token: str) -> TokenPayload:
    try:
        payload = jwt.decode(
            token,
            JWT_SECRET,
            algorithms=[JWT_ALGORITHM],
        )
        return TokenPayload(**payload)
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid or expired token")


def get_current_user(authorization: str = Header(...)) -> TokenPayload:
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid authorization header")

    token = authorization.replace("Bearer ", "").strip()
    return decode_token(token)


def get_current_user_from_ws_token(token: str) -> TokenPayload:
    if not token:
        raise WebSocketException(
            code=status.WS_1008_POLICY_VIOLATION,
            reason="Missing token",
        )

    try:
        payload = jwt.decode(
            token,
            JWT_SECRET,
            algorithms=[JWT_ALGORITHM],
        )
        return TokenPayload(**payload)
    except JWTError:
        raise WebSocketException(
            code=status.WS_1008_POLICY_VIOLATION,
            reason="Invalid or expired token",
        )