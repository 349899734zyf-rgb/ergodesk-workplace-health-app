package com.fit5046.ergodesk.data.sensor

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/*
 * When each account pressed "Take a break now", oldest
 * first. Only the last two days are kept.
 *
 * Kept in SharedPreferences so the workers see it even
 * after a process restart, and exposed as a Flow so Home
 * splits the current sitting session at once.
 */
object ManualBreakStore {

    private const val PREFS_NAME = "manual_breaks"
    private const val KEY_PREFIX = "breaks_"
    private const val KEEP_MILLIS = 2 * 24 * 60 * 60 * 1000L

    private val breaksByUser =
        MutableStateFlow<Map<String, List<Long>>>(emptyMap())

    @Volatile
    private var loaded = false

    fun observe(
        context: Context,
        userId: String
    ): Flow<List<Long>> {
        load(context)
        return breaksByUser.map { it[userId].orEmpty() }.distinctUntilChanged()
    }

    fun read(
        context: Context,
        userId: String
    ): List<Long> {
        load(context)
        return breaksByUser.value[userId].orEmpty()
    }

    fun last(
        context: Context,
        userId: String
    ): Long? {
        return read(context, userId).lastOrNull()
    }

    @Synchronized
    fun record(
        context: Context,
        userId: String,
        timeMillis: Long
    ) {
        load(context)

        val updated =
            (read(context, userId) + timeMillis)
                .filter { it > timeMillis - KEEP_MILLIS }
                .sorted()

        prefs(context).edit()
            .putStringSet(KEY_PREFIX + userId, updated.map { it.toString() }.toSet())
            .apply()

        breaksByUser.value = breaksByUser.value + (userId to updated)
    }

    private fun load(
        context: Context
    ) {
        if (loaded) {
            return
        }
        synchronized(this) {
            if (!loaded) {
                breaksByUser.value =
                    prefs(context).all
                        .filterKeys { it.startsWith(KEY_PREFIX) }
                        .mapNotNull { (key, value) ->
                            (value as? Set<*>)?.let { times ->
                                key.removePrefix(KEY_PREFIX) to
                                        times.mapNotNull { (it as? String)?.toLongOrNull() }.sorted()
                            }
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
