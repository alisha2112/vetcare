package com.alisha2112.vetcare.data.cache

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alisha2112.vetcare.data.model.WeatherReport
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.weatherCacheDataStore by preferencesDataStore(
    name = "vetcare_weather_cache"
)

@Singleton
class WeatherCacheRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {

    private val weatherKey =
        stringPreferencesKey("last_successful_weather")

    suspend fun getCachedWeather(): WeatherReport? {
        val preferences = context.weatherCacheDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .first()

        val savedJson =
            preferences[weatherKey] ?: return null

        return try {
            json.decodeFromString<WeatherReport>(savedJson)
        } catch (exception: SerializationException) {
            null
        }
    }

    suspend fun saveWeather(report: WeatherReport) {
        val serialized = json.encodeToString(report)

        context.weatherCacheDataStore.edit { preferences ->
            preferences[weatherKey] = serialized
        }
    }
}
