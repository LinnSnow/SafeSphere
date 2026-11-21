import random
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException

from auth import create_token, require_parent, require_teen
from database import (
    bump_token_version,
    delete_device_data,
    delete_pair_code,
    delete_pairing,
    get_pair_code,
    get_token_version,
    is_paired,
    save_pair_code,
    save_pairing,
)
from models import PairConfirm, PairGenerate

router = APIRouter()


@router.post("/api/pair/generate")
def generate_pair_code(body: PairGenerate):
    # Teen phone yahan apna device_id bhejta hai.
    if is_paired(body.device_id):
        # Paired teen ke naam ka naya token koi aur na bana sake
        raise HTTPException(status_code=409, detail="Device already paired")
    if get_token_version(body.device_id) > 0:
        # Ye device pehle unpair ho chuka hai. Teen app ko naya device_id banana hai.
        raise HTTPException(status_code=409, detail="Device was unpaired, use a new device_id")

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
                "token": create_token(body.device_id, "teen"),
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
    if is_paired(device_id):
        # Is teen se pehle hi koi parent juda hua hai
        raise HTTPException(status_code=409, detail="Already paired")

    save_pairing(device_id)
    return {
        "status": "paired",
        "device_id": device_id,
        "parent_token": create_token(device_id, "parent"),
    }


@router.get("/api/pair/state/{device_id}")
def get_pair_state(device_id: str, token_device: str = Depends(require_teen)):
    # Teen phone isse poll karta hai: parent ne confirm kiya ya nahi
    if device_id != token_device:
        raise HTTPException(status_code=403, detail="Token does not match device")
    return {"paired": is_paired(device_id)}


@router.post("/api/unpair")
def unpair(device_id: str = Depends(require_parent)):
    # Parent ka token hi batata hai kaun sa teen device unpair karna hai
    if not is_paired(device_id):
        raise HTTPException(status_code=404, detail="Not paired")

    delete_pairing(device_id)
    delete_device_data(device_id)
    # Version badhne se teen aur parent dono ke purane tokens invalid ho jate hain
    bump_token_version(device_id)
    return {"status": "unpaired"}