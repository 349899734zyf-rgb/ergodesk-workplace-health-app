package com.fit5046.ergodesk.data.repository

import com.fit5046.ergodesk.BuildConfig
import com.fit5046.ergodesk.data.context.WeatherSnapshot
import com.fit5046.ergodesk.data.network.WeatherRetrofitObject
import com.fit5046.ergodesk.data.network.model.WeatherAdvice
import com.fit5046.ergodesk.data.network.model.WeatherResponse
import java.util.Calendar
import kotlin.math.roundToInt

/**
 * Repository layer managing live weather data retrieval and contextual ergonomic advice.
 * Connects Retrofit networking with the UI presentation layer.
 * Member 4 responsibility.
 */
class WeatherRepository(
    private val apiKey: String = BuildConfig.WEATHER_API_KEY
) {

    private val weatherService = WeatherRetrofitObject.retrofitService

    companion object {
        // Daylight walking hours matching ContextEngine (08:00 - 18:00)
        const val WALK_FIRST_HOUR = 8
        const val WALK_LAST_HOUR = 18
    }

    suspend fun getWeather(cityName: String = "Melbourne"): Result<WeatherResponse> {
        return try {
            val response = weatherService.getCurrentWeather(apiKey, cityName)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Context-aware computing logic: Generates break and stretch suggestions
     * based on live temperature, atmospheric conditions, UV rating, and time of day.
     * Aligns with ContextEngine daylight hours (08:00 - 18:00).
     */
    fun deriveErgonomicAdvice(
        weather: WeatherResponse,
        hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    ): WeatherAdvice {
        val temp = weather.current.tempC
        val tempRounded = temp.roundToInt()
        val conditionText = weather.current.condition.text
        val uv = weather.current.uv ?: 2.0

        val isRainingOrStormy = conditionText.contains("rain", ignoreCase = true) ||
                conditionText.contains("drizzle", ignoreCase = true) ||
                conditionText.contains("storm", ignoreCase = true) ||
                conditionText.contains("snow", ignoreCase = true)

        val isDaylightWalkingHours = hourOfDay in WALK_FIRST_HOUR..WALK_LAST_HOUR
        val isOutdoorSuitable = (temp in 16.0..28.0) && !isRainingOrStormy && isDaylightWalkingHours

        return if (isOutdoorSuitable) {
            WeatherAdvice(
                temperatureFormatted = "$tempRounded°",
                headline = "Good time for an outdoor break",
                subtitle = "$tempRounded °C, ${conditionText.lowercase()}, UV ${uv.roundToInt()} · Ideal for a 5-min walk",
                isOutdoorFriendly = true
            )
        } else {
            val reason = when {
                !isDaylightWalkingHours -> "Evening/Night hours"
                isRainingOrStormy -> "Wet outdoor conditions"
                else -> "Cool/Hot outdoor air"
            }
            WeatherAdvice(
                temperatureFormatted = "$tempRounded°",
                headline = if (!isDaylightWalkingHours) "Evening indoor desk stretch" else "Recommended indoor desk stretch",
                subtitle = "$tempRounded °C, ${conditionText.lowercase()} · $reason: Relax your neck & back",
                isOutdoorFriendly = false
            )
        }
    }

    /**
     * Maps cached WeatherSnapshot to UI presentation model.
     */
    fun adviceFromSnapshot(snapshot: WeatherSnapshot): WeatherAdvice {
        val tempRounded = snapshot.temperatureC.roundToInt()
        return if (snapshot.isOutdoorFriendly) {
            WeatherAdvice(
                temperatureFormatted = "$tempRounded°",
                headline = "Good time for an outdoor break",
                subtitle = "$tempRounded °C, ${snapshot.condition.lowercase()} · Ideal for a 5-min walk",
                isOutdoorFriendly = true
            )
        } else {
            WeatherAdvice(
                temperatureFormatted = "$tempRounded°",
                headline = "Recommended indoor desk stretch",
                subtitle = "$tempRounded °C, ${snapshot.condition.lowercase()} · Relax your neck & back",
                isOutdoorFriendly = false
            )
        }
    }

    /**
     * Fallback suggestion when API is unreachable or offline.
     * Displays "--" and "Weather unavailable" instead of misleading mock data.
     */
    fun getUnavailableAdvice(): WeatherAdvice {
        return WeatherAdvice(
            temperatureFormatted = "--",
            headline = "Weather unavailable",
            subtitle = "Offline or API unreachable · Recommend indoor desk stretch",
            isOutdoorFriendly = false
        )
    }

    /**
     * Backward-compatible alias for getUnavailableAdvice().
     */
    fun getFallbackAdvice(): WeatherAdvice = getUnavailableAdvice()
}
