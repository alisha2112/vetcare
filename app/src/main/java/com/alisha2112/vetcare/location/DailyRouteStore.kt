package com.alisha2112.vetcare.location

import android.content.Context
import android.location.Location
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

private val Context.dailyRouteDataStore by preferencesDataStore(
    name = "daily_route"
)

data class DailyRoute(
    val date: String,
    val points: List<Location>,
    val currentLocation: Location
)

class DailyRouteStore(context: Context) {

    private val dataStore =
        context.applicationContext.dailyRouteDataStore

    private val routeKey =
        stringPreferencesKey("route_json")

    val route: Flow<Result<DailyRoute?>> =
        dataStore.data.map { preferences ->
            runCatching {
                decode(preferences[routeKey])
            }
        }.catch { exception ->
            emit(Result.failure(exception))
        }

    suspend fun recordPoint(location: Location) {
        val today = LocalDate.now().toString()
        val newPoint = Location(location)

        dataStore.edit { preferences ->
            val previousRoute =
                decode(preferences[routeKey])
                    ?.takeIf { it.date == today }

            val points =
                previousRoute?.points.orEmpty()

            val previousPoint = points.lastOrNull()

            val shouldAddPoint = if (previousPoint == null) {
                true
            } else {
                val results = FloatArray(1)

                Location.distanceBetween(
                    previousPoint.latitude,
                    previousPoint.longitude,
                    newPoint.latitude,
                    newPoint.longitude,
                    results
                )

                val minimumDistance = maxOf(
                    5f,
                    previousPoint.accuracy,
                    newPoint.accuracy
                )

                results[0] >= minimumDistance
            }

            val updatedPoints = if (shouldAddPoint) {
                points + newPoint
            } else {
                points
            }

            val updatedRoute = DailyRoute(
                date = today,
                points = updatedPoints,
                currentLocation = newPoint
            )

            preferences[routeKey] = encode(updatedRoute)
        }
    }

    suspend fun clearRoute() {
        dataStore.edit { preferences ->
            preferences.remove(routeKey)
        }
    }

    private fun encodeLocation(
        location: Location
    ): JSONObject {
        return JSONObject().apply {
            put("latitude", location.latitude)
            put("longitude", location.longitude)
            put("accuracy", location.accuracy.toDouble())
            put("time", location.time)
        }
    }

    private fun decodeLocation(
        json: JSONObject
    ): Location {
        return Location("stored_route").apply {
            latitude = json.getDouble("latitude")
            longitude = json.getDouble("longitude")
            accuracy = json.getDouble("accuracy").toFloat()
            time = json.getLong("time")
        }
    }

    private fun encode(route: DailyRoute): String {
        val pointsArray = JSONArray()

        route.points.forEach { point ->
            pointsArray.put(encodeLocation(point))
        }

        return JSONObject().apply {
            put("date", route.date)
            put("points", pointsArray)
            put(
                "currentLocation",
                encodeLocation(route.currentLocation)
            )
        }.toString()
    }

    private fun decode(raw: String?): DailyRoute? {
        if (raw == null) return null

        val json = JSONObject(raw)
        val pointsArray = json.getJSONArray("points")

        val points = buildList {
            for (index in 0 until pointsArray.length()) {
                add(
                    decodeLocation(
                        pointsArray.getJSONObject(index)
                    )
                )
            }
        }

        return DailyRoute(
            date = json.getString("date"),
            points = points,
            currentLocation = decodeLocation(
                json.getJSONObject("currentLocation")
            )
        )
    }
}
