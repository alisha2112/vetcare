package com.alisha2112.vetcare.data.model

data class CurrentWeather(
    val temperatureCelsius: Double,
    val feelsLikeCelsius: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val weatherCode: Int,
    val observedAt: String
) {
    val description: String
        get() = when (weatherCode) {
            0 -> "Ясно"
            1 -> "Переважно ясно"
            2 -> "Мінлива хмарність"
            3 -> "Хмарно"
            45, 48 -> "Туман"
            51, 53, 55 -> "Мряка"
            56, 57 -> "Переохолоджена мряка"
            61, 63, 65 -> "Дощ"
            66, 67 -> "Крижаний дощ"
            71, 73, 75 -> "Сніг"
            77 -> "Снігові зерна"
            80, 81, 82 -> "Зливи"
            85, 86 -> "Снігові заряди"
            95 -> "Гроза"
            96, 99 -> "Гроза з градом"
            else -> "Погодні умови: код $weatherCode"
        }
}
