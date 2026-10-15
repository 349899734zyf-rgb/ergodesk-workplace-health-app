package com.fit5046.ergodesk.data.sensor

import com.fit5046.ergodesk.data.model.ActivityRecord

/*
 * Rules for updating the day's ActivityRecord. Pure Kotlin;
 * SensorIngestWorker and Home do the I/O.
 */
object ActivityIngest {

    // Used when today has no record yet.
    // TODO(merge with Settings): read the user's break target.
    const val DEFAULT_BREAK_TARGET = 8

    /*
     * Sets today's sitting and active minutes from the day's
     * sessions so far. Recomputed from scratch on every run,
     * so a late or repeated run never double-counts or skips
     * time, and the totals match what Home shows. Standing
     * and moving both count as active time.
     */
    fun withTodayTotals(
        existing: ActivityRecord?,
        summary: SessionSummary,
        hourly: HourlyActivity,
        nowMillis: Long
    ): ActivityRecord {

        val base =
            existing ?: emptyDay(nowMillis)

        return base.copy(
            sittingMinutes = (summary.sittingSeconds / 60L).toInt(),
            activeMinutes = (summary.activeSeconds / 60L).toInt(),
            hourlySittingMinutes = hourly.sittingMinutes,
            hourlyActiveMinutes = hourly.activeMinutes
        )
    }

    /*
     * The user took a break from Home ("Take a break now").
     * Feeds break compliance, which ContextEngine uses to
     * choose the reminder interval.
     */
    fun addBreak(
        existing: ActivityRecord?,
        nowMillis: Long
    ): ActivityRecord {

        val base =
            existing ?: emptyDay(nowMillis)

        return base.copy(
            breaksCompleted = base.breaksCompleted + 1
        )
    }

    private fun emptyDay(
        nowMillis: Long
    ): ActivityRecord {

        return ActivityRecord(
            timestamp = nowMillis,
            sittingMinutes = 0,
            activeMinutes = 0,
            breaksCompleted = 0,
            breakTarget = DEFAULT_BREAK_TARGET
        )
    }
}
