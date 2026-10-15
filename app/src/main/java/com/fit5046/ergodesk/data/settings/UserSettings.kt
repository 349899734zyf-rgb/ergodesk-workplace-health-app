package com.fit5046.ergodesk.data.settings

import com.fit5046.ergodesk.data.context.QuietHours
import java.util.Locale

/*
 * One account's preferences from the Settings screen. The
 * defaults reproduce the behaviour described in the A2
 * report (30-minute interval, adaptive mode on).
 */
data class UserSettings(
    val monitoringEnabled: Boolean = true,
    val smartMode: Boolean = true,
    val breakIntervalMinutes: Int = 30,
    // 1 (relaxed) .. 5 (sensitive)
    val movementSensitivity: Int = 3,
    // Minutes after midnight; start > end wraps past midnight.
    val quietStartMinute: Int = 22 * 60,
    val quietEndMinute: Int = 7 * 60,
    val reminderStyle: String = REMINDER_STYLES.first(),
    val breakActivity: String = BREAK_ACTIVITIES.first(),
    val weatherAware: Boolean = true,
    val calendarAware: Boolean = false
) {

    /*
     * How long standing or moving must last to end a sitting
     * session. A more sensitive setting notices shorter
     * breaks; level 3 is the 60 seconds used by default.
     */
    val minBreakSeconds: Int
        get() = when (movementSensitivity) {
            1 -> 120
            2 -> 90
            4 -> 45
            5 -> 30
            else -> 60
        }

    val quietHours: QuietHours
        get() = QuietHours(quietStartMinute, quietEndMinute)

    companion object {

        val REMINDER_STYLES =
            listOf("Notification", "Sound + notification", "Vibration only", "Silent banner")

        const val ACTIVITY_STRETCH = "Stretch"
        const val ACTIVITY_WALK = "Walk"
        const val ACTIVITY_EYE_REST = "Eye rest"

        val BREAK_ACTIVITIES =
            listOf(ACTIVITY_STRETCH, ACTIVITY_WALK, ACTIVITY_EYE_REST)

        fun formatMinuteOfDay(
            minuteOfDay: Int
        ): String {
            return String.format(Locale.ENGLISH, "%02d:%02d", minuteOfDay / 60, minuteOfDay % 60)
        }
    }
}
