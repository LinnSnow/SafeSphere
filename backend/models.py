from pydantic import BaseModel


class Heartbeat(BaseModel):
    device_id: str
    timestamp: str
    vpn_active: bool


class PairGenerate(BaseModel):
    # Teen phone code banate waqt apna device_id bhejta hai
    device_id: str


class PairConfirm(BaseModel):
    # Parent phone sirf code bhejta hai, device_id backend khud nikalta hai
    code: str