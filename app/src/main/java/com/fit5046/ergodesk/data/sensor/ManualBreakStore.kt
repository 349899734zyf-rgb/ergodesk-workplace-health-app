package com.fit5046.ergodesk.data.sensor

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/*
 * When the user last pressed "Take a break now".
 *
 * Kept in SharedPreferences so ContextEvaluationWorker
 * sees it even after a process restart, and exposed as a
 * StateFlow so Home restarts its sitting timer at once.
 */
object ManualBreakStore {

    private const val PREFS_NAME = "manual_break"
    private const val KEY_LAST_BREAK = "last_break_millis"

    private val lastBreak =
        MutableStateFlow<Long?>(null)

    @Volatile
    private var loaded = false

    fun observe(
        context: Context
    ): StateFlow<Long?> {
        load(context)
        return lastBreak.asStateFlow()
    }

    fun read(
        context: Context
    ): Long? {
        load(context)
        return lastBreak.value
    }

    fun record(
        context: Context,
        timeMillis: Long
    ) {
        load(context)
        prefs(context).edit().putLong(KEY_LAST_BREAK, timeMillis).apply()
        lastBreak.value = timeMillis
    }

    private fun load(
        context: Context
    ) {
        if (loaded) {
            return
        }
        synchronized(this) {
            if (!loaded) {
                lastBreak.value =
                    prefs(context).getLong(KEY_LAST_BREAK, 0L).takeIf { it > 0L }
                loaded = true
            }
        }
    }

    private fun prefs(
        context: Context
    ) = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
