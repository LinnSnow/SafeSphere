from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException

from auth import get_current_device
from database import get_device, save_heartbeat
from models import Heartbeat

router = APIRouter()


@router.post("/api/heartbeat")
def receive_heartbeat(hb: Heartbeat, token_device: str = Depends(get_current_device)):
    # token ka device_id aur body ka device_id same hona chahiye
    if hb.device_id != token_device:
        raise HTTPException(status_code=403, detail="Token does not match device")

    now = save_heartbeat(hb.device_id, hb.vpn_active)
    return {
        "status": "ok",
        "device_id": hb.device_id,
        "vpn_active": hb.vpn_active,
        "server_time": now.isoformat(),
    }


@router.get("/api/status/{device_id}")
def get_status(device_id: str, token_device: str = Depends(get_current_device)):
    # abhi ke liye: device apna hi status dekh sakta hai
    if device_id != token_device:
        raise HTTPException(status_code=403, detail="Token does not match device")

    info = get_device(device_id)
    if info is None:
        raise HTTPException(status_code=404, detail="Device not found")

    now = datetime.now(timezone.utc)
    minutes_ago = int((now - info["last_seen"]).total_seconds() // 60)
    protection_active = info["vpn_active"] and minutes_ago <= 360

    return {
        "protection_active": protection_active,
        "last_seen_minutes_ago": minutes_ago,
    }