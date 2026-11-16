package com.safesphere.data

interface BlockEventLogger {
    suspend fun logBlock(category: String)
}