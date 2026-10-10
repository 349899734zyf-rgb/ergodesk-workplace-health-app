
package com.fit5046.ergodesk.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Assessment::class],
    version = 1,
    exportSchema = false
)
abstract class ErgoDeskDatabase : RoomDatabase() {

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
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}
