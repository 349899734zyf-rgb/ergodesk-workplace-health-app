package com.fit5046.ergodesk.data.sensor

import com.fit5046.ergodesk.data.model.ActivityRecord
import kotlin.math.roundToInt

/*
 * Turns one sensing window into minutes and adds them to
 * the day's ActivityRecord. Pure Kotlin so the merge rules
 * are covered by JVM tests; SensorIngestWorker does the I/O.
 */
object ActivityIngest {

    // Used when today has no record yet.
    // TODO(merge with Settings): read the user's break target.
    const val DEFAULT_BREAK_TARGET = 8

    /*
     * Standing and moving both count as active time, since
     * either one breaks up sitting.
     *
     * Minutes are rounded from the window total and sitting
     * is subtracted, so a 900-second window always adds
     * exactly 15 minutes.
     */
    fun addToDailyRecord(
        existing: ActivityRecord?,
        summary: PostureSummary,
        nowMillis: Long
    ): ActivityRecord {

        val totalMinutes =
            ((summary.sittingSeconds +
                    summary.standingSeconds +
                    summary.movingSeconds) / 60.0).roundToInt()

        val sittingMinutes =
            (summary.sittingSeconds / 60.0).roundToInt()
                .coerceAtMost(totalMinutes)

        val activeMinutes =
            totalMinutes - sittingMinutes

        val base =
            existing ?: emptyDay(nowMillis)

        return base.copy(
            sittingMinutes = base.sittingMinutes + sittingMinutes,
            activeMinutes = base.activeMinutes + activeMinutes
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
