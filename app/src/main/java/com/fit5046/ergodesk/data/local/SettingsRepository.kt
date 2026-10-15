package com.fit5046.ergodesk.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

//Create a DataStore to save app settings locally
private val Context.settingsDataStore by preferencesDataStore(name = "ergodesk_settings")

//Values shown on the Settings screen
data class AppSettings(
    val monitoringEnabled: Boolean = true,
    val smartMode: Boolean = true,
    val weatherAware: Boolean = true,
    val calendarAware: Boolean = false,
    val breakInterval: Float = 30f,
    val movementSensitivity: Float = 3f,
    val quietStart: String = "12:30",
    val quietEnd: String = "13:30",
    val reminderStyle: String = "Notification",
    val breakActivity: String = "Stretch"
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val MONITORING = booleanPreferencesKey("monitoring")
        val SMART_MODE = booleanPreferencesKey("smart_mode")
        val WEATHER = booleanPreferencesKey("weather")
        val CALENDAR = booleanPreferencesKey("calendar")
        val BREAK_INTERVAL = floatPreferencesKey("break_interval")
        val SENSITIVITY = floatPreferencesKey("sensitivity")
        val QUIET_START = stringPreferencesKey("quiet_start")
        val QUIET_END = stringPreferencesKey("quiet_end")
        val REMINDER_STYLE = stringPreferencesKey("reminder_style")
        val BREAK_ACTIVITY = stringPreferencesKey("break_activity")
    }

    //Saved settings or default values if nothing is saved
    val settings: Flow<AppSettings> =
        context.settingsDataStore.data.map { preferences ->
            AppSettings(
                monitoringEnabled = preferences[Keys.MONITORING] ?: true,
                smartMode = preferences[Keys.SMART_MODE] ?: true,
                weatherAware = preferences[Keys.WEATHER] ?: true,
                calendarAware = preferences[Keys.CALENDAR] ?: false,
                breakInterval = preferences[Keys.BREAK_INTERVAL] ?: 30f,
                movementSensitivity = preferences[Keys.SENSITIVITY] ?: 3f,
                quietStart = preferences[Keys.QUIET_START] ?: "12:30",
                quietEnd = preferences[Keys.QUIET_END] ?: "13:30",
                reminderStyle = preferences[Keys.REMINDER_STYLE] ?: "Notification",
                breakActivity = preferences[Keys.BREAK_ACTIVITY] ?: "Stretch"
            )
        }

    //Save all Settings screen values
    suspend fun saveSettings(settings: AppSettings) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.MONITORING] = settings.monitoringEnabled
            preferences[Keys.SMART_MODE] = settings.smartMode
            preferences[Keys.WEATHER] = settings.weatherAware
            preferences[Keys.CALENDAR] = settings.calendarAware
            preferences[Keys.BREAK_INTERVAL] = settings.breakInterval
            preferences[Keys.SENSITIVITY] = settings.movementSensitivity
            preferences[Keys.QUIET_START] = settings.quietStart
            preferences[Keys.QUIET_END] = settings.quietEnd
            preferences[Keys.REMINDER_STYLE] = settings.reminderStyle
            preferences[Keys.BREAK_ACTIVITY] = settings.breakActivity
        }
    }
}