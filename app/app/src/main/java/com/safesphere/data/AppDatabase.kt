package com.safesphere.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BlockedEvent::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun blockedEventDao(): BlockedEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "safesphere.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}