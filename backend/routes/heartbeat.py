from datetime import datetime, timezone

from fastapi import APIRouter, HTTPException

from database import get_device, save_heartbeat
from models import Heartbeat

router = APIRouter()


@router.post("/api/heartbeat")
def receive_heartbeat(hb: Heartbeat):
    now = save_heartbeat(hb.device_id, hb.vpn_active)
    return {
        "status": "ok",
        "device_id": hb.device_id,
        "vpn_active": hb.vpn_active,
        "server_time": now.isoformat(),
    }


@router.get("/api/status/{device_id}")
def get_status(device_id: str):
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