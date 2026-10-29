
package com.alisha2112.vetcare.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponseDto(
    @SerialName("current")
    val current: CurrentWeatherDto,

    @SerialName("daily")
    val daily: DailyForecastDto
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

@Serializable
data class DailyForecastDto(
    @SerialName("time")
    val dates: List<String>,

    @SerialName("temperature_2m_max")
    val maxTemperatures: List<Double?>,

    @SerialName("temperature_2m_min")
    val minTemperatures: List<Double?>
)
