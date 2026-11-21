package com.safesphere.ui.screens

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.safesphere.data.HeartbeatWorker
import java.util.concurrent.TimeUnit

/** Tells the backend (through Person 4's heartbeat) that protection changed, without waiting 15 minutes. */
object ProtectionSync {
    fun push(ctx: Context) {
        val app = ctx.applicationContext
        val wm = WorkManager.getInstance(app)
        val net = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        wm.enqueueUniqueWork(
            "protection_push", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<HeartbeatWorker>().setConstraints(net).build()
        )

        // If paused, send one more update right after the pause ends.
        val delay = AppPrefs.pausedUntil(app) - System.currentTimeMillis()
        if (delay > 0) {
            wm.enqueueUniqueWork(
                "protection_resume_push", ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<HeartbeatWorker>()
                    .setConstraints(net)
                    .setInitialDelay(delay + 5_000L, TimeUnit.MILLISECONDS)
                    .build()
            )
        }
    }
}