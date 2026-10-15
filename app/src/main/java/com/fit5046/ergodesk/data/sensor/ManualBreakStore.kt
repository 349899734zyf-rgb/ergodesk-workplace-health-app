package com.fit5046.ergodesk.data.sensor

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/*
 * When each account last pressed "Take a break now".
 *
 * Kept in SharedPreferences so ContextEvaluationWorker
 * sees it even after a process restart, and exposed as a
 * Flow so Home restarts its sitting timer at once.
 */
object ManualBreakStore {

    private const val PREFS_NAME = "manual_break"
    private const val KEY_PREFIX = "last_break_millis_"

    private val lastBreaks =
        MutableStateFlow<Map<String, Long>>(emptyMap())

    @Volatile
    private var loaded = false

    fun observe(
        context: Context,
        userId: String
    ): Flow<Long?> {
        load(context)
        return lastBreaks.map { it[userId] }.distinctUntilChanged()
    }

    fun read(
        context: Context,
        userId: String
    ): Long? {
        load(context)
        return lastBreaks.value[userId]
    }

    fun record(
        context: Context,
        userId: String,
        timeMillis: Long
    ) {
        load(context)
        prefs(context).edit().putLong(KEY_PREFIX + userId, timeMillis).apply()
        lastBreaks.value = lastBreaks.value + (userId to timeMillis)
    }

    private fun load(
        context: Context
    ) {
        if (loaded) {
            return
        }
        synchronized(this) {
            if (!loaded) {
                lastBreaks.value =
                    prefs(context).all
                        .filterKeys { it.startsWith(KEY_PREFIX) }
                        .mapNotNull { (key, value) ->
                            (value as? Long)?.let { key.removePrefix(KEY_PREFIX) to it }
                        }
                        .toMap()
                loaded = true
            }
        }
    }

    private fun prefs(
        context: Context
    ) = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
