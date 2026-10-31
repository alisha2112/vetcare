package com.alisha2112.vetcare.data.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherReportSerializationTest {

    @Test
    fun reportSurvivesJsonSerializationRoundTrip() {
        val original = WeatherReport(
            current = CurrentWeather(
                temperatureCelsius = 12.5,
                feelsLikeCelsius = 10.0,
                humidityPercent = 75,
                windSpeedKmh = 14.0,
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

        val json = Json.encodeToString(original)
        val restored =
            Json.decodeFromString<WeatherReport>(json)

        assertEquals(original, restored)
        assertEquals(3, restored.forecast.size)
        assertEquals(
            "Мінлива хмарність",
            restored.current.description
        )
    }
}
