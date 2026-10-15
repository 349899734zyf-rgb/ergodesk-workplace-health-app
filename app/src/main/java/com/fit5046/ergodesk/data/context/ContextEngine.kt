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
    DAY_OFF,
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
 * Minutes after midnight. startMinute > endMinute wraps
 * past midnight, e.g. 22:00 to 07:00.
 */
data class QuietHours(
    val startMinute: Int,
    val endMinute: Int
)

/*
 * Everything ContextEngine decides from: the sensed sitting
 * bout, break history, time of day, weather and the user's
 * own settings.
 */
data class ContextInput(
    val sittingBoutMinutes: Int,
    // Share of planned breaks taken recently, 0..1;
    // null when there is no history yet.
    val complianceRate: Float?,
    val minuteOfDay: Int,
    // From Settings: the user's interval and whether ErgoDesk
    // may adapt it to their break history.
    val baseIntervalMinutes: Int = ContextEngine.BASE_INTERVAL_MINUTES,
    val smartMode: Boolean = true,
    val inMeeting: Boolean = false,
    // "Weekend" or a public holiday's name when Calendar
    // awareness is on and today is a day off; else null.
    val dayOffReason: String? = null,
    // Null when there is no fresh weather or the user turned
    // weather-based suggestions off.
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
 * decision (A2 report, section 1.4), personalised by the
 * user's Settings. Pure Kotlin; ContextEvaluationWorker
 * gathers the inputs and schedules the alarm.
 */
object ContextEngine {

    // Default break interval; users can change it in Settings.
    const val BASE_INTERVAL_MINUTES = 30

    // Never tighten below this, whatever the base interval.
    private const val MIN_INTERVAL_MINUTES = 15

    const val LOW_COMPLIANCE = 0.4f
    const val HIGH_COMPLIANCE = 0.8f

    const val COMPLIANCE_DAYS = 7

    val DEFAULT_QUIET_HOURS = QuietHours(startMinute = 22 * 60, endMinute = 7 * 60)

    // Outdoor walks are only suggested in daylight working
    // hours, and only when the weather suits one.
    private const val WALK_FIRST_HOUR = 8
    private const val WALK_LAST_HOUR = 18

    fun evaluate(
        input: ContextInput
    ): ContextDecision {

        // With smart mode off the user's interval is used as is.
        val compliance =
            input.complianceRate.takeIf { input.smartMode }

        val interval =
            breakInterval(input.baseIntervalMinutes, compliance)

        return ContextDecision(
            intervalMinutes = interval,
            minutesUntilDue = interval - input.sittingBoutMinutes,
            holdReason = holdReason(input),
            style = reminderStyle(compliance),
            suggestion =
                if (isWalkWeather(input.weather, input.minuteOfDay / 60)) {
                    BreakSuggestion.OUTDOOR_WALK
                } else {
                    BreakSuggestion.DESK_STRETCH
                }
        )
    }

    /*
     * Users who skip most breaks get fewer, quieter
     * reminders instead of being nagged (+50%); users who
     * keep up get a tighter interval (-5 min). With the
     * default 30 minutes that is 45 and 25, as in the A2
     * report.
     */
    fun breakInterval(
        baseIntervalMinutes: Int,
        complianceRate: Float?
    ): Int {

        return when {
            complianceRate == null -> baseIntervalMinutes
            complianceRate < LOW_COMPLIANCE -> baseIntervalMinutes + baseIntervalMinutes / 2
            complianceRate > HIGH_COMPLIANCE ->
                (baseIntervalMinutes - 5).coerceAtLeast(MIN_INTERVAL_MINUTES)
            else -> baseIntervalMinutes
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
            input.dayOffReason != null -> HoldReason.DAY_OFF
            isQuietTime(input.minuteOfDay, input.quietHours) -> HoldReason.QUIET_HOURS
            else -> null
        }
    }

    fun isQuietTime(
        minuteOfDay: Int,
        quietHours: QuietHours
    ): Boolean {

        return if (quietHours.startMinute <= quietHours.endMinute) {
            minuteOfDay >= quietHours.startMinute && minuteOfDay < quietHours.endMinute
        } else {
            minuteOfDay >= quietHours.startMinute || minuteOfDay < quietHours.endMinute
        }
    }

    /*
     * Minutes after midnight, local time.
     */
    fun minuteOfDay(
        timeMillis: Long
    ): Int {

        val calendar =
            Calendar.getInstance().apply { this.timeInMillis = timeMillis }

        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
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
