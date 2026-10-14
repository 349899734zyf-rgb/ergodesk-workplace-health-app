package com.fit5046.ergodesk.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton Retrofit object configuring the HTTP client and Gson JSON converter.
 * Follows Monash FIT5046 Week 8 architectural standards.
 * Member 4 responsibility.
 */
object WeatherRetrofitObject {

    private const val BASE_URL = "https://api.weatherapi.com/v1/"

    val retrofitService: WeatherInterface by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherInterface::class.java)
    }
}
