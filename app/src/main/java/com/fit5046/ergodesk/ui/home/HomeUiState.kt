package com.fit5046.ergodesk.ui.home

import com.fit5046.ergodesk.data.context.ContextEngine
import com.fit5046.ergodesk.data.context.ContextInput
import com.fit5046.ergodesk.data.context.HoldReason
import com.fit5046.ergodesk.data.model.ActivityRecord
import com.fit5046.ergodesk.data.sensor.ActivityIngest
import com.fit5046.ergodesk.data.sensor.PostureState

/*
 * Everything the Home screen shows. posture is null until
 * the first sensor sample arrives.
 */
data class HomeUiState(
    val posture: PostureState? = null,
    val sittingBoutMinutes: Int = 0,
    val breakIntervalMinutes: Int = ContextEngine.BASE_INTERVAL_MINUTES,
    val holdReason: HoldReason? = null,
    val sittingTodayMinutes: Int = 0,
    val breaksCompleted: Int = 0,
    val breakTarget: Int = ActivityIngest.DEFAULT_BREAK_TARGET
) {

    val sittingProgress: Float
        get() = (sittingBoutMinutes / breakIntervalMinutes.toFloat())
            .coerceIn(0f, 1f)

    val breakProgress: Float
        get() = if (breakTarget > 0) {
            (breaksCompleted / breakTarget.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val postureLabel: String
        get() = when (posture) {
            null -> "Reading sensors…"
            PostureState.SITTING -> "Sitting"
            PostureState.STANDING -> "Standing"
            PostureState.MOVING -> "Moving"
        }

    val statusText: String
        get() {
            val minutesLeft =
                breakIntervalMinutes - sittingBoutMinutes

            return when {
                posture == null -> ""
                holdReason == HoldReason.MEETING -> "Reminders paused during your meeting"
                holdReason == HoldReason.QUIET_HOURS -> "Reminders paused for quiet hours"
                sittingBoutMinutes == 0 && posture != PostureState.SITTING -> "You're on a break"
                minutesLeft <= 0 -> "Break due now"
                minutesLeft == 1 -> "Next break in 1 minute"
                else -> "Next break in $minutesLeft minutes"
            }
        }

    /*
     * One line for Logcat, written whenever the state changes.
     */
    val logSummary: String
        get() = "Now $postureLabel, sitting $sittingBoutMinutes min, " +
                "interval $breakIntervalMinutes min -> \"$statusText\" | " +
                "hold ${holdReason ?: "none"} | " +
                "today sitting $sittingTodayMinutes min, breaks $breaksCompleted/$breakTarget"

    companion object {

        /*
         * Combines live sensing with stored records. The
         * break interval comes from ContextEngine, so Home
         * shows the same threshold ContextEvaluationWorker
         * uses for the alarm.
         */
        fun from(
            posture: PostureState,
            sittingBoutSeconds: Int,
            today: ActivityRecord?,
            history: List<ActivityRecord>,
            hourOfDay: Int
        ): HomeUiState {

            val boutMinutes =
                sittingBoutSeconds / 60

            val decision =
                ContextEngine.evaluate(
                    ContextInput(
                        sittingBoutMinutes = boutMinutes,
                        complianceRate = ContextEngine.complianceRate(history),
                        hourOfDay = hourOfDay
                    )
                )

            return HomeUiState(
                posture = posture,
                sittingBoutMinutes = boutMinutes,
                breakIntervalMinutes = decision.intervalMinutes,
                holdReason = decision.holdReason,
                sittingTodayMinutes = today?.sittingMinutes ?: 0,
                breaksCompleted = today?.breaksCompleted ?: 0,
                breakTarget = today?.breakTarget ?: ActivityIngest.DEFAULT_BREAK_TARGET
            )
        }
    }
}
