from typing import Literal, Optional

from pydantic import BaseModel, Field


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


class EventItem(BaseModel):
    # Room ke blocked_events jaisa: epoch millis (UTC) aur category. Domain/URL/text kabhi nahi.
    timestamp: int = Field(gt=0)
    category: Literal["website", "text"]


class EventsUpload(BaseModel):
    device_id: str
    events: list[EventItem] = Field(max_length=500)


class ProtectionEventUpload(BaseModel):
    # Teen phone sirf state bhejta hai. device_id token se aata hai, time server lagata hai.
    # "removed" = teen ne app ka device admin hataya (uninstall warning ke liye).
    # until = optional, epoch millis (UTC), "paused" ke liye kab tak.
    state: Literal["off", "paused", "on", "removed"]
    until: Optional[int] = Field(default=None, gt=0)