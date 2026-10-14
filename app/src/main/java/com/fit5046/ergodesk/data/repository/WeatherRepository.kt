package com.fit5046.ergodesk.data.repository

import com.fit5046.ergodesk.data.network.WeatherRetrofitObject
import com.fit5046.ergodesk.data.network.model.WeatherAdvice
import com.fit5046.ergodesk.data.network.model.WeatherResponse
import kotlin.math.roundToInt

/**
 * Repository layer managing live weather data retrieval and contextual ergonomic advice.
 * Connects Retrofit networking with the UI presentation layer.
 * Member 4 responsibility.
 */
class WeatherRepository {

    private val weatherService = WeatherRetrofitObject.retrofitService

    // WeatherAPI key configured from FIT5046 unit resources
    private val apiKey = "54aec09c81644a1f99c12244261609"

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
     * based on live temperature, atmospheric conditions, and UV rating.
     */
    fun deriveErgonomicAdvice(weather: WeatherResponse): WeatherAdvice {
        val temp = weather.current.tempC
        val tempRounded = temp.roundToInt()
        val conditionText = weather.current.condition.text
        val uv = weather.current.uv ?: 2.0

        val isRainingOrStormy = conditionText.contains("rain", ignoreCase = true) ||
                conditionText.contains("drizzle", ignoreCase = true) ||
                conditionText.contains("storm", ignoreCase = true) ||
                conditionText.contains("snow", ignoreCase = true)

        val isOutdoorSuitable = (temp in 16.0..28.0) && !isRainingOrStormy

        return if (isOutdoorSuitable) {
            WeatherAdvice(
                temperatureFormatted = "$tempRounded°",
                headline = "Good time for an outdoor break",
                subtitle = "$tempRounded °C, ${conditionText.lowercase()}, UV ${uv.roundToInt()} · Ideal for a 5-min walk",
                isOutdoorFriendly = true
            )
        } else {
            val reason = if (isRainingOrStormy) "Wet outdoor conditions" else "Cool/Hot outdoor air"
            WeatherAdvice(
                temperatureFormatted = "$tempRounded°",
                headline = "Recommended indoor desk stretch",
                subtitle = "$tempRounded °C, ${conditionText.lowercase()} · $reason: Relax your neck & back",
                isOutdoorFriendly = false
            )
        }
    }

    /**
     * Offline fallback suggestion in case of no internet connectivity.
     */
    fun getFallbackAdvice(): WeatherAdvice {
        return WeatherAdvice(
            temperatureFormatted = "21°",
            headline = "Good time for an outdoor break",
            subtitle = "21 °C, pleasant · Ideal time to stand and stretch",
            isOutdoorFriendly = true
        )
    }
}
