package com.fit5046.ergodesk.data.model

/*
 * One account's activity on one day.
 *
 * The hourly lists hold 24 values (minutes in each hour of
 * the day) when the day was tracked by the sensor; they
 * are empty for days without hourly detail, such as the
 * demo account's sample history.
 */
data class ActivityRecord(
    val timestamp: Long,
    val sittingMinutes: Int,
    val activeMinutes: Int,
    val breaksCompleted: Int,
    val breakTarget: Int,
    val hourlySittingMinutes: List<Int> = emptyList(),
    val hourlyActiveMinutes: List<Int> = emptyList()
)
