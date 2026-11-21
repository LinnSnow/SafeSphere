package com.safesphere.ui.screens

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/** Small on-device memory: setup done? role? parent PIN (salted hash)? protection on/off/paused? */
object AppPrefs {
    const val ROLE_TEEN = "teen"
    const val ROLE_PARENT = "parent"

    // Same file as Person 4's TokenStore, but different keys, so nothing clashes.
    private const val FILE = "safesphere_prefs"

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isSetupDone(ctx: Context): Boolean = prefs(ctx).getBoolean("setup_done", false)

    fun setSetupDone(ctx: Context, done: Boolean) {
        prefs(ctx).edit().putBoolean("setup_done", done).apply()
    }

    fun getRole(ctx: Context): String? = prefs(ctx).getString("role", null)

    fun setRole(ctx: Context, role: String) {
        prefs(ctx).edit().putString("role", role).apply()
    }

    // ---------- protection state ----------
    // TODO: also tell Person 1's VPN service and Person 4's ProtectionState when this changes.

    fun isProtectionOn(ctx: Context): Boolean = prefs(ctx).getBoolean("protection_on", true)

    fun setProtectionOn(ctx: Context, on: Boolean) {
        prefs(ctx).edit().putBoolean("protection_on", on).putLong("paused_until", 0L).apply()
    }

    fun pausedUntil(ctx: Context): Long = prefs(ctx).getLong("paused_until", 0L)

    fun isPaused(ctx: Context): Boolean = pausedUntil(ctx) > System.currentTimeMillis()

    fun pauseFor(ctx: Context, minutes: Int) {
        prefs(ctx).edit().putLong("paused_until", System.currentTimeMillis() + minutes * 60_000L).apply()
    }

    fun clearPause(ctx: Context) {
        prefs(ctx).edit().putLong("paused_until", 0L).apply()
    }

    /** The one answer every part of the app should use: is filtering actually running right now? */
    fun isProtectionActive(ctx: Context): Boolean = isProtectionOn(ctx) && !isPaused(ctx)

    // ---------- parent PIN ----------

    fun hasPin(ctx: Context): Boolean = prefs(ctx).getString("pin_hash", null) != null

    fun savePin(ctx: Context, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs(ctx).edit()
            .putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("pin_hash", hash(salt, pin))
            .apply()
    }

    fun checkPin(ctx: Context, pin: String): Boolean {
        val saltText = prefs(ctx).getString("pin_salt", null) ?: return false
        val expected = prefs(ctx).getString("pin_hash", null) ?: return false
        return hash(Base64.decode(saltText, Base64.NO_WRAP), pin) == expected
    }

    private fun hash(salt: ByteArray, pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        return md.digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}