
package com.alisha2112.vetcare.data.model

enum class ObservationType(val title: String, val unit: String) {
    TEMPERATURE("Температура", "°C"),
    PULSE("Пульс", "уд/хв"),
    SLEEP("Сон", "год"),
    FEEDING("Годування", "г")
}

data class VetObservation(
    val id: Long,
    val petName: String,
    val type: ObservationType,
    val value: Double,
    val note: String,
    val recordedAt: String
)

/**
 * Навчальна класифікація тривалості сну.
 * Значення не є ветеринарними нормами.
 */
fun sleepQuality(hours: Double): String = when {
    !hours.isFinite() || hours < 0 -> "Некоректне значення"
    hours < 6 -> "Недостатньо"
    hours <= 9 -> "Норма (навчальний приклад)"
    else -> "Забагато (навчальний приклад)"
}

data class DaySummary(
    val totalFeedGrams: Double,
    val averagePulse: Double?,
    val latestSleepHours: Double?,
    val observationCount: Int
)

fun List<VetObservation>.summary(): DaySummary {
    val pulseValues = filter {
        it.type == ObservationType.PULSE
    }.map {
        it.value
    }

    return DaySummary(
        totalFeedGrams = filter {
            it.type == ObservationType.FEEDING
        }.sumOf {
            it.value
        },
        averagePulse = pulseValues
            .takeIf { it.isNotEmpty() }
            ?.average(),
        latestSleepHours = firstOrNull {
            it.type == ObservationType.SLEEP
        }?.value,
        observationCount = size
    )
}

fun List<VetObservation>.countByType(): Map<ObservationType, Int> =
    ObservationType.entries.associateWith { type ->
        count { it.type == type }
    }
