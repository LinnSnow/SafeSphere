import os
from datetime import datetime, timedelta, timezone
from pathlib import Path

import jwt
from fastapi import Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from database import get_token_version

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
ROLES = ("teen", "parent")

# Swagger mein "Authorize" button isi se aata hai
bearer_scheme = HTTPBearer(auto_error=False)


def create_token(device_id: str, role: str) -> str:
    """Token me device_id, role (teen/parent) aur us device ka current version hota hai."""
    if role not in ROLES:
        raise ValueError("role must be 'teen' or 'parent'")
    payload = {
        "sub": device_id,
        "role": role,
        "ver": get_token_version(device_id),
        "exp": datetime.now(timezone.utc) + timedelta(days=TOKEN_DAYS),
    }
    return jwt.encode(payload, SECRET_KEY, algorithm=ALGORITHM)


def get_current_claims(
    credentials: HTTPAuthorizationCredentials = Depends(bearer_scheme),
) -> dict:
    """Header 'Authorization: Bearer <token>' check karta hai aur {device_id, role} deta hai."""
    if credentials is None:
        raise HTTPException(status_code=401, detail="Missing token")
    try:
        payload = jwt.decode(credentials.credentials, SECRET_KEY, algorithms=[ALGORITHM])
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Token expired")
    except jwt.InvalidTokenError:
        raise HTTPException(status_code=401, detail="Invalid token")

    device_id = payload.get("sub")
    role = payload.get("role")
    ver = payload.get("ver")
    # Purane tokens (role / ver ke bina) ab valid nahi hain
    if not device_id or role not in ROLES or not isinstance(ver, int):
        raise HTTPException(status_code=401, detail="Invalid token")
    # Unpair ke baad version badh jata hai, to purana token yahin rukega
    if ver != get_token_version(device_id):
        raise HTTPException(status_code=401, detail="Token revoked")
    return {"device_id": device_id, "role": role}


def get_current_device(claims: dict = Depends(get_current_claims)) -> str:
    """Kisi bhi role ke liye: bas device_id deta hai."""
    return claims["device_id"]


def require_teen(claims: dict = Depends(get_current_claims)) -> str:
    if claims["role"] != "teen":
        raise HTTPException(status_code=403, detail="Teen token required")
    return claims["device_id"]


def require_parent(claims: dict = Depends(get_current_claims)) -> str:
    if claims["role"] != "parent":
        raise HTTPException(status_code=403, detail="Parent token required")
    return claims["device_id"]