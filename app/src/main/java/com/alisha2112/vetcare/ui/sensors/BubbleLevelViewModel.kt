package com.alisha2112.vetcare.ui.sensors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alisha2112.vetcare.sensors.AccelerationSample
import com.alisha2112.vetcare.sensors.DeviceSensorReader
import com.alisha2112.vetcare.sensors.LowPassFilter
import com.alisha2112.vetcare.sensors.SensorException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BubbleLevelUiState(
    val sensorAvailable: Boolean,
    /** Згладжений показник акселерометра або null до першого значення. */
    val sample: AccelerationSample? = null,
    val error: String? = null
)

@HiltViewModel
class BubbleLevelViewModel @Inject constructor(
    private val sensorReader: DeviceSensorReader
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        BubbleLevelUiState(
            sensorAvailable = sensorReader.isAccelerometerAvailable
        )
    )
    val uiState = _uiState.asStateFlow()

    private val filter = LowPassFilter()
    private var sensorJob: Job? = null

    /** Підписка лише поки екран у стані RESUMED. */
    fun start() {
        if (!_uiState.value.sensorAvailable || sensorJob?.isActive == true) return

        filter.reset()
        _uiState.update { it.copy(error = null) }

        sensorJob = viewModelScope.launch {
            try {
                sensorReader.accelerometerSamples().collect { raw ->
                    val filtered = filter.filter(raw) ?: return@collect
                    _uiState.update { it.copy(sample = filtered) }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: SensorException) {
                _uiState.update { it.copy(error = exception.message) }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(error = "Помилка отримання даних акселерометра")
                }
            }
        }
    }

    fun stop() {
        sensorJob?.cancel()
        sensorJob = null
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
