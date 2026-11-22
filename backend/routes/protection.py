from fastapi import APIRouter, Depends, HTTPException

from auth import require_parent, require_teen
from database import get_protection_log, is_paired, save_protection_event
from models import ProtectionEventUpload

router = APIRouter()


@router.post("/api/protection-event")
def post_protection_event(
    body: ProtectionEventUpload, device_id: str = Depends(require_teen)
):
    # Sirf teen ka token. device_id token se aata hai, body se nahi.
    if not is_paired(device_id):
        raise HTTPException(status_code=409, detail="Not paired")

    ts = save_protection_event(device_id, body.state, body.until)
    return {"status": "ok", "timestamp": ts}


@router.get("/api/protection-log/{device_id}")
def get_log(device_id: str, token_device: str = Depends(require_parent)):
    # Sirf parent ka token, aur apne paired teen ka hi log
    if device_id != token_device:
        raise HTTPException(status_code=403, detail="Token does not match device")
    if not is_paired(device_id):
        raise HTTPException(status_code=404, detail="Not paired")

    return {"events": get_protection_log(device_id, 20)}