from datetime import datetime, timezone
import random
import string

from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(title="SafeSphere Backend")


class Heartbeat(BaseModel):
    device_id: str
    timestamp: str
    vpn_active: bool


@app.get("/")
def root():
    return {"message": "SafeSphere backend running"}


@app.post("/api/heartbeat")
def heartbeat(data: Heartbeat):
    return {
        "status": "received",
        "device_id": data.device_id,
        "vpn_active": data.vpn_active,
        "server_time": datetime.now(timezone.utc).isoformat(),
    }


@app.post("/api/pair/generate")
def generate_pair_code():
    code = "".join(random.choices(string.digits, k=6))
    return {"pair_code": code, "expires_in_seconds": 600}