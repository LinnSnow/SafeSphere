package com.safesphere.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import retrofit2.HttpException
import java.io.IOException

class EventsUploadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val store = TokenStore(applicationContext)

        // Pairing se pehle teen token nahi hota. Bhejne ko kuch nahi, bas skip.
        val token = store.getTeenToken() ?: return Result.success()
        val deviceId = store.getOrCreateDeviceId()
        val dao = AppDatabase.getInstance(applicationContext).blockedEventDao()

        return try {
            var lastId = store.getLastUploadedId()

            // 500-500 ke batches, jab tak naye rows khatam na ho jayein.
            while (true) {
                val rows = dao.getEventsAfter(lastId, BATCH_SIZE)
                if (rows.isEmpty()) break

                val body = EventsUploadRequest(
                    deviceId = deviceId,
                    events = rows.map { EventItemRequest(it.timestamp, it.category) }
                )
                ApiClient.apiService.uploadEvents(store.bearer(token), body)

                // Server ne accept kar liya, tabhi progress save karo.
                lastId = rows.last().id
                store.saveLastUploadedId(lastId)

                if (rows.size < BATCH_SIZE) break
            }
            Result.success()
        } catch (e: HttpException) {
            when (e.code()) {
                // Token revoked (unpair hua). Teen state reset, heartbeat/upload band.
                401 -> {
                    store.resetTeenState()
                    HeartbeatScheduler.cancel(applicationContext)
                    EventsUploadScheduler.cancel(applicationContext)
                    Result.failure()
                }
                // 409 Not paired: parent ne abhi confirm nahi kiya. Baad mein dobara try hoga.
                409 -> Result.success()
                in 500..599 -> if (runAttemptCount < 3) Result.retry() else Result.failure()
                else -> Result.failure()
            }
        } catch (e: IOException) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val BATCH_SIZE = 500
    }
}