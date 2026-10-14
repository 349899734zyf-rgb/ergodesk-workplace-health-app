package com.fit5046.ergodesk.data.repository

import android.content.Context
import com.fit5046.ergodesk.data.local.ErgoDeskDatabase

/*
 * Single place that decides which repository
 * implementation the app uses. Workers can run without
 * any screen open, so callers pass a Context instead of
 * relying on MainActivity to initialise this first.
 */
object RepositoryProvider {

    @Volatile
    private var instance: ActivityRepository? = null

    fun activityRepository(
        context: Context
    ): ActivityRepository {

        return instance ?: synchronized(this) {
            instance ?: RoomActivityRepository(
                ErgoDeskDatabase.getDatabase(context).activityRecordDao()
            ).also { instance = it }
        }
    }
}
