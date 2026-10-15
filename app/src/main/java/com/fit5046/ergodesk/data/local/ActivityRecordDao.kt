package com.fit5046.ergodesk.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.fit5046.ergodesk.data.model.ActivityRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for daily activity summaries.
 * Every query is limited to one account.
 */
@Dao
interface ActivityRecordDao {

    /*
     * Re-emits whenever the table changes, so Home and
     * Analytics update as soon as a worker writes.
     */
    @Query(
        "SELECT * FROM activity_records " +
                "WHERE userId = :userId " +
                "AND timestamp >= :startMillis AND timestamp < :endMillis " +
                "ORDER BY timestamp"
    )
    fun observeBetween(
        userId: String,
        startMillis: Long,
        endMillis: Long
    ): Flow<List<ActivityRecordEntity>>

    @Upsert
    suspend fun upsert(record: ActivityRecordEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(records: List<ActivityRecordEntity>)

    @Query("SELECT COUNT(*) FROM activity_records WHERE userId = :userId")
    suspend fun countFor(userId: String): Int
}
