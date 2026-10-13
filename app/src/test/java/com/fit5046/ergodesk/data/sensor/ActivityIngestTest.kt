package com.fit5046.ergodesk.data.sensor

import com.fit5046.ergodesk.data.model.ActivityRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityIngestTest {

    private val now = 1_760_000_000_000L

    @Test
    fun noRecordYet_startsFromZeroWithDefaultTarget() {
        val record = ActivityIngest.addToDailyRecord(
            existing = null,
            summary = PostureSummary(600, 120, 180),
            nowMillis = now
        )

        assertEquals(ActivityRecord(now, 10, 5, 0, ActivityIngest.DEFAULT_BREAK_TARGET), record)
    }

    @Test
    fun existingRecord_addsMinutesAndKeepsBreaks() {
        val existing = ActivityRecord(now - 3_600_000L, 100, 40, 3, 6)

        val record = ActivityIngest.addToDailyRecord(
            existing = existing,
            summary = PostureSummary(900, 0, 0),
            nowMillis = now
        )

        assertEquals(ActivityRecord(now - 3_600_000L, 115, 40, 3, 6), record)
    }

    @Test
    fun roundedMinutes_alwaysAddUpToWindowLength() {
        // 437 s sitting would round to 7 min, active 463 s to 8 min.
        val record = ActivityIngest.addToDailyRecord(
            existing = null,
            summary = PostureSummary(437, 200, 263),
            nowMillis = now
        )

        assertEquals(15, record.sittingMinutes + record.activeMinutes)
        assertEquals(7, record.sittingMinutes)
    }

    @Test
    fun emptyWindow_changesNothing() {
        val existing = ActivityRecord(now, 100, 40, 3, 6)

        assertEquals(
            existing,
            ActivityIngest.addToDailyRecord(existing, PostureSummary(0, 0, 0), now)
        )
    }
}
