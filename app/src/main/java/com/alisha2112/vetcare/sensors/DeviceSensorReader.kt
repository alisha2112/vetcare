package com.alisha2112.vetcare.sensors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject
import javax.inject.Singleton

/** Помилки отримання даних від апаратних сенсорів. */
sealed class SensorException(message: String) : Exception(message)

class SensorNotAvailableException :
    SensorException("Сенсор недоступний на цьому пристрої")

class SensorPermissionException :
    SensorException("Немає дозволу на розпізнавання фізичної активності")

class SensorRegistrationException :
    SensorException("Не вдалося підписатися на показники сенсора")

/**
 * Спільна обгортка над [SensorManager].
 *
 * Показники надаються як холодний [Flow]: слухач реєструється
 * під час збирання потоку та гарантовано знімається
 * (unregisterListener) після скасування корутини.
 */
@Singleton
class DeviceSensorReader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private fun sensor(type: Int): Sensor? = runCatching {
        sensorManager?.getDefaultSensor(type)
    }.getOrNull()

    fun isAvailable(type: Int): Boolean = sensor(type) != null

    val isStepCounterAvailable: Boolean
        get() = isAvailable(Sensor.TYPE_STEP_COUNTER)

    val isAccelerometerAvailable: Boolean
        get() = isAvailable(Sensor.TYPE_ACCELEROMETER)

    /** До Android 10 runtime-дозвіл для крокоміра не потрібен. */
    fun hasActivityRecognitionPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED

    /** Накопичувальне значення TYPE_STEP_COUNTER з моменту перезавантаження. */
    fun stepCounterValues(): Flow<Float> =
        sensorEvents(
            type = Sensor.TYPE_STEP_COUNTER,
            samplingPeriodUs = SensorManager.SENSOR_DELAY_NORMAL,
            requiresActivityRecognition = true
        ).mapNotNull { it.firstOrNull() }

    /** Прискорення за осями X, Y, Z у м/с². */
    fun accelerometerSamples(): Flow<AccelerationSample> =
        sensorEvents(
            type = Sensor.TYPE_ACCELEROMETER,
            samplingPeriodUs = SensorManager.SENSOR_DELAY_UI
        ).mapNotNull { values ->
            if (values.size < 3) {
                null
            } else {
                AccelerationSample(values[0], values[1], values[2])
            }
        }

    fun sensorEvents(
        type: Int,
        samplingPeriodUs: Int,
        requiresActivityRecognition: Boolean = false
    ): Flow<FloatArray> = callbackFlow {
        if (requiresActivityRecognition && !hasActivityRecognitionPermission()) {
            throw SensorPermissionException()
        }

        val manager = sensorManager ?: throw SensorNotAvailableException()
        val sensor = sensor(type) ?: throw SensorNotAvailableException()

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // SensorEvent перевикористовується системою,
                // тому копіюємо масив значень.
                trySend(event.values.copyOf())
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val registered = try {
            manager.registerListener(listener, sensor, samplingPeriodUs)
        } catch (exception: SecurityException) {
            throw SensorPermissionException()
        } catch (exception: RuntimeException) {
            throw SensorRegistrationException()
        }

        if (!registered) {
            manager.unregisterListener(listener)
            throw SensorRegistrationException()
        }

        awaitClose {
            manager.unregisterListener(listener)
        }
    }.conflate()
}
