import random
from datetime import datetime, timezone

from fastapi import APIRouter, HTTPException

from auth import create_token
from database import (
    delete_pair_code,
    get_pair_code_expiry,
    save_pair_code,
    save_pairing,
)
from models import PairConfirm

router = APIRouter()


@router.post("/api/pair/generate")
def generate_pair_code():
    # Aisa code dhundo jo abhi kisi aur ke liye active na ho
    for _ in range(10):
        code = f"{random.randint(0, 999999):06d}"
        expiry = get_pair_code_expiry(code)
        if expiry is None or datetime.now(timezone.utc) > expiry:
            save_pair_code(code, seconds=600)
            return {"code": code, "expires_in_seconds": 600}
    raise HTTPException(status_code=503, detail="Could not generate a unique code, try again")


@router.post("/api/pair/confirm")
def confirm_pairing(body: PairConfirm):
    expires_at = get_pair_code_expiry(body.code)
    if expires_at is None:
        raise HTTPException(status_code=404, detail="Invalid code")
    if datetime.now(timezone.utc) > expires_at:
        delete_pair_code(body.code)
        raise HTTPException(status_code=400, detail="Code expired")

    delete_pair_code(body.code)  # code sirf ek baar use ho sakta hai
    save_pairing(body.device_id)
    token = create_token(body.device_id)
    parent_token = create_token(body.device_id)
    return {
        "status": "paired",
        "device_id": body.device_id,
        "token": token,
        "parent_token": parent_token,
    }