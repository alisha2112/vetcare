package com.alisha2112.vetcare.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude")
        latitude: Double,

        @Query("longitude")
        longitude: Double,

        @Query("current")
        currentVariables: String =
            "temperature_2m,relative_humidity_2m," +
                    "apparent_temperature,wind_speed_10m," +
                    "weather_code",

        @Query("timezone")
        timezone: String = "auto"
    ): WeatherResponseDto
}
