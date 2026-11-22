from fastapi import FastAPI

from database import init_db
from routes import events, heartbeat, pairing, protection

init_db()  # server start hote hi tables ban jayenge (agar pehle se nahi hain)

app = FastAPI()

app.include_router(heartbeat.router)
app.include_router(pairing.router)
app.include_router(events.router)
app.include_router(protection.router)


@app.get("/")
def root():
    return {"message": "SafeSphere backend running"}