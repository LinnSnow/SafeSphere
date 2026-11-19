package com.safesphere.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

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

// ---------- Retrofit interface ----------
// bearer parameter me poora "Bearer <token>" pass karna hai.

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
}