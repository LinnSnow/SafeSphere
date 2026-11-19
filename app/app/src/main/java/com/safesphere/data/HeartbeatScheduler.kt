package com.safesphere.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object HeartbeatScheduler {

    private const val WORK_NAME = "safesphere_heartbeat"

    // Backend 360 minute (6 ghante) silence par protection OFF maanta hai.
    // Android periodic work ko battery bachane ke liye late kar sakta hai,
    // isliye har ~3 ghante me bhejte hain, taaki late hone par bhi threshold ke andar rahe.
    private const val INTERVAL_HOURS = 3L

    // KEEP ka matlab: schedule pehle se hai to wahi chalne do.
    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<HeartbeatWorker>(INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
    }
}