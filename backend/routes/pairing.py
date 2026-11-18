import random
from datetime import datetime, timezone

from fastapi import APIRouter, HTTPException

from auth import create_token
from database import (
    delete_pair_code,
    get_pair_code,
    save_pair_code,
    save_pairing,
)
from models import PairConfirm, PairGenerate

router = APIRouter()


@router.post("/api/pair/generate")
def generate_pair_code(body: PairGenerate):
    # Teen phone yahan apna device_id bhejta hai.
    # Aisa code dhundo jo abhi kisi aur ke liye active na ho
    for _ in range(10):
        code = f"{random.randint(0, 999999):06d}"
        existing = get_pair_code(code)
        if existing is None or datetime.now(timezone.utc) > existing["expires_at"]:
            save_pair_code(code, body.device_id, seconds=600)
            # Teen ko apna token turant mil jata hai
            return {
                "code": code,
                "expires_in_seconds": 600,
                "device_id": body.device_id,
                "token": create_token(body.device_id),
            }
    raise HTTPException(status_code=503, detail="Could not generate a unique code, try again")


@router.post("/api/pair/confirm")
def confirm_pairing(body: PairConfirm):
    # Parent phone sirf code bhejta hai, device_id backend code se nikalta hai
    entry = get_pair_code(body.code)
    if entry is None or not entry["device_id"]:
        raise HTTPException(status_code=404, detail="Invalid code")
    if datetime.now(timezone.utc) > entry["expires_at"]:
        delete_pair_code(body.code)
        raise HTTPException(status_code=400, detail="Code expired")

    device_id = entry["device_id"]
    delete_pair_code(body.code)  # code sirf ek baar use ho sakta hai
    save_pairing(device_id)
    return {
        "status": "paired",
        "device_id": device_id,
        "parent_token": create_token(device_id),
    }