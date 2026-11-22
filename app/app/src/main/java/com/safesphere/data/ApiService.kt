package com.safesphere.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// ---------- Request / Response data classes ----------

data class PairGenerateRequest(
    @SerializedName("device_id") val deviceId: String
)

data class PairGenerateResponse(
    @SerializedName("code") val code: String,
    @SerializedName("expires_in_seconds") val expiresInSeconds: Int,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("token") val token: String
)

data class PairConfirmRequest(
    @SerializedName("code") val code: String
)

data class PairConfirmResponse(
    @SerializedName("status") val status: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("parent_token") val parentToken: String
)

data class HeartbeatRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("vpn_active") val vpnActive: Boolean
)

data class StatusResponse(
    @SerializedName("protection_active") val protectionActive: Boolean,
    @SerializedName("last_seen_minutes_ago") val lastSeenMinutesAgo: Int
)

// ---------- v2: events, weekly, pair state, unpair ----------

data class EventItemRequest(
    @SerializedName("timestamp") val timestamp: Long,   // epoch millis UTC
    @SerializedName("category") val category: String    // "website" ya "text"
)

data class EventsUploadRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("events") val events: List<EventItemRequest>
)

data class EventsUploadResponse(
    @SerializedName("received") val received: Int,
    @SerializedName("saved") val saved: Int
)

data class WeeklyResponse(
    @SerializedName("week_start") val weekStart: String,
    @SerializedName("daily_counts") val dailyCounts: List<Int>,
    @SerializedName("heatmap") val heatmap: List<Int>
)

data class PairStateResponse(
    @SerializedName("paired") val paired: Boolean
)

data class UnpairResponse(
    @SerializedName("status") val status: String
)

// ---------- v3: protection log ----------

// state: "off" | "paused" | "on". until: optional, epoch millis UTC (paused ke liye).
// Gson null fields skip karta hai, to until = null bhejne pe JSON me aayega hi nahi (server ko yahi chahiye).
data class ProtectionEventRequest(
    @SerializedName("state") val state: String,
    @SerializedName("until") val until: Long? = null
)

data class ProtectionEventResponse(
    @SerializedName("status") val status: String,
    @SerializedName("timestamp") val timestamp: Long   // server ka time, epoch millis UTC
)

data class ProtectionLogEntry(
    @SerializedName("state") val state: String,
    @SerializedName("timestamp") val timestamp: Long,  // server ka time, epoch millis UTC
    @SerializedName("until") val until: Long?
)

data class ProtectionLogResponse(
    @SerializedName("events") val events: List<ProtectionLogEntry>
)

// ---------- Retrofit interface ----------
// bearer parameter me poora "Bearer <token>" pass karna hai.
// Teen calls (heartbeat, uploadEvents, pairState, sendProtectionEvent) me TEEN token,
// parent calls (weekly, unpair, getProtectionLog) me PARENT token.
// status() dono token ke saath chalta hai.
// Koi bhi 401 = unpaired/revoked, app ko state reset karni chahiye.

interface ApiService {

    @POST("api/pair/generate")
    suspend fun pairGenerate(
        @Body body: PairGenerateRequest
    ): PairGenerateResponse

    @POST("api/pair/confirm")
    suspend fun pairConfirm(
        @Body body: PairConfirmRequest
    ): PairConfirmResponse

    @POST("api/heartbeat")
    suspend fun heartbeat(
        @Header("Authorization") bearer: String,
        @Body body: HeartbeatRequest
    )

    @GET("api/status/{device_id}")
    suspend fun status(
        @Header("Authorization") bearer: String,
        @Path("device_id") deviceId: String
    ): StatusResponse

    @POST("api/events")
    suspend fun uploadEvents(
        @Header("Authorization") bearer: String,
        @Body body: EventsUploadRequest
    ): EventsUploadResponse

    @GET("api/weekly/{device_id}")
    suspend fun weekly(
        @Header("Authorization") bearer: String,
        @Path("device_id") deviceId: String,
        @Query("tz_offset_minutes") tzOffsetMinutes: Int
    ): WeeklyResponse

    @GET("api/pair/state/{device_id}")
    suspend fun pairState(
        @Header("Authorization") bearer: String,
        @Path("device_id") deviceId: String
    ): PairStateResponse

    @POST("api/unpair")
    suspend fun unpair(
        @Header("Authorization") bearer: String
    ): UnpairResponse

    @POST("api/protection-event")
    suspend fun sendProtectionEvent(
        @Header("Authorization") bearer: String,
        @Body body: ProtectionEventRequest
    ): ProtectionEventResponse

    @GET("api/protection-log/{device_id}")
    suspend fun getProtectionLog(
        @Header("Authorization") bearer: String,
        @Path("device_id") deviceId: String
    ): ProtectionLogResponse
}