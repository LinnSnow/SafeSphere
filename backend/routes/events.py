from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends, HTTPException, Query

from auth import require_parent, require_teen
from database import get_events_between, is_paired, save_events
from models import EventsUpload

router = APIRouter()

WEEK_MS = 7 * 24 * 60 * 60 * 1000


@router.post("/api/events")
def upload_events(body: EventsUpload, token_device: str = Depends(require_teen)):
    # Sirf teen ka token, aur token ka device_id body ke device_id jaisa hona chahiye
    if body.device_id != token_device:
        raise HTTPException(status_code=403, detail="Token does not match device")
    # Pairing se pehle server pe koi data jama nahi hona chahiye
    if not is_paired(body.device_id):
        raise HTTPException(status_code=409, detail="Not paired")

    pairs = [(e.timestamp, e.category) for e in body.events]
    saved = save_events(body.device_id, pairs)
    return {"received": len(pairs), "saved": saved}


@router.get("/api/weekly/{device_id}")
def get_weekly(
    device_id: str,
    tz_offset_minutes: int = Query(0, ge=-840, le=840),
    token_device: str = Depends(require_parent),
):
    # Sirf parent ka token, aur apne paired teen ka hi data
    if device_id != token_device:
        raise HTTPException(status_code=403, detail="Token does not match device")
    if not is_paired(device_id):
        raise HTTPException(status_code=404, detail="Not paired")

    offset = timedelta(minutes=tz_offset_minutes)

    # Teen ke local time ke hisaab se aaj ka din, aur is hafte ka Monday 00:00
    now_local = datetime.now(timezone.utc) + offset
    monday_local = (now_local - timedelta(days=now_local.weekday())).replace(
        hour=0, minute=0, second=0, microsecond=0
    )
    # Monday 00:00 local ko wapas UTC epoch millis me
    start_ms = int((monday_local - offset).timestamp() * 1000)
    end_ms = start_ms + WEEK_MS

    daily_counts = [0] * 7
    heatmap = [0] * 28  # index = din * 4 + block (0: 00-06, 1: 06-12, 2: 12-18, 3: 18-24)

    for ts in get_events_between(device_id, start_ms, end_ms):
        local = datetime.fromtimestamp(ts / 1000, tz=timezone.utc) + offset
        day = (local - monday_local).days
        if 0 <= day < 7:
            block = local.hour // 6
            daily_counts[day] += 1
            heatmap[day * 4 + block] += 1

    return {
        "week_start": monday_local.strftime("%Y-%m-%d"),
        "daily_counts": daily_counts,
        "heatmap": heatmap,
    }