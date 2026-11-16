package com.alisha2112.vetcare.ui.ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alisha2112.vetcare.ble.BleDevice
import com.alisha2112.vetcare.ble.BleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import com.alisha2112.vetcare.ble.BleGattClient
import com.alisha2112.vetcare.ble.GattPhase
import kotlinx.coroutines.delay

data class BleScanUiState(
    val devices: List<BleDevice> = emptyList(),
    val isScanning: Boolean = false,
    val hasScanned: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class BleViewModel @Inject constructor(
    private val bleManager: BleManager,
    private val gattClient: BleGattClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(BleScanUiState())
    val uiState = _uiState.asStateFlow()
    val connection = gattClient.state

    private var connectionTimeoutJob: Job? = null

    fun connectTo(device: BleDevice) {
        stopScan()
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

    private var scanJob: Job? = null

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

    override fun onCleared() {
        stopScan()
        disconnect()
        super.onCleared()
    }
}
