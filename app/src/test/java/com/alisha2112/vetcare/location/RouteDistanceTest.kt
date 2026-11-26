package com.alisha2112.vetcare.location

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class RouteDistanceTest {

    /** Формула гаверсинусів замість Android-заглушки Location. */
    private val haversine = DistanceCalculator { a, b ->
        val r = 6_371_000.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) *
                sin(dLon / 2).pow(2)
        (2 * r * asin(sqrt(h))).toFloat()
    }

    private val kyiv = RoutePoint(50.4501, 30.5234)

    @Test
    fun `empty route and single point have zero length`() {
        assertEquals(0.0, routeLengthMeters(emptyList(), haversine), 0.0)
        assertEquals(0.0, routeLengthMeters(listOf(kyiv), haversine), 0.0)
    }

    @Test
    fun `identical points add no distance`() {
        assertEquals(0.0, routeLengthMeters(listOf(kyiv, kyiv, kyiv), haversine), 0.0)
    }

    @Test
    fun `length is sum of consecutive segments in creation order`() {
        val calls = mutableListOf<Pair<RoutePoint, RoutePoint>>()
        val fixed = DistanceCalculator { a, b -> calls += a to b; 100f }
        val points = listOf(RoutePoint(1.0, 1.0), RoutePoint(2.0, 2.0), RoutePoint(3.0, 3.0))

        assertEquals(200.0, routeLengthMeters(points, fixed), 0.0)
        assertEquals(
            listOf(points[0] to points[1], points[1] to points[2]),
            calls
        )
    }

    @Test
    fun `real coordinates give expected distance`() {
        // 0.01° широти ≈ 1112 м
        val north = RoutePoint(kyiv.latitude + 0.01, kyiv.longitude)
        val length = routeLengthMeters(listOf(kyiv, north, kyiv), haversine)

        assertEquals(2 * 1112.0, length, 5.0)
    }

    @Test
    fun `invalid segment values are ignored`() {
        val values = ArrayDeque(listOf(Float.NaN, -5f, 50f))
        val calculator = DistanceCalculator { _, _ -> values.removeFirst() }
        val points = List(4) { RoutePoint(it.toDouble(), 0.0) }

        assertEquals(50.0, routeLengthMeters(points, calculator), 0.0)
    }
}
