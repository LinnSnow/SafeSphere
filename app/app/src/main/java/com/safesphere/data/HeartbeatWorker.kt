package com.safesphere.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class HeartbeatWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val store = TokenStore(applicationContext)

        // Pairing se pehle teen token nahi hota. Tab bhejne ko kuch nahi, bas skip.
        val token = store.getTeenToken() ?: return Result.success()
        val deviceId = store.getOrCreateDeviceId()

        val body = HeartbeatRequest(
            deviceId = deviceId,
            timestamp = nowUtcIso(),
            vpnActive = ProtectionState.isVpnActive(applicationContext)
        )

        return try {
            ApiClient.apiService.heartbeat(store.bearer(token), body)
            Result.success()
        } catch (e: HttpException) {
            // Server error (5xx) par thodi der baad dobara try. 4xx (jaise galat token) par retry bekaar hai.
            if (e.code() in 500..599 && runAttemptCount < 3) Result.retry() else Result.failure()
        } catch (e: IOException) {
            // Internet nahi ya server so raha hai, thodi der baad dobara try.
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    // Backend ko "2026-10-14T09:30:00Z" jaisa UTC time chahiye.
    // java.time use nahi kiya kyunki wo API 26+ me hota hai, aur hamara minSdk 24 hai.
    private fun nowUtcIso(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }
}