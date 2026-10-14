package com.fit5046.ergodesk.data.repository

import com.fit5046.ergodesk.data.model.ActivityRecord
import kotlinx.coroutines.flow.Flow

/*
 * Data source for daily activity summaries.
 *
 * Home, Analytics and the WorkManager worker only depend
 * on this interface. Before the Room database is merged,
 * FakeActivityRepository provides prototype records.
 * After the merge, a Room-backed implementation replaces
 * it in RepositoryProvider.
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
