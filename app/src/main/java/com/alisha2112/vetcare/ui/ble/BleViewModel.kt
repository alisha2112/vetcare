
package com.alisha2112.vetcare.ui.ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alisha2112.vetcare.ble.BleDevice
import com.alisha2112.vetcare.ble.BleGattClient
import com.alisha2112.vetcare.ble.BleManager
import com.alisha2112.vetcare.ble.GattPhase
import com.alisha2112.vetcare.ble.BlePulseRecorder
import com.alisha2112.vetcare.ble.PulseSaveResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class BleScanUiState(
    val devices: List<BleDevice> = emptyList(),
    val isScanning: Boolean = false,
    val hasScanned: Boolean = false,
    val error: String? = null
)

data class BleSaveUiState(
    val isSaving: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val savedMeasurementId: Long? = null
)

@HiltViewModel
class BleViewModel @Inject constructor(
    private val bleManager: BleManager,
    private val gattClient: BleGattClient,
    private val pulseRecorder: BlePulseRecorder
) : ViewModel() {

    // Стани сканування та GATT-підключення
    private val _uiState = MutableStateFlow(BleScanUiState())
    val uiState = _uiState.asStateFlow()

    val connection = gattClient.state

    // Стан збереження вимірювання в Room
    private val _saveState = MutableStateFlow(BleSaveUiState())
    val saveState = _saveState.asStateFlow()

    private var scanJob: Job? = null
    private var connectionTimeoutJob: Job? = null

    // 1. BLE-сканування
    fun startScan() {
        if (scanJob?.isActive == true) return

        _uiState.value = BleScanUiState(
            isScanning = true,
            hasScanned = true
        )

        scanJob = viewModelScope.launch {
            try {
                withTimeoutOrNull(10_000L) {
                    bleManager.scan().collect { device ->
                        _uiState.update { state ->
                            val updatedDevices =
                                state.devices.filterNot {
                                    it.address == device.address
                                } + device

                            state.copy(
                                devices = updatedDevices
                                    .sortedByDescending { it.rssi }
                            )
                        }
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        error = exception.message
                            ?: "Не вдалося виконати BLE-пошук"
                    )
                }
            } finally {
                _uiState.update {
                    it.copy(isScanning = false)
                }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
    }

    // 2. GATT-підключення
    fun connectTo(device: BleDevice) {
        stopScan()
        clearSaveMessage()
        connectionTimeoutJob?.cancel()

        gattClient.connect(
            address = device.address,
            name = device.name
        )

        connectionTimeoutJob = viewModelScope.launch {
            delay(15_000L)

            if (
                connection.value.phase == GattPhase.CONNECTING ||
                connection.value.phase == GattPhase.DISCOVERING
            ) {
                gattClient.disconnect(
                    "Час очікування GATT-підключення вичерпано"
                )
            }
        }
    }

    fun disconnect() {
        connectionTimeoutJob?.cancel()
        gattClient.disconnect()
    }

    // 3. Очищення повідомлення збереження
    fun clearSaveMessage() {
        _saveState.update {
            it.copy(
                message = null,
                isError = false
            )
        }
    }

    fun savePulse(petName: String) {
        if (_saveState.value.isSaving) return

        val current = connection.value

        if (
            current.phase != GattPhase.READY ||
            !current.supportsHeartRate
        ) {
            _saveState.update {
                it.copy(
                    message = "BLE-пристрій не готовий до вимірювання",
                    isError = true
                )
            }
            return
        }

        _saveState.update {
            it.copy(
                isSaving = true,
                message = null,
                isError = false
            )
        }

        viewModelScope.launch {
            try {
                when (
                    val result = pulseRecorder.save(
                        petName = petName,
                        pulse = current.heartRate,
                        deviceName = current.deviceName,
                        measurementId = current.measurementId
                    )
                ) {
                    PulseSaveResult.Saved -> {
                        _saveState.update {
                            it.copy(
                                message = "Пульс успішно збережено в журналі",
                                isError = false,
                                savedMeasurementId = current.measurementId
                            )
                        }
                    }

                    PulseSaveResult.AlreadySaved -> {
                        _saveState.update {
                            it.copy(
                                message = "Це вимірювання вже збережено",
                                isError = true
                            )
                        }
                    }

                    is PulseSaveResult.Invalid -> {
                        _saveState.update {
                            it.copy(
                                message = result.message,
                                isError = true
                            )
                        }
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _saveState.update {
                    it.copy(
                        message = "Не вдалося зберегти вимірювання",
                        isError = true
                    )
                }
            } finally {
                _saveState.update {
                    it.copy(isSaving = false)
                }
            }
        }
    }

    // 5. Звільнення ресурсів
    override fun onCleared() {
        stopScan()
        disconnect()
        super.onCleared()
    }
}
