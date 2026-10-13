package com.fit5046.ergodesk.ui.home

import com.fit5046.ergodesk.data.context.HoldReason
import com.fit5046.ergodesk.data.model.ActivityRecord
import com.fit5046.ergodesk.data.sensor.PostureState
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeUiStateTest {

    // 6 of 8 breaks a day -> 75% compliance -> 30-minute interval.
    private val averageWeek = List(7) { ActivityRecord(it.toLong(), 300, 100, 6, 8) }

    private fun state(
        posture: PostureState = PostureState.SITTING,
        boutSeconds: Int = 22 * 60,
        today: ActivityRecord? = ActivityRecord(0L, 380, 90, 5, 8),
        history: List<ActivityRecord> = averageWeek,
        hour: Int = 10
    ) = HomeUiState.from(posture, boutSeconds, today, history, hour)

    @Test
    fun from_takesTotalsFromTodaysRecord() {
        val state = state()

        assertEquals(22, state.sittingBoutMinutes)
        assertEquals(380, state.sittingTodayMinutes)
        assertEquals(5, state.breaksCompleted)
        assertEquals(8, state.breakTarget)
        assertEquals("Next break in 8 minutes", state.statusText)
    }

    @Test
    fun from_usesContextEngineInterval() {
        val strongWeek = List(7) { ActivityRecord(it.toLong(), 300, 100, 8, 8) }
        val weakWeek = List(7) { ActivityRecord(it.toLong(), 300, 100, 1, 8) }

        assertEquals(30, state().breakIntervalMinutes)
        assertEquals(25, state(history = strongWeek).breakIntervalMinutes)
        assertEquals(45, state(history = weakWeek).breakIntervalMinutes)
    }

    @Test
    fun from_noRecordToday_showsZeroWithDefaultTarget() {
        val state = state(today = null)

        assertEquals(0, state.sittingTodayMinutes)
        assertEquals(0, state.breaksCompleted)
        assertEquals(8, state.breakTarget)
    }

    @Test
    fun statusText_coversEachSituation() {
        assertEquals("Break due now", state(boutSeconds = 31 * 60).statusText)
        assertEquals("Next break in 1 minute", state(boutSeconds = 29 * 60).statusText)
        assertEquals("You're on a break", state(PostureState.MOVING, boutSeconds = 0).statusText)
        assertEquals("Reminders paused for quiet hours", state(hour = 23).statusText)
        assertEquals("", HomeUiState().statusText)
    }

    @Test
    fun briefStandMidBout_stillCountsDown() {
        val state = state(PostureState.STANDING, boutSeconds = 10 * 60)

        assertEquals("Standing", state.postureLabel)
        assertEquals("Next break in 20 minutes", state.statusText)
    }

    @Test
    fun progress_isClampedToOne() {
        assertEquals(1f, state(boutSeconds = 90 * 60).sittingProgress, 0f)
        assertEquals(1f, HomeUiState(breaksCompleted = 10, breakTarget = 8).breakProgress, 0f)
        assertEquals(0f, HomeUiState(breakTarget = 0).breakProgress, 0f)
    }

    @Test
    fun meeting_holdIsShown() {
        assertEquals(
            "Reminders paused during your meeting",
            HomeUiState(posture = PostureState.SITTING, holdReason = HoldReason.MEETING).statusText
        )
    }
}
