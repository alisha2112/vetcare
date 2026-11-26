package com.alisha2112.vetcare.location

import android.location.Location

/** Точка маршруту в градусах WGS84. */
data class RoutePoint(
    val latitude: Double,
    val longitude: Double
)

/** Відстань між двома точками в метрах. */
fun interface DistanceCalculator {
    fun distanceMeters(from: RoutePoint, to: RoutePoint): Float
}

/** Обчислення через Location.distanceBetween (Android). */
val AndroidDistanceCalculator = DistanceCalculator { from, to ->
    val results = FloatArray(1)
    Location.distanceBetween(
        from.latitude,
        from.longitude,
        to.latitude,
        to.longitude,
        results
    )
    results[0]
}

/**
 * Сумарна довжина ламаної лінії маршруту в метрах.
 * Порожній маршрут або одна точка дають 0.
 */
fun routeLengthMeters(
    points: List<RoutePoint>,
    calculator: DistanceCalculator = AndroidDistanceCalculator
): Double {
    var total = 0.0

    for (index in 1 until points.size) {
        val segment = calculator.distanceMeters(points[index - 1], points[index])
        if (segment.isFinite() && segment > 0f) {
            total += segment.toDouble()
        }
    }

    return total
}

fun List<Location>.toRoutePoints(): List<RoutePoint> =
    map { RoutePoint(it.latitude, it.longitude) }
