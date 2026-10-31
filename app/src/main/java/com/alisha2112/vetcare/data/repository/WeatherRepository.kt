package com.alisha2112.vetcare.data.repository

import com.alisha2112.vetcare.data.model.CurrentWeather
import com.alisha2112.vetcare.data.model.ForecastDay
import com.alisha2112.vetcare.data.model.WeatherReport
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

    suspend fun getWeatherReport(): WeatherReport {
        val response = api.getWeatherForecast(
            latitude = KYIV_LATITUDE,
            longitude = KYIV_LONGITUDE
        )

        val current = response.current
        val daily = response.daily

        if (
            daily.dates.size < 3 ||
            daily.maxTemperatures.size < 3 ||
            daily.minTemperatures.size < 3
        ) {
            throw IllegalStateException(
                "API повернув неповний прогноз"
            )
        }

        val forecast = (0 until 3).map { index ->
            val maxTemperature =
                daily.maxTemperatures[index]
                    ?: throw IllegalStateException(
                        "Відсутня максимальна температура"
                    )

            val minTemperature =
                daily.minTemperatures[index]
                    ?: throw IllegalStateException(
                        "Відсутня мінімальна температура"
                    )

            ForecastDay(
                date = daily.dates[index],
                maxTemperatureCelsius = maxTemperature,
                minTemperatureCelsius = minTemperature
            )
        }

        return WeatherReport(
            current = CurrentWeather(
                temperatureCelsius = current.temperature,
                feelsLikeCelsius = current.apparentTemperature,
                humidityPercent = current.relativeHumidity,
                windSpeedKmh = current.windSpeed,
                weatherCode = current.weatherCode,
                observedAt = current.time
            ),
            forecast = forecast,
            fetchedAtMillis = System.currentTimeMillis()
        )
    }
}
