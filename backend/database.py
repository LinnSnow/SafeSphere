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
            expires_at TEXT NOT NULL,
            device_id TEXT
        )"""
    )
    # Purani local database mein device_id column nahi hoga, to add kar do
    columns = [
        row["name"]
        for row in conn.execute("PRAGMA table_info(pair_codes)").fetchall()
    ]
    if "device_id" not in columns:
        conn.execute("ALTER TABLE pair_codes ADD COLUMN device_id TEXT")
    conn.execute(
        """CREATE TABLE IF NOT EXISTS pairings (
            device_id TEXT PRIMARY KEY,
            paired_at TEXT NOT NULL
        )"""
    )
    # Har device ka token version. Unpair par badhta hai, to purane tokens band ho jate hain.
    conn.execute(
        """CREATE TABLE IF NOT EXISTS token_versions (
            device_id TEXT PRIMARY KEY,
            version INTEGER NOT NULL
        )"""
    )
    # Blocked events: sirf timestamp (epoch millis UTC) aur category. Domain/URL/text kabhi nahi.
    # PRIMARY KEY se same event dobara upload ho to duplicate nahi banta.
    conn.execute(
        """CREATE TABLE IF NOT EXISTS events (
            device_id TEXT NOT NULL,
            ts INTEGER NOT NULL,
            category TEXT NOT NULL,
            PRIMARY KEY (device_id, ts, category)
        )"""
    )
    # Protection log: teen ne protection off/paused/on kiya. Sirf state aur time, koi content nahi.
    # ts = server ka time (epoch millis UTC). until = optional (epoch millis), paused ke liye.
    conn.execute(
        """CREATE TABLE IF NOT EXISTS protection_events (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            device_id TEXT NOT NULL,
            ts INTEGER NOT NULL,
            state TEXT NOT NULL,
            until INTEGER
        )"""
    )
    conn.execute(
        """CREATE INDEX IF NOT EXISTS idx_protection_events_device_ts
           ON protection_events (device_id, ts)"""
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

def save_pair_code(code, device_id, seconds=600):
    expires_at = datetime.now(timezone.utc) + timedelta(seconds=seconds)
    conn = get_connection()
    conn.execute(
        "INSERT OR REPLACE INTO pair_codes (code, expires_at, device_id) VALUES (?, ?, ?)",
        (code, expires_at.isoformat(), device_id),
    )
    conn.commit()
    conn.close()


def get_pair_code(code):
    conn = get_connection()
    row = conn.execute(
        "SELECT expires_at, device_id FROM pair_codes WHERE code = ?", (code,)
    ).fetchone()
    conn.close()
    if row is None:
        return None
    return {
        "expires_at": datetime.fromisoformat(row["expires_at"]),
        "device_id": row["device_id"],
    }


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


def is_paired(device_id):
    """True agar is device ka abhi koi parent se pairing hai."""
    conn = get_connection()
    row = conn.execute(
        "SELECT 1 FROM pairings WHERE device_id = ?", (device_id,)
    ).fetchone()
    conn.close()
    return row is not None


def delete_pairing(device_id):
    conn = get_connection()
    conn.execute("DELETE FROM pairings WHERE device_id = ?", (device_id,))
    conn.commit()
    conn.close()


# ---------- token versions ----------

def get_token_version(device_id):
    """Is device ka current token version. Pehle kabhi unpair nahi hua to 0."""
    conn = get_connection()
    row = conn.execute(
        "SELECT version FROM token_versions WHERE device_id = ?", (device_id,)
    ).fetchone()
    conn.close()
    return row["version"] if row else 0


def bump_token_version(device_id):
    """Version 1 badhata hai. Isse is device ke saare purane tokens invalid ho jate hain."""
    conn = get_connection()
    conn.execute(
        """INSERT INTO token_versions (device_id, version) VALUES (?, 1)
           ON CONFLICT(device_id) DO UPDATE SET version = version + 1""",
        (device_id,),
    )
    conn.commit()
    row = conn.execute(
        "SELECT version FROM token_versions WHERE device_id = ?", (device_id,)
    ).fetchone()
    conn.close()
    return row["version"]


# ---------- events ----------

def save_events(device_id, events):
    """events = [(timestamp_millis, category), ...]. Duplicate ignore hote hain.
    Kitne naye events sach me save hue, wo number wapas deta hai."""
    conn = get_connection()
    before = conn.total_changes
    conn.executemany(
        "INSERT OR IGNORE INTO events (device_id, ts, category) VALUES (?, ?, ?)",
        [(device_id, ts, category) for ts, category in events],
    )
    conn.commit()
    inserted = conn.total_changes - before
    conn.close()
    return inserted


def get_events_between(device_id, start_ms, end_ms):
    """start_ms se (shamil) end_ms tak (shamil nahi) ke saare event timestamps, sorted."""
    conn = get_connection()
    rows = conn.execute(
        "SELECT ts FROM events WHERE device_id = ? AND ts >= ? AND ts < ? ORDER BY ts",
        (device_id, start_ms, end_ms),
    ).fetchall()
    conn.close()
    return [row["ts"] for row in rows]


def delete_events(device_id):
    conn = get_connection()
    conn.execute("DELETE FROM events WHERE device_id = ?", (device_id,))
    conn.commit()
    conn.close()


# ---------- protection log ----------

def save_protection_event(device_id, state, until=None):
    """Teen ne protection ki state badli (off / paused / on). Time server khud lagata hai.
    Saved event ka timestamp (epoch millis UTC) wapas deta hai."""
    ts = int(datetime.now(timezone.utc).timestamp() * 1000)
    conn = get_connection()
    conn.execute(
        "INSERT INTO protection_events (device_id, ts, state, until) VALUES (?, ?, ?, ?)",
        (device_id, ts, state, until),
    )
    conn.commit()
    conn.close()
    return ts


def get_protection_log(device_id, limit=20):
    """Is device ke last `limit` protection events, sabse naya pehle."""
    conn = get_connection()
    rows = conn.execute(
        """SELECT state, ts, until FROM protection_events
           WHERE device_id = ?
           ORDER BY ts DESC, id DESC
           LIMIT ?""",
        (device_id, limit),
    ).fetchall()
    conn.close()
    return [
        {"state": row["state"], "timestamp": row["ts"], "until": row["until"]}
        for row in rows
    ]


def delete_device_data(device_id):
    """Unpair par is device ka heartbeat record, events, protection log aur baaki bache hue pairing codes hata deta hai."""
    conn = get_connection()
    conn.execute("DELETE FROM devices WHERE device_id = ?", (device_id,))
    conn.execute("DELETE FROM pair_codes WHERE device_id = ?", (device_id,))
    conn.execute("DELETE FROM events WHERE device_id = ?", (device_id,))
    conn.execute("DELETE FROM protection_events WHERE device_id = ?", (device_id,))
    conn.commit()
    conn.close()