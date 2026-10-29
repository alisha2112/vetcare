package com.alisha2112.vetcare.data.repository

import com.alisha2112.vetcare.data.model.CurrentWeather
import com.alisha2112.vetcare.data.remote.WeatherApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val api: WeatherApi
) {

    companion object {
        private const val KYIV_LATITUDE = 50.4501
        private const val KYIV_LONGITUDE = 30.5234
    }

    suspend fun getCurrentWeather(): CurrentWeather {
        val response = api.getCurrentWeather(
            latitude = KYIV_LATITUDE,
            longitude = KYIV_LONGITUDE
        )

        val current = response.current

        return CurrentWeather(
            temperatureCelsius = current.temperature,
            feelsLikeCelsius = current.apparentTemperature,
            humidityPercent = current.relativeHumidity,
            windSpeedKmh = current.windSpeed,
            weatherCode = current.weatherCode,
            observedAt = current.time
        )
    }
}
