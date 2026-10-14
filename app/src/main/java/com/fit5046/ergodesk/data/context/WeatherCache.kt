package com.fit5046.ergodesk.data.context

import android.content.Context

/*
 * Last weather fetched by WeatherSyncWorker, kept in
 * SharedPreferences so ContextEvaluationWorker can read it
 * even when the app process was restarted in between.
 */
class WeatherCache(
    context: Context
) {

    private val prefs =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(
        snapshot: WeatherSnapshot,
        fetchedAtMillis: Long = System.currentTimeMillis()
    ) {
        prefs.edit()
            .putFloat(KEY_TEMPERATURE, snapshot.temperatureC.toFloat())
            .putString(KEY_CONDITION, snapshot.condition)
            .putBoolean(KEY_OUTDOOR_FRIENDLY, snapshot.isOutdoorFriendly)
            .putLong(KEY_FETCHED_AT, fetchedAtMillis)
            .apply()
    }

    /*
     * Null when nothing was fetched yet or the data is too
     * old to describe the weather outside right now.
     */
    fun read(
        nowMillis: Long = System.currentTimeMillis()
    ): WeatherSnapshot? {

        val fetchedAt =
            prefs.getLong(KEY_FETCHED_AT, 0L)

        if (fetchedAt == 0L || nowMillis - fetchedAt > MAX_AGE_MILLIS) {
            return null
        }

        return WeatherSnapshot(
            temperatureC = prefs.getFloat(KEY_TEMPERATURE, 0f).toDouble(),
            condition = prefs.getString(KEY_CONDITION, "") ?: "",
            isOutdoorFriendly = prefs.getBoolean(KEY_OUTDOOR_FRIENDLY, false)
        )
    }

    private companion object {

        const val PREFS_NAME = "weather_cache"

        const val KEY_TEMPERATURE = "temperature_c"
        const val KEY_CONDITION = "condition"
        const val KEY_OUTDOOR_FRIENDLY = "outdoor_friendly"
        const val KEY_FETCHED_AT = "fetched_at"

        // Weather an afternoon old says little about now.
        const val MAX_AGE_MILLIS = 3 * 60 * 60 * 1000L
    }
}
