package com.fit5046.ergodesk.data.sensor

import android.content.Context

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
