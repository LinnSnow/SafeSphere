import sqlite3
from datetime import datetime, timezone, timedelta
from pathlib import Path

# Db file backend folder ke andar banegi
DB_PATH = Path(__file__).parent / "safesphere.db"


def get_connection():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    conn = get_connection()
    conn.execute(
        """CREATE TABLE IF NOT EXISTS devices (
            device_id TEXT PRIMARY KEY,
            last_seen TEXT NOT NULL,
            vpn_active INTEGER NOT NULL
        )"""
    )
    conn.execute(
        """CREATE TABLE IF NOT EXISTS pair_codes (
            code TEXT PRIMARY KEY,
            expires_at TEXT NOT NULL
        )"""
    )
    conn.execute(
        """CREATE TABLE IF NOT EXISTS pairings (
            device_id TEXT PRIMARY KEY,
            paired_at TEXT NOT NULL
        )"""
    )
    conn.commit()
    conn.close()


# ---------- devices (heartbeat / status) ----------

def save_heartbeat(device_id, vpn_active):
    now = datetime.now(timezone.utc)
    conn = get_connection()
    conn.execute(
        "INSERT OR REPLACE INTO devices (device_id, last_seen, vpn_active) VALUES (?, ?, ?)",
        (device_id, now.isoformat(), 1 if vpn_active else 0),
    )
    conn.commit()
    conn.close()
    return now


def get_device(device_id):
    conn = get_connection()
    row = conn.execute(
        "SELECT last_seen, vpn_active FROM devices WHERE device_id = ?",
        (device_id,),
    ).fetchone()
    conn.close()
    if row is None:
        return None
    return {
        "last_seen": datetime.fromisoformat(row["last_seen"]),
        "vpn_active": bool(row["vpn_active"]),
    }


# ---------- pairing codes ----------

def save_pair_code(code, seconds=600):
    expires_at = datetime.now(timezone.utc) + timedelta(seconds=seconds)
    conn = get_connection()
    conn.execute(
        "INSERT OR REPLACE INTO pair_codes (code, expires_at) VALUES (?, ?)",
        (code, expires_at.isoformat()),
    )
    conn.commit()
    conn.close()


def get_pair_code_expiry(code):
    conn = get_connection()
    row = conn.execute(
        "SELECT expires_at FROM pair_codes WHERE code = ?", (code,)
    ).fetchone()
    conn.close()
    if row is None:
        return None
    return datetime.fromisoformat(row["expires_at"])


def delete_pair_code(code):
    conn = get_connection()
    conn.execute("DELETE FROM pair_codes WHERE code = ?", (code,))
    conn.commit()
    conn.close()


# ---------- pairings ----------

def save_pairing(device_id):
    now = datetime.now(timezone.utc)
    conn = get_connection()
    conn.execute(
        "INSERT OR REPLACE INTO pairings (device_id, paired_at) VALUES (?, ?)",
        (device_id, now.isoformat()),
    )
    conn.commit()
    conn.close()