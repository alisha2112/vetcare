package com.alisha2112.vetcare.sensors

enum class StepSessionStatus {
    /** Сеанс не розпочато або лічильник скинуто. */
    IDLE,

    /** Сеанс триває, але базове значення ще не отримано. */
    WAITING_FOR_SENSOR,

    /** Отримано базове значення, кроки рахуються. */
    ACTIVE,

    /** Вимірювання зупинено, результат зафіксовано. */
    STOPPED
}

/**
 * Незмінна модель сеансу крокоміра.
 *
 * TYPE_STEP_COUNTER повертає накопичувальну кількість кроків
 * з моменту перезавантаження, тому кроки сеансу обчислюються
 * як різниця між поточним і базовим показником.
 *
 * Сеанс може складатися з кількох сегментів: при паузі
 * (вихід з екрана, фоновий режим) поточний сегмент
 * додається до [accumulatedSteps], а після відновлення
 * перше значення стає новою базою. Отже кроки, зроблені
 * без активної підписки, не зараховуються.
 */
data class StepSession(
    val status: StepSessionStatus = StepSessionStatus.IDLE,
    val sessionId: Long = 0L,
    val accumulatedSteps: Long = 0L,
    val baseline: Float? = null,
    val lastValue: Float? = null
) {
    val isMeasuring: Boolean
        get() = status == StepSessionStatus.WAITING_FOR_SENSOR ||
                status == StepSessionStatus.ACTIVE

    private val segmentSteps: Long
        get() {
            val base = baseline ?: return 0L
            val last = lastValue ?: return 0L
            return (last - base).toLong().coerceAtLeast(0L)
        }

    /** Кількість кроків за сеанс; ніколи не від'ємна. */
    val steps: Long
        get() = (accumulatedSteps + segmentSteps).coerceAtLeast(0L)

    /** Новий сеанс: попередній результат не переноситься. */
    fun start(newSessionId: Long): StepSession = StepSession(
        status = StepSessionStatus.WAITING_FOR_SENSOR,
        sessionId = newSessionId
    )

    fun onSensorValue(raw: Float): StepSession {
        if (!isMeasuring || !isValidCounterValue(raw)) return this

        val base = baseline
        val last = lastValue

        if (base == null || last == null) {
            // Перший коректний показник — базова точка.
            return copy(
                status = StepSessionStatus.ACTIVE,
                baseline = raw,
                lastValue = raw
            )
        }

        if (raw < last) {
            // Апаратний лічильник зменшився (скидання).
            // Фіксуємо підраховані кроки й починаємо новий
            // сегмент від нового значення.
            return copy(
                accumulatedSteps = steps,
                baseline = raw,
                lastValue = raw
            )
        }

        // Повторний або більший показник.
        return copy(lastValue = raw)
    }

    /** Пауза підписки: зберегти кроки, наступне значення — нова база. */
    fun suspendSegment(): StepSession {
        if (!isMeasuring) return this

        return copy(
            status = StepSessionStatus.WAITING_FOR_SENSOR,
            accumulatedSteps = steps,
            baseline = null,
            lastValue = null
        )
    }

    fun stop(): StepSession {
        if (!isMeasuring) return this

        return copy(
            status = StepSessionStatus.STOPPED,
            accumulatedSteps = steps,
            baseline = null,
            lastValue = null
        )
    }

    fun reset(): StepSession = StepSession()

    companion object {
        fun isValidCounterValue(raw: Float): Boolean =
            raw.isFinite() && raw >= 0f
    }
}
