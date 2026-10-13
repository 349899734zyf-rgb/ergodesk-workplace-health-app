package com.fit5046.ergodesk.data.context

import com.fit5046.ergodesk.data.model.ActivityRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextEngineTest {

    private val niceWeather = WeatherSnapshot(temperatureC = 20.0, precipitationMm = 0.0)

    private fun input(
        sitting: Int = 20,
        compliance: Float? = 0.6f,
        hour: Int = 10,
        inMeeting: Boolean = false,
        weather: WeatherSnapshot? = null
    ) = ContextInput(sitting, compliance, hour, inMeeting, weather)

    /* ---------------- Interval and style ---------------- */

    @Test
    fun lowCompliance_relaxesIntervalAndSilencesAlerts() {
        val decision = ContextEngine.evaluate(input(compliance = 0.39f))

        assertEquals(45, decision.intervalMinutes)
        assertEquals(ReminderStyle.SILENT_BANNER, decision.style)
    }

    @Test
    fun highCompliance_tightensInterval() {
        val decision = ContextEngine.evaluate(input(compliance = 0.81f))

        assertEquals(25, decision.intervalMinutes)
        assertEquals(ReminderStyle.STANDARD, decision.style)
    }

    @Test
    fun complianceAtBoundaries_keepsBaseInterval() {
        assertEquals(30, ContextEngine.breakInterval(0.4f))
        assertEquals(30, ContextEngine.breakInterval(0.8f))
        assertEquals(30, ContextEngine.breakInterval(null))
    }

    @Test
    fun minutesUntilDue_countsDownFromInterval() {
        assertEquals(10, ContextEngine.evaluate(input(sitting = 20)).minutesUntilDue)
        assertEquals(-5, ContextEngine.evaluate(input(sitting = 35)).minutesUntilDue)
    }

    /* ---------------- Holds ---------------- */

    @Test
    fun meeting_holdsReminder() {
        assertEquals(HoldReason.MEETING, ContextEngine.evaluate(input(inMeeting = true)).holdReason)
    }

    @Test
    fun quietHours_wrapPastMidnight() {
        val quiet = QuietHours(startHour = 22, endHour = 7)

        assertTrue(ContextEngine.isQuietHour(22, quiet))
        assertTrue(ContextEngine.isQuietHour(3, quiet))
        assertFalse(ContextEngine.isQuietHour(7, quiet))
        assertFalse(ContextEngine.isQuietHour(21, quiet))
    }

    @Test
    fun quietHours_withinOneDay() {
        val lunch = QuietHours(startHour = 12, endHour = 13)

        assertTrue(ContextEngine.isQuietHour(12, lunch))
        assertFalse(ContextEngine.isQuietHour(13, lunch))
    }

    @Test
    fun workingHours_withoutMeeting_noHold() {
        assertNull(ContextEngine.evaluate(input(hour = 10)).holdReason)
    }

    /* ---------------- Suggestion ---------------- */

    @Test
    fun goodDaytimeWeather_suggestsOutdoorWalk() {
        val decision = ContextEngine.evaluate(input(weather = niceWeather))

        assertEquals(BreakSuggestion.OUTDOOR_WALK, decision.suggestion)
    }

    @Test
    fun rainColdNightOrNoData_suggestsDeskStretch() {
        assertFalse(ContextEngine.isWalkWeather(niceWeather.copy(precipitationMm = 1.2), 10))
        assertFalse(ContextEngine.isWalkWeather(niceWeather.copy(temperatureC = 5.0), 10))
        assertFalse(ContextEngine.isWalkWeather(niceWeather, 20))
        assertFalse(ContextEngine.isWalkWeather(null, 10))
    }

    /* ---------------- Compliance ---------------- */

    @Test
    fun complianceRate_isBreaksTakenOverPlanned() {
        val history = listOf(
            ActivityRecord(0L, 300, 100, 2, 8),
            ActivityRecord(1L, 300, 100, 6, 8)
        )

        assertEquals(0.5f, ContextEngine.complianceRate(history)!!, 0.0001f)
    }

    @Test
    fun complianceRate_noHistory_isNull() {
        assertNull(ContextEngine.complianceRate(emptyList()))
    }
}
