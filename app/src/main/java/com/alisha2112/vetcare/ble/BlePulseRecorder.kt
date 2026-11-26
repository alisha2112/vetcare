package com.alisha2112.vetcare.ble

import com.alisha2112.vetcare.data.model.ObservationType
import com.alisha2112.vetcare.data.repository.ObservationRepository
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject

sealed interface PulseSaveResult {
    data object Saved : PulseSaveResult
    data object AlreadySaved : PulseSaveResult
    data class Invalid(val message: String) : PulseSaveResult
}

class BlePulseRecorder @Inject constructor(
    private val repository: ObservationRepository
) {
    private val mutex = Mutex()
    private var lastSavedMeasurementId: Long? = null

    suspend fun save(
        petName: String,
        pulse: Int?,
        deviceName: String?,
        measurementId: Long
    ): PulseSaveResult {
        val name = petName.trim()

        if (name.isBlank()) {
            return PulseSaveResult.Invalid(
                "Вкажіть кличку тварини"
            )
        }

        if (pulse == null || pulse <= 0 || measurementId <= 0L) {
            return PulseSaveResult.Invalid(
                "Немає коректного BLE-вимірювання"
            )
        }

        mutex.lock()

        try {
            if (lastSavedMeasurementId == measurementId) {
                return PulseSaveResult.AlreadySaved
            }

            val source = deviceName
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.take(80)
                ?: "невідомий BLE-пристрій"

            repository.addRecord(
                petName = name,
                type = ObservationType.PULSE,
                value = pulse.toDouble(),
                note = "Джерело: BLE ($source). " +
                        "Навчальне вимірювання."
            )

            // Позначаємо вимірювання збереженим лише
            // після успішного запису в репозиторій.
            lastSavedMeasurementId = measurementId

            return PulseSaveResult.Saved
        } finally {
            mutex.unlock()
        }
    }
}
