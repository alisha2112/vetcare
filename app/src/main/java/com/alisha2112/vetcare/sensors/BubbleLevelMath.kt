package com.alisha2112.vetcare.sensors

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/** Один показник акселерометра (м/с²) у системі координат пристрою. */
data class AccelerationSample(
    val x: Float,
    val y: Float,
    val z: Float
) {
    val magnitude: Float
        get() = sqrt(x * x + y * y + z * z)
}

/**
 * Результат обробки показника для бульбашкового рівня.
 *
 * [bubbleX] і [bubbleY] — нормалізоване положення бульбашки
 * в екранних координатах (-1..1, вісь Y спрямована вниз),
 * довжина вектора не перевищує 1.
 */
data class LevelReading(
    val tiltXDegrees: Float,
    val tiltYDegrees: Float,
    val bubbleX: Float,
    val bubbleY: Float,
    val isLevel: Boolean
)

/** Простий low-pass filter: f = f + alpha * (x - f). */
class LowPassFilter(private val alpha: Float = DEFAULT_ALPHA) {

    init {
        require(alpha > 0f && alpha <= 1f) { "alpha має бути в (0; 1]" }
    }

    var current: AccelerationSample? = null
        private set

    /** Некоректні показники ігноруються, повертається попереднє значення. */
    fun filter(sample: AccelerationSample): AccelerationSample? {
        if (!BubbleLevelMath.isValid(sample)) return current

        val previous = current
        val next = if (previous == null) {
            sample
        } else {
            AccelerationSample(
                x = previous.x + alpha * (sample.x - previous.x),
                y = previous.y + alpha * (sample.y - previous.y),
                z = previous.z + alpha * (sample.z - previous.z)
            )
        }

        current = next
        return next
    }

    fun reset() {
        current = null
    }

    companion object {
        const val DEFAULT_ALPHA = 0.15f
    }
}

object BubbleLevelMath {

    /** Кут, що відповідає краю індикатора. */
    const val MAX_DISPLAY_ANGLE = 30f

    /** Допуск для «приблизно горизонтально». */
    const val LEVEL_TOLERANCE_DEGREES = 1.5f

    private const val MIN_MAGNITUDE = 1f
    private const val MAX_MAGNITUDE = 40f

    private const val RAD_TO_DEG = (180.0 / Math.PI).toFloat()

    /**
     * Показник коректний, якщо всі осі скінченні, а модуль
     * вектора близький до фізично можливого (не вільне падіння
     * та не різкий удар).
     */
    fun isValid(sample: AccelerationSample): Boolean {
        if (!sample.x.isFinite() || !sample.y.isFinite() || !sample.z.isFinite()) {
            return false
        }

        val magnitude = sample.magnitude
        return magnitude in MIN_MAGNITUDE..MAX_MAGNITUDE
    }

    /**
     * Переводить осі пристрою в осі поточної орієнтації екрана
     * (аналогічно до SensorManager.remapCoordinateSystem).
     * [rotationDegrees] — 0, 90, 180 або 270.
     */
    fun remapToScreen(
        sample: AccelerationSample,
        rotationDegrees: Int
    ): AccelerationSample = when (((rotationDegrees % 360) + 360) % 360) {
        90 -> AccelerationSample(-sample.y, sample.x, sample.z)
        180 -> AccelerationSample(-sample.x, -sample.y, sample.z)
        270 -> AccelerationSample(sample.y, -sample.x, sample.z)
        else -> sample
    }

    /**
     * Кути нахилу відносно горизонтальної площини.
     * Нахил X — поворот навколо вертикальної осі екрана
     * (лівий/правий край), нахил Y — верхній/нижній край.
     */
    fun tiltAngles(sample: AccelerationSample): Pair<Float, Float> {
        val tiltX = atan2(sample.x, sqrt(sample.y * sample.y + sample.z * sample.z)) * RAD_TO_DEG
        val tiltY = atan2(sample.y, sqrt(sample.x * sample.x + sample.z * sample.z)) * RAD_TO_DEG
        return tiltX to tiltY
    }

    fun reading(
        sample: AccelerationSample,
        rotationDegrees: Int = 0
    ): LevelReading? {
        if (!isValid(sample)) return null

        val screen = remapToScreen(sample, rotationDegrees)
        val (tiltX, tiltY) = tiltAngles(screen)

        // Бульбашка рухається до піднятого краю:
        // піднятий лівий край -> x < 0 -> бульбашка ліворуч;
        // піднятий верхній край -> y > 0 -> бульбашка вгору
        // (в екранних координатах Y спрямована вниз).
        var bubbleX = tiltX / MAX_DISPLAY_ANGLE
        var bubbleY = -tiltY / MAX_DISPLAY_ANGLE

        val length = sqrt(bubbleX * bubbleX + bubbleY * bubbleY)
        if (length > 1f) {
            bubbleX /= length
            bubbleY /= length
        }

        return LevelReading(
            tiltXDegrees = tiltX,
            tiltYDegrees = tiltY,
            bubbleX = bubbleX,
            bubbleY = bubbleY,
            isLevel = screen.z > 0f &&
                    abs(tiltX) <= LEVEL_TOLERANCE_DEGREES &&
                    abs(tiltY) <= LEVEL_TOLERANCE_DEGREES
        )
    }
}
