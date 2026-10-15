package com.fit5046.ergodesk.data.sensor

import android.content.Context
import com.fit5046.ergodesk.data.context.QuietHours
import java.time.Instant
import java.time.ZoneId

/*
 * When sensing was on for each account, as [start, end)
 * intervals. Sensing is on while ErgoDesk is in the
 * foreground, and also in the background when "Enable
 * sitting monitoring" is on in Settings. Sessions are only
 * built inside these intervals, so time when sensing was
 * off never counts as sitting or as a break.
 *
 * Kept in SharedPreferences; only the last two days stay.
 */
object TrackingLog {

    private const val PREFS_NAME = "tracking_log"
    private const val KEEP_MILLIS = 2 * 24 * 60 * 60 * 1000L

    // end == null: still running.
    private data class Interval(
        val start: Long,
        val end: Long?
    )

    /*
     * Intervals overlapping [fromMillis, toMillis), clipped
     * to it. An account with nothing recorded yet has been
     * tracked since it was first signed in on this device.
     */
    @Synchronized
    fun intervals(
        context: Context,
        userId: String,
        trackingStartMillis: Long,
        fromMillis: Long,
        toMillis: Long
    ): List<LongRange> {

        return load(context, userId, trackingStartMillis)
            .mapNotNull { interval ->
                val start = maxOf(interval.start, fromMillis)
                val end = minOf(interval.end ?: toMillis, toMillis)
                if (start < end) start until end else null
            }
    }

    /*
     * The same intervals with quiet hours cut out. Quiet
     * hours are usually overnight, when the phone lies still
     * on a desk or bedside table and would read as sitting,
     * so nothing in them counts as sitting or as a break.
     */
    fun withoutQuietHours(
        intervals: List<LongRange>,
        quietHours: QuietHours,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<LongRange> {

        if (intervals.isEmpty() || quietHours.startMinute == quietHours.endMinute) {
            return intervals
        }

        val firstDay =
            Instant.ofEpochMilli(intervals.first().first).atZone(zone).toLocalDate().minusDays(1)

        val lastDay =
            Instant.ofEpochMilli(intervals.last().last).atZone(zone).toLocalDate()

        // Every quiet window that could touch the intervals;
        // one that wraps past midnight ends the next day.
        val quietWindows =
            generateSequence(firstDay) { it.plusDays(1) }
                .takeWhile { !it.isAfter(lastDay) }
                .map { day ->
                    val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
                    val start = dayStart + quietHours.startMinute * 60_000L
                    val end =
                        if (quietHours.startMinute < quietHours.endMinute) {
                            dayStart + quietHours.endMinute * 60_000L
                        } else {
                            day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() +
                                    quietHours.endMinute * 60_000L
                        }
                    start until end
                }
                .toList()

        return intervals.flatMap { interval ->
            quietWindows.fold(listOf(interval)) { pieces, quiet ->
                pieces.flatMap { piece ->
                    listOfNotNull(
                        (piece.first until minOf(piece.last + 1, quiet.first))
                            .takeIf { !it.isEmpty() },
                        (maxOf(piece.first, quiet.last + 1)..piece.last)
                            .takeIf { !it.isEmpty() }
                    )
                }
            }
        }
    }

    /*
     * Opens an interval when sensing turns on and closes the
     * running one when it turns off; repeated calls with the
     * same value change nothing.
     */
    @Synchronized
    fun setActive(
        context: Context,
        userId: String,
        trackingStartMillis: Long,
        active: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ) {

        val intervals =
            load(context, userId, trackingStartMillis).toMutableList()

        val running =
            intervals.lastOrNull()?.takeIf { it.end == null }

        when {
            active && running == null ->
                intervals += Interval(nowMillis, null)

            !active && running != null ->
                intervals[intervals.lastIndex] = running.copy(end = nowMillis)

            else -> return
        }

        save(context, userId, intervals.filter { (it.end ?: nowMillis) > nowMillis - KEEP_MILLIS })
    }

    private fun load(
        context: Context,
        userId: String,
        trackingStartMillis: Long
    ): List<Interval> {

        val stored =
            prefs(context).getString(userId, null)
                ?: return listOf(Interval(trackingStartMillis, null))

        return stored
            .split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { part ->
                val (start, end) = part.split("-", limit = 2).let { it[0] to it.getOrNull(1) }
                start.toLongOrNull()?.let { Interval(it, end?.toLongOrNull()) }
            }
    }

    private fun save(
        context: Context,
        userId: String,
        intervals: List<Interval>
    ) {
        prefs(context).edit()
            .putString(userId, intervals.joinToString(",") { "${it.start}-${it.end ?: ""}" })
            .apply()
    }

    private fun prefs(
        context: Context
    ) = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
