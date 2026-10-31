package com.alisha2112.vetcare.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

    @GET("v1/forecast")
    suspend fun getWeatherForecast(
        @Query("latitude")
        latitude: Double,

        @Query("longitude")
        longitude: Double,

        @Query("current")
        currentVariables: String =
            "temperature_2m,relative_humidity_2m," +
                    "apparent_temperature,wind_speed_10m," +
                    "weather_code",

        @Query("daily")
        dailyVariables: String =
            "temperature_2m_max,temperature_2m_min",

        @Query("forecast_days")
        forecastDays: Int = 3,

        @Query("timezone")
        timezone: String = "Europe/Kyiv"
    ): WeatherResponseDto
}
