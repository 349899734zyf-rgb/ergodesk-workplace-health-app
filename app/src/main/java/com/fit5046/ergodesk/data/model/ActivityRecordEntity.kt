package com.fit5046.ergodesk.data.model

import androidx.room.Entity
import com.fit5046.ergodesk.data.analytics.AnalyticsCalculator
import com.fit5046.ergodesk.data.analytics.AnalyticsPeriod

/*
 * Room row for one account's activity on one day.
 *
 * The primary key is (account, start of that local day),
 * so an upsert for the same day replaces the row instead
 * of adding a second one, and accounts never share rows.
 */
@Entity(
    tableName = "activity_records",
    primaryKeys = ["userId", "dayStartMillis"]
)
data class ActivityRecordEntity(
    val userId: String,
    val dayStartMillis: Long,
    val timestamp: Long,
    val sittingMinutes: Int,
    val activeMinutes: Int,
    val breaksCompleted: Int,
    val breakTarget: Int
) {

    fun toRecord(): ActivityRecord {
        return ActivityRecord(
            timestamp = timestamp,
            sittingMinutes = sittingMinutes,
            activeMinutes = activeMinutes,
            breaksCompleted = breaksCompleted,
            breakTarget = breakTarget
        )
    }

    companion object {

        fun from(
            record: ActivityRecord,
            userId: String
        ): ActivityRecordEntity {
            return ActivityRecordEntity(
                userId = userId,
                dayStartMillis =
                    AnalyticsCalculator
                        .periodRange(AnalyticsPeriod.DAILY, record.timestamp)
                        .startMillis,
                timestamp = record.timestamp,
                sittingMinutes = record.sittingMinutes,
                activeMinutes = record.activeMinutes,
                breaksCompleted = record.breaksCompleted,
                breakTarget = record.breakTarget
            )
        }
    }
}
