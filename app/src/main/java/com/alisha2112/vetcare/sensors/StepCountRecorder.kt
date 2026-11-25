package com.alisha2112.vetcare.sensors

import com.alisha2112.vetcare.data.model.ObservationType
import com.alisha2112.vetcare.data.repository.ObservationRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

sealed interface StepSaveResult {
    data object Saved : StepSaveResult
    data object AlreadySaved : StepSaveResult
    data class Invalid(val message: String) : StepSaveResult
}

/**
 * Збереження результату сеансу крокоміра в журнал
 * (аналогічно до BlePulseRecorder).
 */
class StepCountRecorder @Inject constructor(
    private val repository: ObservationRepository
) {
    private val mutex = Mutex()
    private var lastSavedSessionId: Long? = null

    suspend fun save(
        petName: String,
        steps: Long,
        sessionId: Long,
        note: String = ""
    ): StepSaveResult {
        val name = petName.trim()

        if (name.isBlank()) {
            return StepSaveResult.Invalid("Вкажіть кличку тварини")
        }

        if (name.length > MAX_NAME_LENGTH) {
            return StepSaveResult.Invalid(
                "Кличка занадто довга (до $MAX_NAME_LENGTH символів)"
            )
        }

        if (steps <= 0L || sessionId <= 0L) {
            return StepSaveResult.Invalid(
                "Немає коректного результату вимірювання"
            )
        }

        return mutex.withLock {
            if (lastSavedSessionId == sessionId) {
                return@withLock StepSaveResult.AlreadySaved
            }

            val userNote = note.trim().take(MAX_NOTE_LENGTH)

            repository.addRecord(
                petName = name,
                type = ObservationType.STEPS,
                value = steps.toDouble(),
                note = buildString {
                    append("Джерело: крокомір пристрою (TYPE_STEP_COUNTER).")
                    if (userNote.isNotEmpty()) {
                        append(" ")
                        append(userNote)
                    }
                }
            )

            // Позначаємо сеанс збереженим лише після успішного запису.
            lastSavedSessionId = sessionId
            StepSaveResult.Saved
        }
    }

    private companion object {
        const val MAX_NAME_LENGTH = 50
        const val MAX_NOTE_LENGTH = 200
    }
}
