package com.fit5046.ergodesk.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fit5046.ergodesk.data.model.StretchItemEntity

/**
 * Main Room database instance for ErgoDesk application.
 * Manages tables for stretches, user assessments, and tracking.
 */
@Database(
    entities = [StretchItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ErgoDeskDatabase : RoomDatabase() {

    abstract fun stretchDao(): StretchDao

    companion object {
        @Volatile
        private var INSTANCE: ErgoDeskDatabase? = null

        fun getDatabase(context: Context): ErgoDeskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ErgoDeskDatabase::class.java,
                    "ergodesk_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
