package com.safesphere.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_events")
data class BlockedEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,                 // epoch millis, UTC
    val category: String = "website"     // "website" ya "text"
)

