package com.alisha2112.vetcare.ui.weather

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.alisha2112.vetcare.data.cache.WeatherCacheRepository
import com.alisha2112.vetcare.data.model.CurrentWeather
import com.alisha2112.vetcare.data.model.ForecastDay
import com.alisha2112.vetcare.data.model.WeatherReport
import com.alisha2112.vetcare.data.remote.CurrentWeatherDto
import com.alisha2112.vetcare.data.remote.DailyForecastDto
import com.alisha2112.vetcare.data.remote.WeatherApi
import com.alisha2112.vetcare.data.remote.WeatherResponseDto
import com.alisha2112.vetcare.data.repository.WeatherRepository
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import com.alisha2112.vetcare.createTestDataStoreContext

@RunWith(AndroidJUnit4::class)
class WeatherViewModelTest {

    private val cache by lazy {
        WeatherCacheRepository(
            createTestDataStoreContext(),
            Json { ignoreUnknownKeys = true }
        )
    }


    private fun cachedReport(): WeatherReport =
        WeatherReport(
            current = CurrentWeather(
                temperatureCelsius = 10.0,
                feelsLikeCelsius = 8.0,
                humidityPercent = 70,
                windSpeedKmh = 10.0,
                weatherCode = 3,
                observedAt = "2026-10-28T12:00"
            ),
            forecast = listOf(
                ForecastDay("2026-10-28", 12.0, 6.0),
                ForecastDay("2026-10-29", 11.0, 5.0),
                ForecastDay("2026-10-30", 13.0, 7.0)
            ),
            fetchedAtMillis = 1793188800000L
        )

    private fun fakeApi(
        shouldFail: Boolean
    ): WeatherApi = object : WeatherApi {

        override suspend fun getWeatherForecast(
            latitude: Double,
            longitude: Double,
            currentVariables: String,
            dailyVariables: String,
            forecastDays: Int,
            timezone: String
        ): WeatherResponseDto {

            if (shouldFail) {
                throw IOException("Немає інтернету")
            }

            return WeatherResponseDto(
                current = CurrentWeatherDto(
                    time = "2026-10-29T15:00",
                    temperature = 12.0,
                    relativeHumidity = 75,
                    apparentTemperature = 10.0,
                    windSpeed = 12.0,
                    weatherCode = 2
                ),
                daily = DailyForecastDto(
                    dates = listOf(
                        "2026-10-29",
                        "2026-10-30",
                        "2026-10-31"
                    ),
                    maxTemperatures = listOf(
                        14.0, 11.0, 13.0
                    ),
                    minTemperatures = listOf(
                        7.0, 5.0, 6.0
                    )
                )
            )
        }
    }

    @Test
    fun offlineModeShowsStaleCachedWeather() = runBlocking {
        val previousReport = cachedReport()

        cache.saveWeather(previousReport)

        val viewModel = WeatherViewModel(
            repository = WeatherRepository(
                fakeApi(shouldFail = true)
            ),
            cacheRepository = cache
        )

        val state = withTimeout(10_000) {
            viewModel.uiState.first {
                it is WeatherUiState.Success &&
                        it.isStale &&
                        !it.isRefreshing
            }
        } as WeatherUiState.Success

        assertEquals(previousReport, state.report)
        assertTrue(state.isStale)
        assertFalse(state.isRefreshing)
        assertNotNull(state.warning)
        assertTrue(
            state.warning!!.contains("інтернету")
        )
    }

    @Test
    fun successfulRequestUpdatesDataAndCache() = runBlocking {
        cache.saveWeather(cachedReport())

        val viewModel = WeatherViewModel(
            repository = WeatherRepository(
                fakeApi(shouldFail = false)
            ),
            cacheRepository = cache
        )

        val state = withTimeout(10_000) {
            viewModel.uiState.first {
                it is WeatherUiState.Success &&
                        !it.isStale &&
                        !it.isRefreshing
            }
        } as WeatherUiState.Success

        assertFalse(state.isStale)
        assertEquals(
            12.0,
            state.report.current.temperatureCelsius,
            0.001
        )
        assertEquals(3, state.report.forecast.size)

        val storedReport = cache.getCachedWeather()

        assertEquals(state.report, storedReport)
    }
}
