package com.alisha2112.vetcare.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = VetCareDarkPrimary,
    secondary = VetCareDarkSecondary,
    tertiary = VetCareDarkTertiary
)

private val LightColorScheme = lightColorScheme(
    primary = VetCareLightPrimary,
    secondary = VetCareLightSecondary,
    tertiary = VetCareLightTertiary
)

@Composable
fun VetCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) {
            DarkColorScheme
        } else {
            LightColorScheme
        },
        typography = Typography,
        content = content
    )
}
