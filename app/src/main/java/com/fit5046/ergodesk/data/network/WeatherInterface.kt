package com.fit5046.ergodesk.data.network

import com.fit5046.ergodesk.data.network.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit Interface defining endpoints for WeatherAPI.
 * Public Web API implementation - Member 4 responsibility.
 */
interface WeatherInterface {

    /**
     * Retrieves current weather and atmospheric conditions for the specified city.
     * @param apiKey Registered API key on WeatherAPI.com
     * @param cityName Location name (e.g., "Melbourne", "Clayton")
     */
    @GET("current.json")
    suspend fun getCurrentWeather(
        @Query("key") apiKey: String,
        @Query("q") cityName: String
    ): WeatherResponse
}
