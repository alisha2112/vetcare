package com.alisha2112.vetcare.data.settings

enum class ThemeMode(val title: String) {
    SYSTEM("Як у системі"),
    LIGHT("Світла"),
    DARK("Темна")
}

data class UserSettings(
    val userName: String = "Користувач",
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
