package com.safesphere.data

class BlockEventLoggerImpl(
    private val dao: BlockedEventDao
) : BlockEventLogger {

    override suspend fun logBlock(category: String) {
        dao.insert(
            BlockedEvent(
                timestamp = System.currentTimeMillis(),
                category = category
            )
        )
    }
}