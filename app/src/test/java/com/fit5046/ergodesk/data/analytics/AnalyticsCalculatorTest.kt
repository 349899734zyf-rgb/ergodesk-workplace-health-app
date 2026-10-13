package com.fit5046.ergodesk.data.analytics

import com.fit5046.ergodesk.data.model.ActivityRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AnalyticsCalculatorTest {

    private fun millis(year: Int, month: Int, day: Int, hour: Int = 12): Long {
        return Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, 0, 0)
        }.timeInMillis
    }

    private fun record(
        timestamp: Long,
        sitting: Int = 300,
        active: Int = 100,
        breaks: Int = 5,
        target: Int = 8
    ) = ActivityRecord(timestamp, sitting, active, breaks, target)

    /* ---------------- periodRange ---------------- */

    @Test
    fun dailyRange_coversWholeSelectedDay() {
        val range = AnalyticsCalculator.periodRange(
            AnalyticsPeriod.DAILY,
            millis(2026, Calendar.OCTOBER, 13, hour = 15)
        )

        assertEquals(millis(2026, Calendar.OCTOBER, 13, hour = 0), range.startMillis)
        assertEquals(millis(2026, Calendar.OCTOBER, 14, hour = 0), range.endMillis)
    }

    @Test
    fun weeklyRange_startsOnMonday_evenWhenSundayIsSelected() {
        // 18 Oct 2026 is a Sunday; its week is Mon 12 – Sun 18.
        val range = AnalyticsCalculator.periodRange(
            AnalyticsPeriod.WEEKLY,
            millis(2026, Calendar.OCTOBER, 18)
        )

        assertEquals(millis(2026, Calendar.OCTOBER, 12, hour = 0), range.startMillis)
        assertEquals(millis(2026, Calendar.OCTOBER, 19, hour = 0), range.endMillis)
    }

    @Test
    fun monthlyRange_coversWholeMonth() {
        val range = AnalyticsCalculator.periodRange(
            AnalyticsPeriod.MONTHLY,
            millis(2026, Calendar.FEBRUARY, 20)
        )

        assertEquals(millis(2026, Calendar.FEBRUARY, 1, hour = 0), range.startMillis)
        assertEquals(millis(2026, Calendar.MARCH, 1, hour = 0), range.endMillis)
    }

    @Test
    fun previousWeeklyRange_isTheWeekBefore() {
        val range = AnalyticsCalculator.previousPeriodRange(
            AnalyticsPeriod.WEEKLY,
            millis(2026, Calendar.OCTOBER, 14)
        )

        assertEquals(millis(2026, Calendar.OCTOBER, 5, hour = 0), range.startMillis)
        assertEquals(millis(2026, Calendar.OCTOBER, 12, hour = 0), range.endMillis)
    }

    @Test
    fun previousMonthlyRange_handlesYearBoundary() {
        val range = AnalyticsCalculator.previousPeriodRange(
            AnalyticsPeriod.MONTHLY,
            millis(2026, Calendar.JANUARY, 15)
        )

        assertEquals(millis(2025, Calendar.DECEMBER, 1, hour = 0), range.startMillis)
        assertEquals(millis(2026, Calendar.JANUARY, 1, hour = 0), range.endMillis)
    }

    /* ---------------- summarize / percentageChange ---------------- */

    @Test
    fun summarize_addsUpAllRecords() {
        val summary = AnalyticsCalculator.summarize(
            listOf(
                record(1L, sitting = 300, active = 100, breaks = 5, target = 8),
                record(2L, sitting = 360, active = 90, breaks = 7, target = 8)
            )
        )

        assertEquals(ActivitySummary(660, 190, 12, 16), summary)
    }

    @Test
    fun summarize_emptyList_isAllZero() {
        assertEquals(
            ActivitySummary(0, 0, 0, 0),
            AnalyticsCalculator.summarize(emptyList())
        )
    }

    @Test
    fun percentageChange_roundsToNearestPercent() {
        assertEquals(10, AnalyticsCalculator.percentageChange(current = 330, previous = 300))
        assertEquals(-33, AnalyticsCalculator.percentageChange(current = 200, previous = 300))
    }

    @Test
    fun percentageChange_noPreviousData_returnsZeroInsteadOfDividingByZero() {
        assertEquals(0, AnalyticsCalculator.percentageChange(current = 300, previous = 0))
    }

    /* ---------------- buildChartData ---------------- */

    @Test
    fun chartData_empty_whenNoRecords() {
        val chart = AnalyticsCalculator.buildChartData(AnalyticsPeriod.WEEKLY, emptyList())

        assertTrue(chart.sittingValues.isEmpty())
        assertTrue(chart.labels.isEmpty())
    }

    @Test
    fun chartData_weekly_hasOneBarPerDayInHours() {
        val monday = millis(2026, Calendar.OCTOBER, 12)
        val tuesday = millis(2026, Calendar.OCTOBER, 13)

        val chart = AnalyticsCalculator.buildChartData(
            AnalyticsPeriod.WEEKLY,
            listOf(record(monday, sitting = 360), record(tuesday, sitting = 300))
        )

        assertEquals(listOf(6f, 5f), chart.sittingValues)
        assertEquals(listOf("Mon", "Tue"), chart.labels)
    }

    @Test
    fun chartData_daily_blocksAddUpToDailyTotal() {
        val chart = AnalyticsCalculator.buildChartData(
            AnalyticsPeriod.DAILY,
            listOf(record(1L, sitting = 360, active = 120))
        )

        assertEquals(6, chart.labels.size)
        assertEquals(6f, chart.sittingValues.sum(), 0.01f)
        assertEquals(2f, chart.activeValues.sum(), 0.01f)
    }

    @Test
    fun chartData_monthly_groupsRecordsIntoWeeks() {
        val records = (1..10).map { record(it.toLong(), sitting = 60) }

        val chart = AnalyticsCalculator.buildChartData(AnalyticsPeriod.MONTHLY, records)

        assertEquals(listOf("W1", "W2"), chart.labels)
        assertEquals(listOf(7f, 3f), chart.sittingValues)
    }

    /* ---------------- formatting ---------------- */

    @Test
    fun formatMinutes_coversAllCases() {
        assertEquals("0m", AnalyticsCalculator.formatMinutes(0))
        assertEquals("45m", AnalyticsCalculator.formatMinutes(45))
        assertEquals("2h", AnalyticsCalculator.formatMinutes(120))
        assertEquals("6h 20m", AnalyticsCalculator.formatMinutes(380))
    }

    @Test
    fun formatChanges_showArrowOrDash() {
        assertEquals("↑ 12%", AnalyticsCalculator.formatPercentageChange(12))
        assertEquals("↓ 5%", AnalyticsCalculator.formatPercentageChange(-5))
        assertEquals("—", AnalyticsCalculator.formatPercentageChange(0))
        assertEquals("↓ 2", AnalyticsCalculator.formatNumberChange(-2))
    }

    @Test
    fun weeklyLabel_showsMondayToSunday() {
        assertEquals(
            "12 Oct – 18 Oct 2026",
            AnalyticsCalculator.formatPeriodLabel(
                millis(2026, Calendar.OCTOBER, 15),
                AnalyticsPeriod.WEEKLY
            )
        )
    }

    /* ---------------- insight ---------------- */

    @Test
    fun insight_sittingIncrease_takesPriority() {
        val text = AnalyticsCalculator.createInsight(
            sittingChange = 15,
            activeChange = 20,
            breaksCompleted = 8,
            breakTarget = 8
        )

        assertTrue(text.startsWith("Your sitting time has increased"))
    }

    @Test
    fun insight_lowBreakCompliance() {
        val text = AnalyticsCalculator.createInsight(
            sittingChange = 0,
            activeChange = 0,
            breaksCompleted = 3,
            breakTarget = 8
        )

        assertTrue(text.startsWith("You have completed fewer than half"))
    }
}
