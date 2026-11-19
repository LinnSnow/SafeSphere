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

    // ---------- PARENT phone ----------

    fun savePairing(teenDeviceId: String, parentToken: String) {
        prefs.edit()
            .putString(KEY_PAIRED_DEVICE_ID, teenDeviceId)
            .putString(KEY_PARENT_TOKEN, parentToken)
            .apply()
    }

    fun getPairedDeviceId(): String? = prefs.getString(KEY_PAIRED_DEVICE_ID, null)

    fun getParentToken(): String? = prefs.getString(KEY_PARENT_TOKEN, null)

    // ---------- Helper ----------

    // Retrofit ke @Header("Authorization") ke liye.
    fun bearer(token: String): String = "Bearer $token"

    companion object {
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_TEEN_TOKEN = "teen_token"
        private const val KEY_PAIRED_DEVICE_ID = "paired_device_id"
        private const val KEY_PARENT_TOKEN = "parent_token"
    }
}