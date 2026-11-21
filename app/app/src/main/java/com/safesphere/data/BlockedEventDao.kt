package com.safesphere.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

// Ek din ka count (day: 0 = Sunday ... 6 = Saturday, SQLite ka %w)
data class DayCount(
    val day: Int,
    val count: Int
)

// Ek din + ek time block ka count (block: 0 = 00-06, 1 = 06-12, 2 = 12-18, 3 = 18-24)
data class HeatmapCount(
    val day: Int,
    val block: Int,
    val count: Int
)

@Dao
interface BlockedEventDao {

    @Insert
    suspend fun insert(event: BlockedEvent)

    @Query(
        """
        SELECT CAST(strftime('%w', timestamp / 1000, 'unixepoch', 'localtime') AS INTEGER) AS day,
               COUNT(*) AS count
        FROM blocked_events
        WHERE timestamp >= :startMillis AND timestamp < :endMillis
        GROUP BY day
        """
    )
    suspend fun getDailyCounts(startMillis: Long, endMillis: Long): List<DayCount>

    @Query(
        """
        SELECT CAST(strftime('%w', timestamp / 1000, 'unixepoch', 'localtime') AS INTEGER) AS day,
               CAST(strftime('%H', timestamp / 1000, 'unixepoch', 'localtime') AS INTEGER) / 6 AS block,
               COUNT(*) AS count
        FROM blocked_events
        WHERE timestamp >= :startMillis AND timestamp < :endMillis
        GROUP BY day, block
        """
    )
    suspend fun getHeatmapCounts(startMillis: Long, endMillis: Long): List<HeatmapCount>

    // v2: events uploader ke liye. Sirf wahi rows jo abhi tak server ko nahi bheji gayi.
    @Query("SELECT * FROM blocked_events WHERE id > :lastId ORDER BY id ASC LIMIT :limit")
    suspend fun getEventsAfter(lastId: Long, limit: Int): List<BlockedEvent>
}