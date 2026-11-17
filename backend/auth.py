import os
from datetime import datetime, timedelta, timezone
from pathlib import Path

import jwt
from fastapi import Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

# .env file se SECRET_KEY padhna (extra library ke bina)
ENV_PATH = Path(__file__).parent / ".env"


def load_secret_key():
    key = os.environ.get("SECRET_KEY")
    if key:
        return key
    if ENV_PATH.exists():
        for line in ENV_PATH.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if line.startswith("SECRET_KEY="):
                return line.split("=", 1)[1].strip()
    raise RuntimeError("SECRET_KEY nahi mili. backend/.env check karo.")


SECRET_KEY = load_secret_key()
ALGORITHM = "HS256"
TOKEN_DAYS = 90

# Swagger mein "Authorize" button isi se aata hai
bearer_scheme = HTTPBearer(auto_error=False)


def create_token(device_id: str) -> str:
    payload = {
        "sub": device_id,
        "exp": datetime.now(timezone.utc) + timedelta(days=TOKEN_DAYS),
    }
    return jwt.encode(payload, SECRET_KEY, algorithm=ALGORITHM)


def get_current_device(
    credentials: HTTPAuthorizationCredentials = Depends(bearer_scheme),
) -> str:
    """Request ke header 'Authorization: Bearer <token>' se device_id nikalta hai."""
    if credentials is None:
        raise HTTPException(status_code=401, detail="Missing token")
    token = credentials.credentials
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Token expired")
    except jwt.InvalidTokenError:
        raise HTTPException(status_code=401, detail="Invalid token")
    return payload["sub"]