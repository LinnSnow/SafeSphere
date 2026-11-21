package com.safesphere.data

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class TokenStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("safesphere_prefs", Context.MODE_PRIVATE)

    // ---------- TEEN phone ----------

    // Pehli baar random UUID banta hai, uske baad hamesha wahi milta hai.
    fun getOrCreateDeviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (existing != null) return existing
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        return newId
    }

    fun saveTeenToken(token: String) {
        prefs.edit().putString(KEY_TEEN_TOKEN, token).apply()
    }

    fun getTeenToken(): String? = prefs.getString(KEY_TEEN_TOKEN, null)

    // ---------- Events uploader tracking (teen phone) ----------

    // Room blocked_events ka last id jo server ko bhej diya gaya. 0 = abhi kuch nahi bheja.
    fun getLastUploadedId(): Long = prefs.getLong(KEY_LAST_UPLOADED_ID, 0L)

    fun saveLastUploadedId(id: Long) {
        prefs.edit().putLong(KEY_LAST_UPLOADED_ID, id).apply()
    }

    // ---------- PARENT phone ----------

    fun savePairing(teenDeviceId: String, parentToken: String) {
        prefs.edit()
            .putString(KEY_PAIRED_DEVICE_ID, teenDeviceId)
            .putString(KEY_PARENT_TOKEN, parentToken)
            .apply()
    }

    fun getPairedDeviceId(): String? = prefs.getString(KEY_PAIRED_DEVICE_ID, null)

    fun getParentToken(): String? = prefs.getString(KEY_PARENT_TOKEN, null)

    // ---------- Unpair reset ----------

    // Teen phone: koi bhi 401 (Token revoked) ya parent ke unpair ke baad call karo.
    // Teen token, last_uploaded_id aur purana device_id hata deta hai,
    // aur NAYA device_id bana ke return karta hai
    // (server pe unpaired device_id dobara use nahi ho sakta).
    fun resetTeenState(): String {
        prefs.edit()
            .remove(KEY_TEEN_TOKEN)
            .remove(KEY_LAST_UPLOADED_ID)
            .remove(KEY_DEVICE_ID)
            .apply()
        return getOrCreateDeviceId()
    }

    // Parent phone: unpair ke baad ya 401 aane pe call karo.
    fun resetParentState() {
        prefs.edit()
            .remove(KEY_PAIRED_DEVICE_ID)
            .remove(KEY_PARENT_TOKEN)
            .apply()
    }

    // ---------- Helper ----------

    // Retrofit ke @Header("Authorization") ke liye.
    fun bearer(token: String): String = "Bearer $token"

    companion object {
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_TEEN_TOKEN = "teen_token"
        private const val KEY_PAIRED_DEVICE_ID = "paired_device_id"
        private const val KEY_PARENT_TOKEN = "parent_token"
        private const val KEY_LAST_UPLOADED_ID = "last_uploaded_id"
    }
}