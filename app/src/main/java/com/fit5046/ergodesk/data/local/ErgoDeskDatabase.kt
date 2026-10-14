package com.fit5046.ergodesk.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fit5046.ergodesk.data.model.StretchItemEntity
import com.fit5046.ergodesk.data.room.Assessment
import com.fit5046.ergodesk.data.room.AssessmentDAO

@Database(
    entities = [StretchItemEntity::class, Assessment::class],
    version = 2,
    exportSchema = false
)
abstract class ErgoDeskDatabase : RoomDatabase() {

    abstract fun stretchDao(): StretchDao
    abstract fun assessmentDAO(): AssessmentDAO

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
                    .fallbackToDestructiveMigration(true)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}