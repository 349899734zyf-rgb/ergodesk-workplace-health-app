package com.fit5046.ergodesk.data.analytics

import com.fit5046.ergodesk.data.model.ActivityRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class AnalyticsPeriod {
    DAILY,
    WEEKLY,
    MONTHLY
}

/*
 * Time window [startMillis, endMillis) used to query records.
 */
data class PeriodRange(
    val startMillis: Long,
    val endMillis: Long
)

data class ActivitySummary(
    val sittingMinutes: Int,
    val activeMinutes: Int,
    val breaksCompleted: Int,
    val breakTarget: Int
)

data class ChartData(
    val sittingValues: List<Float>,
    val activeValues: List<Float>,
    val labels: List<String>
)

/*
 * One day on the break completion trend: breaks taken as a
 * percentage of the day's target, 0..100.
 */
data class TrendPoint(
    val label: String,
    val percent: Float
)

/*
 * Pure calculation helpers for Home and Analytics.
 *
 * Nothing here depends on Compose or Android, so every
 * function can be covered by JVM unit tests.
 */
object AnalyticsCalculator {

    /* ---------------- Period ranges ---------------- */

    fun periodRange(
        period: AnalyticsPeriod,
        selectedDateMillis: Long
    ): PeriodRange {

        val start =
            Calendar.getInstance().apply {
                timeInMillis = selectedDateMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

        when (period) {

            AnalyticsPeriod.DAILY -> Unit

            AnalyticsPeriod.WEEKLY -> {
                // Weeks start on Monday, whatever the device locale.
                val daysSinceMonday =
                    (start.get(Calendar.DAY_OF_WEEK) + 5) % 7
                start.add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
            }

            AnalyticsPeriod.MONTHLY ->
                start.set(Calendar.DAY_OF_MONTH, 1)
        }

        val end =
            (start.clone() as Calendar).apply {
                when (period) {
                    AnalyticsPeriod.DAILY ->
                        add(Calendar.DAY_OF_YEAR, 1)

                    AnalyticsPeriod.WEEKLY ->
                        add(Calendar.DAY_OF_YEAR, 7)

                    AnalyticsPeriod.MONTHLY ->
                        add(Calendar.MONTH, 1)
                }
            }

        return PeriodRange(
            startMillis = start.timeInMillis,
            endMillis = end.timeInMillis
        )
    }

    /*
     * Yesterday, last week or last month, used for the
     * "vs previous period" comparison.
     */
    fun previousPeriodRange(
        period: AnalyticsPeriod,
        selectedDateMillis: Long
    ): PeriodRange {

        val previous =
            Calendar.getInstance().apply {
                timeInMillis = selectedDateMillis
                when (period) {
                    AnalyticsPeriod.DAILY ->
                        add(Calendar.DAY_OF_YEAR, -1)

                    AnalyticsPeriod.WEEKLY ->
                        add(Calendar.DAY_OF_YEAR, -7)

                    AnalyticsPeriod.MONTHLY ->
                        add(Calendar.MONTH, -1)
                }
            }

        return periodRange(
            period = period,
            selectedDateMillis = previous.timeInMillis
        )
    }

    /* ---------------- Totals ---------------- */

    fun summarize(
        records: List<ActivityRecord>
    ): ActivitySummary {

        return ActivitySummary(
            sittingMinutes = records.sumOf { it.sittingMinutes },
            activeMinutes = records.sumOf { it.activeMinutes },
            breaksCompleted = records.sumOf { it.breaksCompleted },
            breakTarget = records.sumOf { it.breakTarget }
        )
    }

    /*
     * Returns 0 when there is no previous data, instead
     * of dividing by zero.
     */
    fun percentageChange(
        current: Int,
        previous: Int
    ): Int {

        if (previous <= 0) {
            return 0
        }

        return (
                (current - previous).toDouble() /
                        previous.toDouble() *
                        100.0
                ).roundToInt()
    }

    /* ---------------- Chart ---------------- */

    fun buildChartData(
        period: AnalyticsPeriod,
        records: List<ActivityRecord>
    ): ChartData {

        if (records.isEmpty()) {

            return ChartData(
                sittingValues = emptyList(),
                activeValues = emptyList(),
                labels = emptyList()
            )
        }

        return when (period) {

            AnalyticsPeriod.DAILY -> {

                /*
                 * A daily ActivityRecord only stores daily totals,
                 * so the total is spread over time blocks using
                 * weights. Replace with real hourly data once
                 * activity sensing records it.
                 */
                val record =
                    records.first()

                val sittingWeights =
                    listOf(0.15f, 0.19f, 0.17f, 0.18f, 0.16f, 0.15f)

                val activeWeights =
                    listOf(0.08f, 0.10f, 0.19f, 0.22f, 0.20f, 0.21f)

                ChartData(
                    sittingValues =
                        sittingWeights.map {
                            record.sittingMinutes * it / 60f
                        },
                    activeValues =
                        activeWeights.map {
                            record.activeMinutes * it / 60f
                        },
                    labels =
                        listOf("8am", "10am", "12pm", "2pm", "4pm", "6pm")
                )
            }

            AnalyticsPeriod.WEEKLY -> {

                ChartData(
                    sittingValues =
                        records.map { it.sittingMinutes / 60f },
                    activeValues =
                        records.map { it.activeMinutes / 60f },
                    labels =
                        records.map { weekdayLabel(it.timestamp) }
                )
            }

            AnalyticsPeriod.MONTHLY -> {

                val weeklyGroups =
                    records.chunked(7)

                ChartData(
                    sittingValues =
                        weeklyGroups.map { week ->
                            week.sumOf { it.sittingMinutes } / 60f
                        },
                    activeValues =
                        weeklyGroups.map { week ->
                            week.sumOf { it.activeMinutes } / 60f
                        },
                    labels =
                        weeklyGroups.indices.map { "W${it + 1}" }
                )
            }
        }
    }

    /* ---------------- Break trend ---------------- */

    /*
     * Daily break completion for the line chart, oldest
     * first. Weekly views label points by weekday, monthly
     * views by day of the month.
     */
    fun buildBreakTrend(
        period: AnalyticsPeriod,
        records: List<ActivityRecord>
    ): List<TrendPoint> {

        return records
            .sortedBy { it.timestamp }
            .map { record ->
                TrendPoint(
                    label =
                        if (period == AnalyticsPeriod.MONTHLY) {
                            SimpleDateFormat("d", Locale.ENGLISH)
                                .format(Date(record.timestamp))
                        } else {
                            weekdayLabel(record.timestamp)
                        },
                    percent =
                        if (record.breakTarget > 0) {
                            (record.breaksCompleted * 100f / record.breakTarget)
                                .coerceIn(0f, 100f)
                        } else {
                            0f
                        }
                )
            }
    }

    /* ---------------- Text formatting ---------------- */

    fun formatMinutes(
        totalMinutes: Int
    ): String {

        if (totalMinutes <= 0) {
            return "0m"
        }

        val hours =
            totalMinutes / 60

        val minutes =
            totalMinutes % 60

        return when {
            hours == 0 -> "${minutes}m"
            minutes == 0 -> "${hours}h"
            else -> "${hours}h ${minutes}m"
        }
    }

    fun formatPercentageChange(
        change: Int
    ): String {

        return when {
            change > 0 -> "↑ ${change}%"
            change < 0 -> "↓ ${abs(change)}%"
            else -> "—"
        }
    }

    fun formatNumberChange(
        change: Int
    ): String {

        return when {
            change > 0 -> "↑ $change"
            change < 0 -> "↓ ${abs(change)}"
            else -> "—"
        }
    }

    fun comparisonLabel(
        period: AnalyticsPeriod
    ): String {

        return when (period) {
            AnalyticsPeriod.DAILY -> "vs yesterday"
            AnalyticsPeriod.WEEKLY -> "vs last week"
            AnalyticsPeriod.MONTHLY -> "vs last month"
        }
    }

    fun createInsight(
        sittingChange: Int,
        activeChange: Int,
        breaksCompleted: Int,
        breakTarget: Int
    ): String {

        val breakCompliance =
            if (breakTarget > 0) {
                breaksCompleted.toFloat() / breakTarget.toFloat()
            } else {
                0f
            }

        return when {

            sittingChange >= 10 ->
                "Your sitting time has increased compared with the previous period. " +
                        "Try taking a short movement break soon."

            sittingChange <= -10 ->
                "Your sitting time has decreased compared with the previous period. " +
                        "Keep maintaining regular movement breaks."

            activeChange >= 10 ->
                "Your active time is improving. Keep up the regular movement breaks."

            breakCompliance < 0.5f ->
                "You have completed fewer than half of your planned breaks. " +
                        "A short ergonomic stretch may help."

            breakCompliance >= 0.8f ->
                "You are completing most of your planned breaks. " +
                        "Keep maintaining this routine."

            else ->
                "Your activity pattern is relatively balanced. " +
                        "Continue taking regular ergonomic breaks."
        }
    }

    fun formatPeriodLabel(
        millis: Long,
        period: AnalyticsPeriod
    ): String {

        return when (period) {

            AnalyticsPeriod.DAILY ->
                SimpleDateFormat("EEE, d MMM yyyy", Locale.ENGLISH)
                    .format(Date(millis))

            AnalyticsPeriod.WEEKLY -> {

                val range =
                    periodRange(AnalyticsPeriod.WEEKLY, millis)

                val lastDay =
                    Calendar.getInstance().apply {
                        timeInMillis = range.endMillis
                        add(Calendar.DAY_OF_YEAR, -1)
                    }

                val start =
                    SimpleDateFormat("d MMM", Locale.ENGLISH)
                        .format(Date(range.startMillis))

                val end =
                    SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)
                        .format(lastDay.time)

                "$start – $end"
            }

            AnalyticsPeriod.MONTHLY ->
                SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
                    .format(Date(millis))
        }
    }

    fun weekdayLabel(
        millis: Long
    ): String {

        return SimpleDateFormat("EEE", Locale.ENGLISH)
            .format(Date(millis))
    }
}
