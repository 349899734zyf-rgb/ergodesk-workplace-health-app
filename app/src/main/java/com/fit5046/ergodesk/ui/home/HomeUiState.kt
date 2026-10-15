package com.fit5046.ergodesk.ui.home

import com.fit5046.ergodesk.data.context.BreakSuggestion
import com.fit5046.ergodesk.data.context.ContextEngine
import com.fit5046.ergodesk.data.context.ContextInput
import com.fit5046.ergodesk.data.context.HoldReason
import com.fit5046.ergodesk.data.context.WeatherSnapshot
import com.fit5046.ergodesk.data.model.ActivityRecord
import com.fit5046.ergodesk.data.sensor.ActivityIngest
import com.fit5046.ergodesk.data.sensor.ActivitySessions
import com.fit5046.ergodesk.data.sensor.PostureState
import com.fit5046.ergodesk.data.sensor.SessionSummary

/*
 * What the ring on Home is showing; each mode has its own
 * colour so sitting and resting are easy to tell apart.
 */
enum class RingMode {
    SITTING,
    BREAK_DUE,
    ON_BREAK
}

/*
 * Everything the Home screen shows. posture is null until
 * the first sensor sample arrives.
 */
data class HomeUiState(
    val posture: PostureState? = null,
    val sittingBoutMinutes: Int = 0,
    val onBreak: Boolean = false,
    val breakMinutes: Int = 0,
    val breakIsLogged: Boolean = false,
    val breakIntervalMinutes: Int = ContextEngine.BASE_INTERVAL_MINUTES,
    val holdReason: HoldReason? = null,
    val suggestion: BreakSuggestion = BreakSuggestion.DESK_STRETCH,
    val sittingTodayMinutes: Int = 0,
    val longestSitMinutes: Int = 0,
    val breaksCompleted: Int = 0,
    val breakTarget: Int = ActivityIngest.DEFAULT_BREAK_TARGET
) {

    private val minutesUntilBreak: Int
        get() = breakIntervalMinutes - sittingBoutMinutes

    val ringMode: RingMode
        get() = when {
            onBreak -> RingMode.ON_BREAK
            posture != null && holdReason == null && minutesUntilBreak <= 0 -> RingMode.BREAK_DUE
            else -> RingMode.SITTING
        }

    // The number in the middle of the ring.
    val ringMinutes: Int
        get() = if (onBreak) breakMinutes else sittingBoutMinutes

    val ringLabel: String
        get() = if (onBreak) "minutes on a break" else "minutes sitting"

    // Sitting fills towards the break interval; a break fills
    // towards the recommended break length.
    val ringProgress: Float
        get() = if (onBreak) {
            breakMinutes / RECOMMENDED_BREAK_MINUTES.toFloat()
        } else {
            sittingBoutMinutes / breakIntervalMinutes.toFloat()
        }.coerceIn(0f, 1f)

    val breakProgress: Float
        get() = if (breakTarget > 0) {
            (breaksCompleted / breakTarget.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val postureLabel: String
        get() = when {
            posture == null -> "Reading sensors…"
            onBreak && breakIsLogged -> "Taking a break"
            posture == PostureState.SITTING -> "Sitting"
            posture == PostureState.STANDING -> "Standing"
            else -> "Moving"
        }

    val statusText: String
        get() = when {
            posture == null -> ""

            onBreak && breakMinutes >= RECOMMENDED_BREAK_MINUTES ->
                "Great break! Sit down whenever you're ready"

            onBreak && suggestion == BreakSuggestion.OUTDOOR_WALK ->
                "Good time for a short walk outside"

            onBreak -> "Try a quick desk stretch"

            holdReason == HoldReason.MEETING -> "Reminders paused during your meeting"
            holdReason == HoldReason.QUIET_HOURS -> "Reminders paused for quiet hours"
            minutesUntilBreak <= 0 -> "Break due now"
            minutesUntilBreak == 1 -> "Next break in 1 minute"
            else -> "Next break in $minutesUntilBreak minutes"
        }

    /*
     * One line for Logcat, written whenever the state changes.
     */
    val logSummary: String
        get() = "$ringMode: now $postureLabel, sitting $sittingBoutMinutes min, " +
                "break $breakMinutes min, interval $breakIntervalMinutes min -> \"$statusText\" | " +
                "hold ${holdReason ?: "none"} | " +
                "today sitting $sittingTodayMinutes min, longest $longestSitMinutes min, " +
                "breaks $breaksCompleted/$breakTarget"

    companion object {

        // A logged break lasts this long, so it is also the
        // break length Home encourages.
        val RECOMMENDED_BREAK_MINUTES =
            (ActivitySessions.LOGGED_BREAK_MILLIS / 60_000L).toInt()

        /*
         * Combines today's sessions with stored records and
         * cached weather. The break interval comes from
         * ContextEngine, so Home shows the same threshold
         * ContextEvaluationWorker uses for the alarm.
         */
        fun from(
            posture: PostureState,
            sessions: SessionSummary,
            today: ActivityRecord?,
            history: List<ActivityRecord>,
            hourOfDay: Int,
            weather: WeatherSnapshot?
        ): HomeUiState {

            val boutMinutes =
                (sessions.currentSittingSeconds / 60L).toInt()

            val decision =
                ContextEngine.evaluate(
                    ContextInput(
                        sittingBoutMinutes = boutMinutes,
                        complianceRate = ContextEngine.complianceRate(history),
                        hourOfDay = hourOfDay,
                        weather = weather
                    )
                )

            return HomeUiState(
                posture = posture,
                sittingBoutMinutes = boutMinutes,
                onBreak = sessions.onBreak,
                breakMinutes = (sessions.currentBreakSeconds / 60L).toInt(),
                breakIsLogged = sessions.currentBreakIsLogged,
                breakIntervalMinutes = decision.intervalMinutes,
                holdReason = decision.holdReason,
                suggestion = decision.suggestion,
                // Live from today's sessions, so total, longest
                // and current always agree. Room gets the same
                // totals from SensorIngestWorker.
                sittingTodayMinutes = (sessions.sittingSeconds / 60L).toInt(),
                longestSitMinutes = (sessions.longestSittingSeconds / 60L).toInt(),
                breaksCompleted = today?.breaksCompleted ?: 0,
                breakTarget = today?.breakTarget ?: ActivityIngest.DEFAULT_BREAK_TARGET
            )
        }
    }
}
