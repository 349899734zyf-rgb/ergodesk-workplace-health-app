package com.fit5046.ergodesk.data.network.model

import com.google.gson.annotations.SerializedName

/**
 * Weather API JSON response model.
 * Public Web API implementation - Member 4 responsibility.
 */
data class WeatherResponse(
    @SerializedName("location")
    val location: WeatherLocation? = null,
    @SerializedName("current")
    val current: CurrentWeather
)

data class CurrentWeather(
    @SerializedName("temp_c")
    val tempC: Double,
    @SerializedName("condition")
    val condition: WeatherCondition,
    @SerializedName("uv")
    val uv: Double? = 0.0,
    @SerializedName("humidity")
    val humidity: Int? = 0,
    @SerializedName("feelslike_c")
    val feelsLikeC: Double? = null
)

data class WeatherCondition(
    @SerializedName("text")
    val text: String,
    @SerializedName("icon")
    val icon: String? = null
)

data class WeatherLocation(
    @SerializedName("name")
    val name: String,
    @SerializedName("country")
    val country: String? = null
)

/**
 * Context-aware suggestion generated from current weather conditions.
 */
data class WeatherAdvice(
    val temperatureFormatted: String,
    val headline: String,
    val subtitle: String,
    val isOutdoorFriendly: Boolean
)
