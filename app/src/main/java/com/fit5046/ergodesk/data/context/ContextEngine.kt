package com.fit5046.ergodesk.data.context

import com.fit5046.ergodesk.data.analytics.AnalyticsCalculator
import com.fit5046.ergodesk.data.analytics.AnalyticsPeriod
import com.fit5046.ergodesk.data.analytics.PeriodRange
import com.fit5046.ergodesk.data.model.ActivityRecord
import java.util.Calendar

enum class ReminderStyle {
    STANDARD,
    SILENT_BANNER
}

enum class BreakSuggestion {
    DESK_STRETCH,
    OUTDOOR_WALK
}

enum class HoldReason {
    MEETING,
    QUIET_HOURS
}

/*
 * Latest weather from the cache WeatherSyncWorker fills.
 * isOutdoorFriendly comes from WeatherRepository (Member 4),
 * so the Home weather card and break suggestions agree.
 */
data class WeatherSnapshot(
    val temperatureC: Double,
    val condition: String,
    val isOutdoorFriendly: Boolean
)

/*
 * Hours in 24-hour time. startHour > endHour wraps past
 * midnight, e.g. 22 to 7.
 */
data class QuietHours(
    val startHour: Int,
    val endHour: Int
)

data class ContextInput(
    val sittingBoutMinutes: Int,
    // Share of planned breaks taken recently, 0..1;
    // null when there is no history yet.
    val complianceRate: Float?,
    val hourOfDay: Int,
    val inMeeting: Boolean = false,
    val weather: WeatherSnapshot? = null,
    val quietHours: QuietHours = ContextEngine.DEFAULT_QUIET_HOURS
)

/*
 * holdReason != null means no reminder may be shown now.
 * minutesUntilDue <= 0 means the break is already due.
 */
data class ContextDecision(
    val intervalMinutes: Int,
    val minutesUntilDue: Int,
    val holdReason: HoldReason?,
    val style: ReminderStyle,
    val suggestion: BreakSuggestion
)

/*
 * Fuses activity and non-sensory context into a break
 * decision (A2 report, section 1.4). Pure Kotlin so every
 * rule is covered by JVM tests; ContextEvaluationWorker
 * gathers the inputs and schedules the alarm.
 */
object ContextEngine {

    const val BASE_INTERVAL_MINUTES = 30
    const val RELAXED_INTERVAL_MINUTES = 45
    const val TIGHT_INTERVAL_MINUTES = 25

    const val LOW_COMPLIANCE = 0.4f
    const val HIGH_COMPLIANCE = 0.8f

    const val COMPLIANCE_DAYS = 7

    // TODO(merge with Settings): read the user's quiet hours.
    val DEFAULT_QUIET_HOURS = QuietHours(startHour = 22, endHour = 7)

    // Outdoor walks are only suggested in daylight working
    // hours, and only when the weather suits one.
    private const val WALK_FIRST_HOUR = 8
    private const val WALK_LAST_HOUR = 18

    fun evaluate(
        input: ContextInput
    ): ContextDecision {

        val interval =
            breakInterval(input.complianceRate)

        return ContextDecision(
            intervalMinutes = interval,
            minutesUntilDue = interval - input.sittingBoutMinutes,
            holdReason = holdReason(input),
            style = reminderStyle(input.complianceRate),
            suggestion =
                if (isWalkWeather(input.weather, input.hourOfDay)) {
                    BreakSuggestion.OUTDOOR_WALK
                } else {
                    BreakSuggestion.DESK_STRETCH
                }
        )
    }

    /*
     * Users who skip most breaks get fewer, quieter
     * reminders instead of being nagged; users who keep
     * up get a tighter interval.
     */
    fun breakInterval(
        complianceRate: Float?
    ): Int {

        return when {
            complianceRate == null -> BASE_INTERVAL_MINUTES
            complianceRate < LOW_COMPLIANCE -> RELAXED_INTERVAL_MINUTES
            complianceRate > HIGH_COMPLIANCE -> TIGHT_INTERVAL_MINUTES
            else -> BASE_INTERVAL_MINUTES
        }
    }

    fun reminderStyle(
        complianceRate: Float?
    ): ReminderStyle {

        return if (complianceRate != null && complianceRate < LOW_COMPLIANCE) {
            ReminderStyle.SILENT_BANNER
        } else {
            ReminderStyle.STANDARD
        }
    }

    fun holdReason(
        input: ContextInput
    ): HoldReason? {

        return when {
            input.inMeeting -> HoldReason.MEETING
            isQuietHour(input.hourOfDay, input.quietHours) -> HoldReason.QUIET_HOURS
            else -> null
        }
    }

    fun isQuietHour(
        hourOfDay: Int,
        quietHours: QuietHours
    ): Boolean {

        return if (quietHours.startHour <= quietHours.endHour) {
            hourOfDay >= quietHours.startHour && hourOfDay < quietHours.endHour
        } else {
            hourOfDay >= quietHours.startHour || hourOfDay < quietHours.endHour
        }
    }

    fun isWalkWeather(
        weather: WeatherSnapshot?,
        hourOfDay: Int
    ): Boolean {

        if (weather == null) {
            return false
        }

        return hourOfDay in WALK_FIRST_HOUR..WALK_LAST_HOUR &&
                weather.isOutdoorFriendly
    }

    /*
     * The COMPLIANCE_DAYS full days before today. Past days
     * rather than today, so compliance is not 0% every
     * morning before the first break.
     */
    fun complianceWindow(
        nowMillis: Long
    ): PeriodRange {

        val today =
            AnalyticsCalculator.periodRange(AnalyticsPeriod.DAILY, nowMillis)

        val start =
            Calendar.getInstance().apply {
                timeInMillis = today.startMillis
                add(Calendar.DAY_OF_YEAR, -COMPLIANCE_DAYS)
            }

        return PeriodRange(
            startMillis = start.timeInMillis,
            endMillis = today.startMillis
        )
    }

    /*
     * Breaks taken / breaks planned over the given records.
     */
    fun complianceRate(
        history: List<ActivityRecord>
    ): Float? {

        val target =
            history.sumOf { it.breakTarget }

        if (target <= 0) {
            return null
        }

        return history.sumOf { it.breaksCompleted }.toFloat() / target
    }
}
