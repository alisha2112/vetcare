package com.alisha2112.vetcare.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

enum class GattPhase {
    DISCONNECTED,
    CONNECTING,
    DISCOVERING,
    READY
}

data class GattConnectionState(
    val phase: GattPhase = GattPhase.DISCONNECTED,
    val deviceName: String? = null,
    val supportsBattery: Boolean = false,
    val supportsHeartRate: Boolean = false,
    val batteryPercent: Int? = null,
    val heartRate: Int? = null,
    val message: String? = null
)

@SuppressLint("MissingPermission")
@Suppress("DEPRECATION")
class BleGattClient @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private fun uuid(shortCode: String): UUID =
            UUID.fromString(
                "0000$shortCode-0000-1000-8000-00805f9b34fb"
            )

        private val HEART_SERVICE = uuid("180D")
        private val HEART_MEASUREMENT = uuid("2A37")
        private val BATTERY_SERVICE = uuid("180F")
        private val BATTERY_LEVEL = uuid("2A19")
        private val CCCD = uuid("2902")

        fun parseHeartRate(bytes: ByteArray): Int? {
            if (bytes.size < 2) return null

            val flags = bytes[0].toInt() and 0xFF
            val is16Bit = flags and 0x01 != 0

            return if (is16Bit) {
                if (bytes.size < 3) null
                else (bytes[1].toInt() and 0xFF) or
                        ((bytes[2].toInt() and 0xFF) shl 8)
            } else {
                bytes[1].toInt() and 0xFF
            }
        }
    }

    private val _state = MutableStateFlow(GattConnectionState())
    val state = _state.asStateFlow()

    private val generation = AtomicLong(0L)

    @Volatile
    private var activeGatt: BluetoothGatt? = null

    private fun hasConnectPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED

    fun disconnect(message: String? = null) {
        generation.incrementAndGet()

        val oldGatt = activeGatt
        activeGatt = null

        runCatching { oldGatt?.disconnect() }
        runCatching { oldGatt?.close() }

        _state.value = GattConnectionState(message = message)
    }

    private fun fail(
        token: Long,
        gatt: BluetoothGatt?,
        message: String
    ) {
        if (generation.get() != token) {
            runCatching { gatt?.close() }
            return
        }

        generation.incrementAndGet()
        activeGatt = null

        runCatching { gatt?.disconnect() }
        runCatching { gatt?.close() }

        _state.value = GattConnectionState(message = message)
    }

    private fun isCurrent(token: Long): Boolean =
        generation.get() == token

    fun connect(address: String, name: String) {
        disconnect()

        if (!hasConnectPermission()) {
            _state.value = GattConnectionState(
                message = "Немає дозволу на BLE-підключення"
            )
            return
        }

        val adapter = try {
            val manager = context.getSystemService(
                Context.BLUETOOTH_SERVICE
            ) as? BluetoothManager

            manager?.adapter
        } catch (e: Exception) {
            null
        }

        if (adapter == null ||
            runCatching { !adapter.isEnabled }.getOrDefault(true)
        ) {
            _state.value = GattConnectionState(
                message = "Bluetooth недоступний або вимкнений"
            )
            return
        }

        val device = try {
            adapter.getRemoteDevice(address)
        } catch (e: Exception) {
            _state.value = GattConnectionState(
                message = "Некоректна адреса BLE-пристрою"
            )
            return
        }

        val token = generation.incrementAndGet()

        _state.value = GattConnectionState(
            phase = GattPhase.CONNECTING,
            deviceName = name
        )

        val callback = object : BluetoothGattCallback() {

            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
            ) {
                if (!isCurrent(token)) return

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    fail(
                        token,
                        gatt,
                        "Помилка GATT-підключення: $status"
                    )
                    return
                }

                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        _state.update {
                            it.copy(phase = GattPhase.DISCOVERING)
                        }

                        val started = runCatching {
                            gatt.discoverServices()
                        }.getOrDefault(false)

                        if (!started) {
                            fail(
                                token,
                                gatt,
                                "Не вдалося отримати GATT-сервіси"
                            )
                        }
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        fail(
                            token,
                            gatt,
                            "BLE-пристрій відключено"
                        )
                    }
                }
            }

            override fun onServicesDiscovered(
                gatt: BluetoothGatt,
                status: Int
            ) {
                if (!isCurrent(token)) return

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    fail(
                        token,
                        gatt,
                        "Помилка пошуку сервісів: $status"
                    )
                    return
                }

                val battery = gatt.getService(BATTERY_SERVICE)
                    ?.getCharacteristic(BATTERY_LEVEL)

                val heart = gatt.getService(HEART_SERVICE)
                    ?.getCharacteristic(HEART_MEASUREMENT)

                _state.update {
                    it.copy(
                        phase = GattPhase.READY,
                        supportsBattery = battery != null,
                        supportsHeartRate = heart != null,
                        message = if (
                            battery == null && heart == null
                        ) {
                            "Пристрій не підтримує сервіси " +
                                    "Battery або Heart Rate"
                        } else null
                    )
                }

                val canReadBattery = battery != null &&
                        battery.properties and
                        BluetoothGattCharacteristic.PROPERTY_READ != 0

                if (canReadBattery) {
                    val started = runCatching {
                        gatt.readCharacteristic(battery!!)
                    }.getOrDefault(false)

                    if (started) return

                    _state.update {
                        it.copy(
                            message = "Не вдалося прочитати батарею"
                        )
                    }
                }

                subscribeToHeartRate(gatt, token)
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) {
                handleRead(
                    gatt, characteristic, value, status, token
                )
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                if (Build.VERSION.SDK_INT <
                    Build.VERSION_CODES.TIRAMISU
                ) {
                    handleRead(
                        gatt,
                        characteristic,
                        characteristic.value ?: byteArrayOf(),
                        status,
                        token
                    )
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                handleNotification(
                    characteristic, value, token
                )
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                if (Build.VERSION.SDK_INT <
                    Build.VERSION_CODES.TIRAMISU
                ) {
                    handleNotification(
                        characteristic,
                        characteristic.value ?: byteArrayOf(),
                        token
                    )
                }
            }

            override fun onDescriptorWrite(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int
            ) {
                if (!isCurrent(token)) return

                if (descriptor.uuid == CCCD &&
                    status != BluetoothGatt.GATT_SUCCESS
                ) {
                    _state.update {
                        it.copy(
                            message = "Не вдалося увімкнути " +
                                    "сповіщення пульсу: $status"
                        )
                    }
                }
            }
        }

        try {
            val gatt = device.connectGatt(
                context,
                false,
                callback,
                BluetoothDevice.TRANSPORT_LE
            )

            if (!isCurrent(token)) {
                runCatching { gatt?.close() }
            } else if (gatt == null) {
                fail(token, null, "Не вдалося створити GATT-з'єднання")
            } else {
                activeGatt = gatt
            }
        } catch (e: Exception) {
            fail(
                token,
                null,
                "Помилка підключення: ${e.message ?: "невідома"}"
            )
        }
    }

    private fun handleRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int,
        token: Long
    ) {
        if (!isCurrent(token)) return
        if (characteristic.uuid != BATTERY_LEVEL) return

        if (status == BluetoothGatt.GATT_SUCCESS &&
            value.isNotEmpty()
        ) {
            val percent = value[0].toInt() and 0xFF

            if (percent in 0..100) {
                _state.update {
                    it.copy(batteryPercent = percent)
                }
            } else {
                _state.update {
                    it.copy(message = "Некоректне значення батареї")
                }
            }
        } else {
            _state.update {
                it.copy(message = "Помилка читання Battery Level")
            }
        }

        subscribeToHeartRate(gatt, token)
    }

    private fun handleNotification(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        token: Long
    ) {
        if (!isCurrent(token)) return
        if (characteristic.uuid != HEART_MEASUREMENT) return

        val pulse = parseHeartRate(value)

        if (pulse != null) {
            _state.update {
                it.copy(heartRate = pulse)
            }
        }
    }

    private fun subscribeToHeartRate(
        gatt: BluetoothGatt,
        token: Long
    ) {
        if (!isCurrent(token)) return

        val characteristic = gatt.getService(HEART_SERVICE)
            ?.getCharacteristic(HEART_MEASUREMENT)
            ?: return

        if (characteristic.properties and
            BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0
        ) {
            _state.update {
                it.copy(
                    message = "Характеристика пульсу " +
                            "не підтримує notifications"
                )
            }
            return
        }

        val descriptor = characteristic.getDescriptor(CCCD)

        if (descriptor == null) {
            _state.update {
                it.copy(message = "Відсутній дескриптор CCCD")
            }
            return
        }

        val started = runCatching {
            if (!gatt.setCharacteristicNotification(
                    characteristic, true
                )
            ) {
                false
            } else if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {
                gatt.writeDescriptor(
                    descriptor,
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                ) == BluetoothStatusCodes.SUCCESS
            } else {
                descriptor.value =
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        }.getOrDefault(false)

        if (!started) {
            _state.update {
                it.copy(
                    message = "Не вдалося активувати " +
                            "сповіщення пульсу"
                )
            }
        }
    }
}
