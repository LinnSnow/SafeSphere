import random
from datetime import datetime, timezone, timedelta

from fastapi import APIRouter, HTTPException

from database import pair_codes
from models import PairConfirm

router = APIRouter()


@router.post("/api/pair/generate")
def generate_pair_code():
    code = f"{random.randint(0, 999999):06d}"
    expires_at = datetime.now(timezone.utc) + timedelta(seconds=600)
    pair_codes[code] = expires_at
    return {"code": code, "expires_in_seconds": 600}


@router.post("/api/pair/confirm")
def confirm_pairing(body: PairConfirm):
    expires_at = pair_codes.get(body.code)
    if expires_at is None:
        raise HTTPException(status_code=404, detail="Invalid code")
    if datetime.now(timezone.utc) > expires_at:
        del pair_codes[body.code]
        raise HTTPException(status_code=400, detail="Code expired")

    del pair_codes[body.code]  # code sirf ek baar use ho sakta hai
    return {"status": "paired", "device_id": body.device_id}