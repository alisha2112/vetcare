package com.alisha2112.vetcare.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

data class BleDevice(
    val name: String,
    val address: String,
    val rssi: Int
)

class BleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private fun hasPermissions(): Boolean {
        val permissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                listOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }

        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun scan(): Flow<BleDevice> = callbackFlow {
        if (!hasPermissions()) {
            close(
                SecurityException(
                    "Не надано необхідних дозволів Bluetooth"
                )
            )
            return@callbackFlow
        }

        val scanner = runCatching {
            val manager = context.getSystemService(
                Context.BLUETOOTH_SERVICE
            ) as? BluetoothManager

            manager?.adapter?.bluetoothLeScanner
        }.getOrNull()

        if (scanner == null) {
            close(
                IllegalStateException(
                    "Bluetooth вимкнено або BLE недоступний"
                )
            )
            return@callbackFlow
        }

        val callback = object : ScanCallback() {

            override fun onScanResult(
                callbackType: Int,
                result: ScanResult
            ) {
                val address = runCatching {
                    result.device.address
                }.getOrNull() ?: return

                val name = runCatching {
                    result.device.name
                }.getOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?: result.scanRecord?.deviceName
                        ?.takeIf { it.isNotBlank() }
                    ?: "Невідомий пристрій"

                trySend(
                    BleDevice(
                        name = name,
                        address = address,
                        rssi = result.rssi
                    )
                )
            }

            override fun onScanFailed(errorCode: Int) {
                close(
                    IllegalStateException(
                        "Помилка BLE-сканування (код $errorCode)"
                    )
                )
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(
                ScanSettings.SCAN_MODE_LOW_LATENCY
            )
            .build()

        try {
            scanner.startScan(
                null,
                settings,
                callback
            )
        } catch (exception: Exception) {
            close(exception)
            return@callbackFlow
        }

        awaitClose {
            runCatching {
                scanner.stopScan(callback)
            }
        }
    }
}
