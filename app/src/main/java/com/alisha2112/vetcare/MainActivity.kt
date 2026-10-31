package com.alisha2112.vetcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alisha2112.vetcare.data.settings.ThemeMode
import com.alisha2112.vetcare.ui.VetCareApp
import com.alisha2112.vetcare.ui.settings.SettingsViewModel
import com.alisha2112.vetcare.ui.theme.VetCareTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val userSettings by settingsViewModel.settings
                .collectAsStateWithLifecycle()

            val darkTheme = when (userSettings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            VetCareTheme(darkTheme = darkTheme) {
                VetCareApp(
                    userSettings = userSettings,
                    onSaveUserName = settingsViewModel::saveUserName,
                    onThemeModeChange = settingsViewModel::setThemeMode
                )
            }
        }
    }
}
