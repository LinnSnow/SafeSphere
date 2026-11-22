package com.safesphere.ui.screens

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.safesphere.data.ApiClient
import com.safesphere.data.HeartbeatWorker
import com.safesphere.data.ProtectionEventRequest
import com.safesphere.data.TokenStore
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Called by AppPrefs after every protection change (off / on / paused / resumed). It
 *  1. sends a heartbeat right away (so the parent's status card updates), and
 *  2. sends a protection event (off / paused+until / on) for the parent's history.
 * When protection is paused it also schedules an "on" event + heartbeat for when the pause ends.
 */
object ProtectionSync {
    private const val EVENT_WORK = "protection_event"
    private const val RESUME_EVENT_WORK = "protection_resume_event"
    private const val RESUME_BEAT_WORK = "protection_resume_push"

    private fun network() =
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    private fun eventRequest(
        state: String, until: Long, onlyIfActive: Boolean, delayMs: Long
    ): OneTimeWorkRequest {
        val data = Data.Builder()
            .putString("state", state)
            .putLong("until", until)
            .putBoolean("only_if_active", onlyIfActive)
            .build()
        val b = OneTimeWorkRequestBuilder<ProtectionEventWorker>()
            .setConstraints(network())
            .setInputData(data)
        if (delayMs > 0) b.setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
        return b.build()
    }

    fun push(ctx: Context) {
        val app = ctx.applicationContext
        val wm = WorkManager.getInstance(app)

        // 1) heartbeat now
        wm.enqueueUniqueWork(
            "protection_push", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<HeartbeatWorker>().setConstraints(network()).build()
        )

        // 2) history event (kept in order, none are dropped)
        val on = AppPrefs.isProtectionOn(app)
        val paused = AppPrefs.isPaused(app)
        val until = AppPrefs.pausedUntil(app)
        val state = when {
            !on -> "off"
            paused -> "paused"
            else -> "on"
        }
        wm.cancelUniqueWork(RESUME_EVENT_WORK)   // any older "pause ends" event is now out of date
        wm.enqueueUniqueWork(
            EVENT_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE,
            eventRequest(state, until, onlyIfActive = false, delayMs = 0)
        )

        // 3) when the pause ends: one more heartbeat and an "on" event
        if (state == "paused") {
            val delay = until - System.currentTimeMillis() + 5_000L
            if (delay > 0) {
                wm.enqueueUniqueWork(
                    RESUME_BEAT_WORK, ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<HeartbeatWorker>()
                        .setConstraints(network())
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .build()
                )
                wm.enqueueUniqueWork(
                    RESUME_EVENT_WORK, ExistingWorkPolicy.REPLACE,
                    eventRequest("on", 0L, onlyIfActive = true, delayMs = delay)
                )
            }
        }
    }
}

/** Sends one protection event to the server. Only state + time, never content. */
class ProtectionEventWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val state = inputData.getString("state") ?: return Result.success()
        val until = inputData.getLong("until", 0L)

        // The "pause ended" event is only sent if protection really is active again.
        if (inputData.getBoolean("only_if_active", false) && !AppPrefs.isProtectionActive(ctx)) {
            return Result.success()
        }

        val store = TokenStore(ctx)
        val token = store.getTeenToken() ?: return Result.success()   // not a paired teen phone

        return try {
            ApiClient.apiService.sendProtectionEvent(
                store.bearer(token),
                ProtectionEventRequest(state, if (state == "paused" && until > 0L) until else null)
            )
            Result.success()
        } catch (e: HttpException) {
            // 5xx: try again later. 4xx (401 unpaired, 409 not paired yet, ...): nothing to retry.
            if (e.code() in 500..599 && runAttemptCount < 3) Result.retry() else Result.success()
        } catch (e: IOException) {
            if (runAttemptCount < 5) Result.retry() else Result.success()
        }
    }
}