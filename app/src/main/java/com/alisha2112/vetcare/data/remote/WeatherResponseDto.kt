package com.alisha2112.vetcare.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponseDto(
    @SerialName("current")
    val current: CurrentWeatherDto
)

@Serializable
data class CurrentWeatherDto(
    @SerialName("time")
    val time: String,

    @SerialName("temperature_2m")
    val temperature: Double,

    @SerialName("relative_humidity_2m")
    val relativeHumidity: Int,

    @SerialName("apparent_temperature")
    val apparentTemperature: Double,

    @SerialName("wind_speed_10m")
    val windSpeed: Double,

    @SerialName("weather_code")
    val weatherCode: Int
)
