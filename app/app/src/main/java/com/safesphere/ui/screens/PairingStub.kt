package com.safesphere.ui.screens

import kotlinx.coroutines.delay

/** What the server can answer when the parent enters a pairing code. */
enum class PairResult { SUCCESS, INVALID_CODE, EXPIRED, NO_NETWORK }

/**
 * TEMPORARY stand-in for the real pairing call (Person 4's backend).
 * Demo codes:  000000 = invalid / already used,  999999 = expired,
 *              111111 = no internet,  anything else = success.
 * TODO: replace with the real request once the endpoint details are confirmed.
 */
object PairingStub {
    suspend fun pair(code: String): PairResult {
        delay(700)
        return when (code) {
            "000000" -> PairResult.INVALID_CODE
            "999999" -> PairResult.EXPIRED
            "111111" -> PairResult.NO_NETWORK
            else -> PairResult.SUCCESS
        }
    }
}