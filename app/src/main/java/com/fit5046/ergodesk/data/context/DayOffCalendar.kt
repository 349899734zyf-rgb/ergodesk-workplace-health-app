package com.fit5046.ergodesk.data.context

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.TemporalAdjusters

/*
 * Weekends and Victorian public holidays, for "Calendar
 * awareness" in Settings: on these days ErgoDesk keeps
 * tracking but sends no break reminders.
 *
 * Holidays are worked out from their rules, so no network
 * or calendar permission is needed. The Friday before the
 * AFL Grand Final is set by the government each year and
 * is not included.
 */
object DayOffCalendar {

    /*
     * Why the date is a day off, or null on a working day.
     */
    fun dayOffReason(
        date: LocalDate
    ): String? {

        return publicHolidays(date.year)[date]
            ?: if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
                "Weekend"
            } else {
                null
            }
    }

    fun publicHolidays(
        year: Int
    ): Map<LocalDate, String> {

        val holidays = mutableMapOf<LocalDate, String>()

        // Moved to Monday when it falls on a weekend.
        fun addObserved(date: LocalDate, name: String) {
            holidays[date] = name
            when (date.dayOfWeek) {
                DayOfWeek.SATURDAY -> holidays[date.plusDays(2)] = "$name (observed)"
                DayOfWeek.SUNDAY -> holidays[date.plusDays(1)] = "$name (observed)"
                else -> Unit
            }
        }

        addObserved(LocalDate.of(year, Month.JANUARY, 1), "New Year's Day")
        addObserved(LocalDate.of(year, Month.JANUARY, 26), "Australia Day")

        holidays[nthWeekday(year, Month.MARCH, DayOfWeek.MONDAY, 2)] = "Labour Day"

        val easter = easterSunday(year)
        holidays[easter.minusDays(2)] = "Good Friday"
        holidays[easter.minusDays(1)] = "Easter Saturday"
        holidays[easter] = "Easter Sunday"
        holidays[easter.plusDays(1)] = "Easter Monday"

        holidays[LocalDate.of(year, Month.APRIL, 25)] = "ANZAC Day"
        holidays[nthWeekday(year, Month.JUNE, DayOfWeek.MONDAY, 2)] = "King's Birthday"
        holidays[nthWeekday(year, Month.NOVEMBER, DayOfWeek.TUESDAY, 1)] = "Melbourne Cup Day"

        // Christmas and Boxing Day each get a weekday off.
        val christmas = LocalDate.of(year, Month.DECEMBER, 25)
        val boxingDay = christmas.plusDays(1)
        holidays[christmas] = "Christmas Day"
        holidays[boxingDay] = "Boxing Day"
        when (christmas.dayOfWeek) {
            DayOfWeek.FRIDAY ->
                holidays[christmas.plusDays(3)] = "Boxing Day (observed)"
            DayOfWeek.SATURDAY -> {
                holidays[christmas.plusDays(2)] = "Christmas Day (observed)"
                holidays[christmas.plusDays(3)] = "Boxing Day (observed)"
            }
            DayOfWeek.SUNDAY ->
                holidays[christmas.plusDays(2)] = "Christmas Day (observed)"
            else -> Unit
        }

        return holidays
    }

    private fun nthWeekday(
        year: Int,
        month: Month,
        dayOfWeek: DayOfWeek,
        n: Int
    ): LocalDate {
        return LocalDate.of(year, month, 1)
            .with(TemporalAdjusters.dayOfWeekInMonth(n, dayOfWeek))
    }

    /*
     * Gregorian Easter (anonymous Gregorian algorithm).
     */
    private fun easterSunday(
        year: Int
    ): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }
}
