from fastapi import FastAPI

from routes import heartbeat, pairing

app = FastAPI()

app.include_router(heartbeat.router)
app.include_router(pairing.router)


@app.get("/")
def root():
    return {"message": "SafeSphere backend running"}