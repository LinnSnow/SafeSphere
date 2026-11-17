# Abhi data memory mein hai (server restart pe ud jayega).
# B4 step mein isko SQLite se replace karenge.

# device_id -> {"last_seen": datetime, "vpn_active": bool}
device_status = {}

# pairing code -> expiry datetime
pair_codes = {}