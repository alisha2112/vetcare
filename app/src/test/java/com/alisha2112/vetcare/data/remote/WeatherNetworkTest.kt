package com.alisha2112.vetcare.data.remote

import com.alisha2112.vetcare.data.repository.WeatherRepository
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class WeatherNetworkTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun createApi(): WeatherApi {
        val json = Json {
            ignoreUnknownKeys = true
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(
                json.asConverterFactory(
                    "application/json".toMediaType()
                )
            )
            .build()

        return retrofit.create(WeatherApi::class.java)
    }

    @Test
    fun currentAndDailyAreReceivedInOneRequest() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(VALID_WEATHER_JSON)
        )

        val repository = WeatherRepository(createApi())
        val report = repository.getWeatherReport()

        val request = server.takeRequest(
            5,
            TimeUnit.SECONDS
        ) ?: error("Не отримано HTTP-запит")

        val url = requireNotNull(request.requestUrl)

        assertEquals("GET", request.method)
        assertEquals("/v1/forecast", url.encodedPath)

        assertEquals(
            "50.4501",
            url.queryParameter("latitude")
        )
        assertEquals(
            "30.5234",
            url.queryParameter("longitude")
        )
        assertEquals(
            "temperature_2m_max,temperature_2m_min",
            url.queryParameter("daily")
        )
        assertEquals(
            "3",
            url.queryParameter("forecast_days")
        )
        assertEquals(
            "Europe/Kyiv",
            url.queryParameter("timezone")
        )

        val currentParameters =
            requireNotNull(url.queryParameter("current"))

        assertTrue(
            currentParameters.contains("temperature_2m")
        )
        assertTrue(
            currentParameters.contains("weather_code")
        )

        assertEquals(1, server.requestCount)

        assertEquals(
            12.0,
            report.current.temperatureCelsius,
            0.001
        )

        assertEquals(
            3,
            report.forecast.size
        )

        assertEquals(
            "2026-10-29",
            report.forecast.first().date
        )

        assertEquals(
            14.0,
            report.forecast.first().maxTemperatureCelsius,
            0.001
        )

        assertEquals(
            7.0,
            report.forecast.first().minTemperatureCelsius,
            0.001
        )

        assertTrue(report.fetchedAtMillis > 0)
    }

    @Test
    fun incompleteDailyForecastIsRejected() = runTest {
        val invalidJson = VALID_WEATHER_JSON.replace(
            "\"temperature_2m_min\": [7.0, 5.0, 6.0]",
            "\"temperature_2m_min\": [7.0]"
        )

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(invalidJson)
        )

        val repository = WeatherRepository(createApi())

        try {
            repository.getWeatherReport()
            fail("Очікувалася помилка неповного прогнозу")
        } catch (exception: IllegalStateException) {
            assertEquals(
                "API повернув неповний прогноз",
                exception.message
            )
        }

        assertEquals(1, server.requestCount)
    }

    @Test
    fun serverErrorIsNotTreatedAsSuccess() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(503)
        )

        val repository = WeatherRepository(createApi())

        try {
            repository.getWeatherReport()
            fail("Очікувалася HTTP-помилка")
        } catch (exception: HttpException) {
            assertEquals(503, exception.code())
        }

        assertEquals(1, server.requestCount)
    }

    companion object {
        private val VALID_WEATHER_JSON = """
            {
              "latitude": 50.45,
              "longitude": 30.52,
              "current": {
                "time": "2026-10-29T15:00",
                "temperature_2m": 12.0,
                "relative_humidity_2m": 75,
                "apparent_temperature": 10.0,
                "wind_speed_10m": 12.0,
                "weather_code": 2
              },
              "daily": {
                "time": [
                  "2026-10-29",
                  "2026-10-30",
                  "2026-10-31"
                ],
                "temperature_2m_max": [14.0, 11.0, 13.0],
                "temperature_2m_min": [7.0, 5.0, 6.0]
              }
            }
        """.trimIndent()
    }
}
