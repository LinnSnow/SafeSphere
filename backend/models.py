from pydantic import BaseModel


class Heartbeat(BaseModel):
    device_id: str
    timestamp: str
    vpn_active: bool


class PairConfirm(BaseModel):
    code: str
    device_id: str