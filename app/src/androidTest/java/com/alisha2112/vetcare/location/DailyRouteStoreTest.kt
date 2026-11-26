package com.alisha2112.vetcare.location

import android.location.Location
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alisha2112.vetcare.createTestDataStoreContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * DataStore маршруту в окремій тестовій директорії
 * (реальний маршрут застосунку не змінюється).
 */
@RunWith(AndroidJUnit4::class)
class DailyRouteStoreTest {

    private val context = createTestDataStoreContext()
    private val store = DailyRouteStore(context)

    private fun location(lat: Double, lon: Double, accuracy: Float = 3f) =
        Location("test").apply {
            latitude = lat
            longitude = lon
            this.accuracy = accuracy
            time = System.currentTimeMillis()
        }

    @Before
    fun setUp() = runBlocking { store.clearRoute() }

    @After
    fun tearDown() = runBlocking { store.clearRoute() }

    @Test
    fun emptyStoreHasNoRoute() = runBlocking {
        assertNull(store.route.first().getOrThrow())
    }

    @Test
    fun firstPointStartsTodayRoute() = runBlocking {
        store.recordPoint(location(50.4501, 30.5234))

        val route = store.route.first().getOrThrow()!!
        assertEquals(LocalDate.now().toString(), route.date)
        assertEquals(1, route.points.size)
        assertEquals(50.4501, route.points.single().latitude, 1e-9)
    }

    @Test
    fun smallGpsJitterIsFilteredButCurrentLocationUpdates() = runBlocking {
        store.recordPoint(location(50.4501, 30.5234))
        // ~1 м — менше мінімального порогу 5 м
        store.recordPoint(location(50.45011, 30.5234))
        // повтор тієї самої точки
        store.recordPoint(location(50.4501, 30.5234))

        val route = store.route.first().getOrThrow()!!
        assertEquals(1, route.points.size)
        assertEquals(50.4501, route.currentLocation.latitude, 1e-9)
    }

    @Test
    fun lowAccuracyRaisesFilterThreshold() = runBlocking {
        store.recordPoint(location(50.4501, 30.5234, accuracy = 50f))
        // ~22 м, але точність 50 м — точку не додаємо
        store.recordPoint(location(50.4503, 30.5234, accuracy = 50f))

        assertEquals(1, store.route.first().getOrThrow()!!.points.size)
    }

    @Test
    fun distantPointsAreAccumulatedAndLengthComputed() = runBlocking {
        store.recordPoint(location(50.4501, 30.5234))
        store.recordPoint(location(50.4511, 30.5234)) // ~111 м
        store.recordPoint(location(50.4521, 30.5234)) // ще ~111 м

        val route = store.route.first().getOrThrow()!!
        assertEquals(3, route.points.size)

        val length = routeLengthMeters(route.points.toRoutePoints())
        assertEquals(222.6, length, 2.0)
    }

    @Test
    fun routeIsPersistedForNewStoreInstanceAndCanBeCleared() = runBlocking {
        store.recordPoint(location(50.4501, 30.5234))
        store.recordPoint(location(50.4601, 30.5234))

        val reopened = DailyRouteStore(context)
        assertEquals(2, reopened.route.first().getOrThrow()!!.points.size)

        reopened.clearRoute()
        assertNull(store.route.first().getOrThrow())
    }
}
