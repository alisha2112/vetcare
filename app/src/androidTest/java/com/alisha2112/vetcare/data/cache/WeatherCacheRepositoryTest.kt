package com.alisha2112.vetcare.data.cache

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.alisha2112.vetcare.data.model.CurrentWeather
import com.alisha2112.vetcare.data.model.ForecastDay
import com.alisha2112.vetcare.data.model.WeatherReport
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import com.alisha2112.vetcare.createTestDataStoreContext

@RunWith(AndroidJUnit4::class)
class WeatherCacheRepositoryTest {

    private val repository by lazy {
        WeatherCacheRepository(
            createTestDataStoreContext(),
            Json { ignoreUnknownKeys = true }
        )
    }


    @Test
    fun savedWeatherCanBeReadFromDataStore() = runBlocking {
        val report = WeatherReport(
            current = CurrentWeather(
                temperatureCelsius = 12.0,
                feelsLikeCelsius = 10.0,
                humidityPercent = 75,
                windSpeedKmh = 12.0,
                weatherCode = 2,
                observedAt = "2026-10-29T15:00"
            ),
            forecast = listOf(
                ForecastDay("2026-10-29", 14.0, 7.0),
                ForecastDay("2026-10-30", 11.0, 5.0),
                ForecastDay("2026-10-31", 13.0, 6.0)
            ),
            fetchedAtMillis = 1793286000000L
        )

        repository.saveWeather(report)

        val restored = repository.getCachedWeather()

        assertEquals(report, restored)
        assertEquals(3, restored?.forecast?.size)
    }
}
