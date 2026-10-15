package com.fit5046.ergodesk.ui.home

import com.fit5046.ergodesk.data.context.BreakSuggestion
import com.fit5046.ergodesk.data.context.ContextEngine
import com.fit5046.ergodesk.data.context.ContextInput
import com.fit5046.ergodesk.data.context.DayOffCalendar
import com.fit5046.ergodesk.data.context.HoldReason
import com.fit5046.ergodesk.data.context.WeatherSnapshot
import com.fit5046.ergodesk.data.model.ActivityRecord
import com.fit5046.ergodesk.data.sensor.ActivityIngest
import com.fit5046.ergodesk.data.sensor.ActivitySessions
import com.fit5046.ergodesk.data.sensor.PostureState
import com.fit5046.ergodesk.data.sensor.SessionSummary
import com.fit5046.ergodesk.data.settings.UserSettings
import java.time.LocalDate

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
    val dayOffReason: String? = null,
    val suggestion: BreakSuggestion = BreakSuggestion.DESK_STRETCH,
    // From Settings: keep sensing when the app is closed.
    val backgroundMonitoring: Boolean = true,
    val breakActivity: String = UserSettings.ACTIVITY_STRETCH,
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

            onBreak -> breakIdea

            holdReason == HoldReason.MEETING -> "Reminders paused during your meeting"
            holdReason == HoldReason.DAY_OFF -> "No reminders today: $dayOffReason"
            holdReason == HoldReason.QUIET_HOURS -> "Reminders paused for quiet hours"
            minutesUntilBreak <= 0 -> "Break due now"
            minutesUntilBreak == 1 -> "Next break in 1 minute"
            else -> "Next break in $minutesUntilBreak minutes"
        }

    // Shown under the ring when sensing stops with the app.
    val monitoringNote: String?
        get() = if (backgroundMonitoring) {
            null
        } else {
            "Background monitoring is off: tracking only while ErgoDesk is open"
        }

    /*
     * What to do on a break: the user's preferred activity
     * (Settings), with a walk taken outside only when the
     * weather and time of day suit it (ContextEngine).
     */
    private val breakIdea: String
        get() = when (breakActivity) {
            UserSettings.ACTIVITY_EYE_REST ->
                "Rest your eyes: look far away for 20 seconds"

            UserSettings.ACTIVITY_WALK ->
                if (suggestion == BreakSuggestion.OUTDOOR_WALK) {
                    "Good time for a short walk outside"
                } else {
                    "Take a short walk indoors"
                }

            else ->
                if (suggestion == BreakSuggestion.OUTDOOR_WALK) {
                    "Stretch, or step outside: the weather's nice"
                } else {
                    "Try a quick desk stretch"
                }
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
         * Combines today's sessions with stored records, cached
         * weather and the user's settings. The break interval comes from
         * ContextEngine, so Home shows the same threshold
         * ContextEvaluationWorker uses for the alarm.
         */
        fun from(
            posture: PostureState,
            sessions: SessionSummary,
            today: ActivityRecord?,
            history: List<ActivityRecord>,
            minuteOfDay: Int,
            weather: WeatherSnapshot?,
            settings: UserSettings
        ): HomeUiState {

            val boutMinutes =
                (sessions.currentSittingSeconds / 60L).toInt()

            val dayOffReason =
                if (settings.calendarAware) {
                    DayOffCalendar.dayOffReason(LocalDate.now())
                } else {
                    null
                }

            val decision =
                ContextEngine.evaluate(
                    ContextInput(
                        sittingBoutMinutes = boutMinutes,
                        complianceRate = ContextEngine.complianceRate(history),
                        minuteOfDay = minuteOfDay,
                        baseIntervalMinutes = settings.breakIntervalMinutes,
                        smartMode = settings.smartMode,
                        dayOffReason = dayOffReason,
                        weather = weather?.takeIf { settings.weatherAware },
                        quietHours = settings.quietHours
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
                dayOffReason = dayOffReason,
                suggestion = decision.suggestion,
                backgroundMonitoring = settings.monitoringEnabled,
                breakActivity = settings.breakActivity,
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
