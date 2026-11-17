import random
from datetime import datetime, timezone

from fastapi import APIRouter, HTTPException

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
    code = f"{random.randint(0, 999999):06d}"
    save_pair_code(code, seconds=600)
    return {"code": code, "expires_in_seconds": 600}


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
    return {"status": "paired", "device_id": body.device_id}