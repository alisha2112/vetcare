package com.alisha2112.vetcare.data.settings

import android.content.Context
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(
    name = "vetcare_settings"
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val settings: Flow<UserSettings> =
        context.settingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val storedTheme = preferences[Keys.THEME_MODE]

                val themeMode = ThemeMode.entries
                    .firstOrNull { it.name == storedTheme }
                    ?: ThemeMode.SYSTEM

                UserSettings(
                    userName = preferences[Keys.USER_NAME]
                        ?.takeIf { it.isNotBlank() }
                        ?: "Користувач",
                    themeMode = themeMode
                )
            }

    suspend fun setUserName(name: String) {
        val normalizedName = name.trim()

        if (normalizedName.isBlank()) return

        context.settingsDataStore.edit { preferences ->
            preferences[Keys.USER_NAME] = normalizedName
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = mode.name
        }
    }
}
