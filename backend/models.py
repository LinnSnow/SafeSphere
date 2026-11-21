from typing import Literal

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