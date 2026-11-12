package com.alisha2112.vetcare.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ForecastDay(
    val date: String,
    val maxTemperatureCelsius: Double,
    val minTemperatureCelsius: Double
)

@Serializable
data class WeatherReport(
    val current: CurrentWeather,
    val forecast: List<ForecastDay>,
    val fetchedAtMillis: Long
)
