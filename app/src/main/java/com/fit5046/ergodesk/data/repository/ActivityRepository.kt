package com.fit5046.ergodesk.data.repository

import com.fit5046.ergodesk.data.model.ActivityRecord
import kotlinx.coroutines.flow.Flow

/*
 * Data source for daily activity summaries.
 *
 * Home, Analytics and the workers only depend on this
 * interface. RepositoryProvider supplies the Room-backed
 * RoomActivityRepository; FakeActivityRepository keeps an
 * in-memory version and the prototype data used to seed
 * Room's history.
 */
interface ActivityRepository {

    /*
     * Records whose timestamp is in [startMillis, endMillis),
     * oldest first. The Flow emits again whenever data changes.
     */
    fun observeRecordsBetween(
        startMillis: Long,
        endMillis: Long
    ): Flow<List<ActivityRecord>>

    /*
     * Inserts a record, or replaces the record for the same day.
     */
    suspend fun upsertRecord(
        record: ActivityRecord
    )
}
