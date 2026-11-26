package com.alisha2112.vetcare.ui.sensors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alisha2112.vetcare.sensors.DeviceSensorReader
import com.alisha2112.vetcare.sensors.SensorException
import com.alisha2112.vetcare.sensors.SensorPermissionException
import com.alisha2112.vetcare.sensors.StepCountRecorder
import com.alisha2112.vetcare.sensors.StepSaveResult
import com.alisha2112.vetcare.sensors.StepSession
import com.alisha2112.vetcare.sensors.StepSessionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StepCounterUiState(
    val sensorAvailable: Boolean,
    val session: StepSession = StepSession(),
    val error: String? = null,
    val isSaving: Boolean = false,
    val saveMessage: String? = null,
    val saveIsError: Boolean = false,
    val savedSessionId: Long? = null
) {
    val isCurrentSessionSaved: Boolean
        get() = savedSessionId != null && savedSessionId == session.sessionId

    val canSave: Boolean
        get() = session.status == StepSessionStatus.STOPPED &&
                session.steps > 0 &&
                !isSaving &&
                !isCurrentSessionSaved
}

@HiltViewModel
class StepCounterViewModel @Inject constructor(
    private val sensorReader: DeviceSensorReader,
    private val recorder: StepCountRecorder
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StepCounterUiState(
            sensorAvailable = sensorReader.isStepCounterAvailable
        )
    )
    val uiState = _uiState.asStateFlow()

    private var sensorJob: Job? = null
    private var inForeground = false
    private var nextSessionId = 1L

    fun startMeasurement() {
        val state = _uiState.value
        if (!state.sensorAvailable || state.session.isMeasuring) return

        if (!sensorReader.hasActivityRecognitionPermission()) {
            _uiState.update {
                it.copy(error = SensorPermissionException().message)
            }
            return
        }

        _uiState.update {
            it.copy(
                session = it.session.start(nextSessionId++),
                error = null,
                saveMessage = null,
                saveIsError = false
            )
        }

        if (inForeground) subscribe()
    }

    fun stopMeasurement() {
        unsubscribe()
        _uiState.update { it.copy(session = it.session.stop()) }
    }

    fun resetCounter() {
        unsubscribe()
        _uiState.update {
            it.copy(
                session = it.session.reset(),
                error = null,
                saveMessage = null,
                saveIsError = false
            )
        }
    }

    /**
     * Викликається з екрана при ON_RESUME / ON_PAUSE.
     * У фоні слухач сенсора не залишається активним.
     */
    fun onForegroundChanged(foreground: Boolean) {
        inForeground = foreground

        if (!foreground) {
            unsubscribe()
            _uiState.update {
                it.copy(session = it.session.suspendSegment())
            }
        } else if (_uiState.value.session.isMeasuring) {
            subscribe()
        }
    }

    fun clearSaveMessage() {
        _uiState.update { it.copy(saveMessage = null, saveIsError = false) }
    }

    fun save(petName: String, note: String) {
        val state = _uiState.value
        if (!state.canSave) return

        val session = state.session

        _uiState.update {
            it.copy(isSaving = true, saveMessage = null, saveIsError = false)
        }

        viewModelScope.launch {
            try {
                when (
                    val result = recorder.save(
                        petName = petName,
                        steps = session.steps,
                        sessionId = session.sessionId,
                        note = note
                    )
                ) {
                    StepSaveResult.Saved -> _uiState.update {
                        it.copy(
                            saveMessage = "Кроки збережено в журналі",
                            saveIsError = false,
                            savedSessionId = session.sessionId
                        )
                    }

                    StepSaveResult.AlreadySaved -> _uiState.update {
                        it.copy(
                            saveMessage = "Результат цього сеансу вже збережено",
                            saveIsError = true,
                            savedSessionId = session.sessionId
                        )
                    }

                    is StepSaveResult.Invalid -> _uiState.update {
                        it.copy(saveMessage = result.message, saveIsError = true)
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        saveMessage = "Не вдалося зберегти результат",
                        saveIsError = true
                    )
                }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun subscribe() {
        if (sensorJob?.isActive == true) return

        sensorJob = viewModelScope.launch {
            try {
                sensorReader.stepCounterValues().collect { value ->
                    _uiState.update {
                        it.copy(session = it.session.onSensorValue(value))
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: SensorException) {
                failMeasurement(exception.message)
            } catch (exception: Exception) {
                failMeasurement(null)
            }
        }
    }

    private fun failMeasurement(message: String?) {
        _uiState.update {
            it.copy(
                session = it.session.stop(),
                error = message ?: "Помилка отримання даних крокоміра"
            )
        }
    }

    private fun unsubscribe() {
        sensorJob?.cancel()
        sensorJob = null
    }

    override fun onCleared() {
        unsubscribe()
        super.onCleared()
    }
}
