package com.fit5046.ergodesk.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/*
 * Saves each account's UserSettings in SharedPreferences.
 *
 * Settings are small key-value preferences, so they don't
 * need a Room table. The workers read them with read();
 * Home and Settings observe them, so a saved change shows
 * up on Home straight away.
 */
object SettingsRepository {

    private const val PREFS_NAME = "user_settings"

    private val settingsByUser =
        MutableStateFlow<Map<String, UserSettings>>(emptyMap())

    fun observe(
        context: Context,
        userId: String
    ): Flow<UserSettings> {
        read(context, userId)
        return settingsByUser
            .map { it[userId] ?: UserSettings() }
            .distinctUntilChanged()
    }

    @Synchronized
    fun read(
        context: Context,
        userId: String
    ): UserSettings {

        settingsByUser.value[userId]?.let { return it }

        val loaded =
            load(prefs(context), userId)

        settingsByUser.value = settingsByUser.value + (userId to loaded)

        return loaded
    }

    @Synchronized
    fun save(
        context: Context,
        userId: String,
        settings: UserSettings
    ) {
        prefs(context).edit()
            .putBoolean(key(userId, "monitoring"), settings.monitoringEnabled)
            .putBoolean(key(userId, "smart_mode"), settings.smartMode)
            .putInt(key(userId, "break_interval"), settings.breakIntervalMinutes)
            .putInt(key(userId, "sensitivity"), settings.movementSensitivity)
            .putInt(key(userId, "quiet_start"), settings.quietStartMinute)
            .putInt(key(userId, "quiet_end"), settings.quietEndMinute)
            .putString(key(userId, "reminder_style"), settings.reminderStyle)
            .putString(key(userId, "break_activity"), settings.breakActivity)
            .putBoolean(key(userId, "weather_aware"), settings.weatherAware)
            .putBoolean(key(userId, "calendar_aware"), settings.calendarAware)
            .apply()

        settingsByUser.value = settingsByUser.value + (userId to settings)
    }

    private fun load(
        prefs: SharedPreferences,
        userId: String
    ): UserSettings {

        val defaults = UserSettings()

        return UserSettings(
            monitoringEnabled = prefs.getBoolean(key(userId, "monitoring"), defaults.monitoringEnabled),
            smartMode = prefs.getBoolean(key(userId, "smart_mode"), defaults.smartMode),
            breakIntervalMinutes = prefs.getInt(key(userId, "break_interval"), defaults.breakIntervalMinutes),
            movementSensitivity = prefs.getInt(key(userId, "sensitivity"), defaults.movementSensitivity),
            quietStartMinute = prefs.getInt(key(userId, "quiet_start"), defaults.quietStartMinute),
            quietEndMinute = prefs.getInt(key(userId, "quiet_end"), defaults.quietEndMinute),
            reminderStyle = prefs.getString(key(userId, "reminder_style"), null) ?: defaults.reminderStyle,
            breakActivity = prefs.getString(key(userId, "break_activity"), null) ?: defaults.breakActivity,
            weatherAware = prefs.getBoolean(key(userId, "weather_aware"), defaults.weatherAware),
            calendarAware = prefs.getBoolean(key(userId, "calendar_aware"), defaults.calendarAware)
        )
    }

    private fun key(
        userId: String,
        name: String
    ) = "$userId/$name"

    private fun prefs(
        context: Context
    ) = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
