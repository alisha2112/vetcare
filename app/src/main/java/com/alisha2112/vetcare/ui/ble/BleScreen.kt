package com.alisha2112.vetcare.ui.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alisha2112.vetcare.ble.GattPhase
import com.alisha2112.vetcare.ui.components.PermissionCard
import com.alisha2112.vetcare.ui.components.rememberPermissionsState

@SuppressLint("MissingPermission")
private fun isBluetoothEnabled(
    context: Context,
    adapter: BluetoothAdapter?
): Boolean {
    if (adapter == null) return false

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) return false
    }

    return runCatching {
        adapter.isEnabled
    }.getOrDefault(false)
}

@Composable
fun BleScreen(
    onBack: () -> Unit,
    viewModel: BleViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val scanState by viewModel.uiState.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()

    // Дозволи залежать від версії Android.
    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    val permissionState = rememberPermissionsState(
        permissions = permissions
    )

    val adapter = remember(context) {
        val manager = context.getSystemService(
            Context.BLUETOOTH_SERVICE
        ) as? BluetoothManager

        manager?.adapter
    }

    val bleAvailable = remember(context, adapter) {
        context.packageManager.hasSystemFeature(
            PackageManager.FEATURE_BLUETOOTH_LE
        ) && adapter != null
    }

    var bluetoothEnabled by remember {
        mutableStateOf(false)
    }

    // Системний запит на ввімкнення Bluetooth.
    val enableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        bluetoothEnabled = permissionState.granted &&
                isBluetoothEnabled(context, adapter)
    }

    // Перевірка Bluetooth після повернення до екрана.
    LifecycleResumeEffect(permissionState.granted) {
        bluetoothEnabled = permissionState.granted &&
                isBluetoothEnabled(context, adapter)

        onPauseOrDispose { }
    }

    // Зупинка роботи при втраті дозволу або Bluetooth.
    LaunchedEffect(
        permissionState.granted,
        bluetoothEnabled
    ) {
        if (!permissionState.granted || !bluetoothEnabled) {
            viewModel.stopScan()
            viewModel.disconnect()
        }
    }

    // Звільнення ресурсів при виході з екрана.
    DisposableEffect(viewModel) {
        onDispose {
            viewModel.stopScan()
            viewModel.disconnect()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Bluetooth Low Energy",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Пошук і підключення зовнішніх " +
                    "BLE-датчиків для VetCare."
        )

        when {
            !bleAvailable -> {
                Text(
                    text = "BLE недоступний на цьому пристрої.",
                    color = MaterialTheme.colorScheme.error
                )
            }

            !permissionState.granted -> {
                PermissionCard(
                    title = "Доступ до Bluetooth",
                    description = "VetCare потрібен дозвіл " +
                            "для пошуку та підключення BLE-пристроїв.",
                    state = permissionState
                )
            }

            !bluetoothEnabled -> {
                Text(
                    text = "Bluetooth вимкнено.",
                    style = MaterialTheme.typography.titleMedium
                )

                Button(
                    onClick = {
                        enableLauncher.launch(
                            Intent(
                                BluetoothAdapter.ACTION_REQUEST_ENABLE
                            )
                        )
                    }
                ) {
                    Text("Увімкнути Bluetooth")
                }
            }

            else -> {
                // Bluetooth доступний та увімкнений.
                Text(
                    text = "Bluetooth увімкнено.",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )

                // Помилки та інформаційні повідомлення GATT.
                connection.message?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Картка поточного GATT-підключення.
                if (
                    connection.phase != GattPhase.DISCONNECTED
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = connection.deviceName
                                    ?: "BLE-пристрій",
                                style =
                                    MaterialTheme.typography.titleMedium
                            )

                            val statusText = when (
                                connection.phase
                            ) {
                                GattPhase.DISCONNECTED ->
                                    "Не підключено"

                                GattPhase.CONNECTING ->
                                    "Підключення..."

                                GattPhase.DISCOVERING ->
                                    "Пошук GATT-сервісів..."

                                GattPhase.READY ->
                                    "Підключено"
                            }

                            Text("Стан: $statusText")

                            if (
                                connection.phase == GattPhase.READY
                            ) {
                                val batteryText = when {
                                    !connection.supportsBattery ->
                                        "сервіс не підтримується"

                                    connection.batteryPercent == null ->
                                        "немає даних"

                                    else ->
                                        "${connection.batteryPercent}%"
                                }

                                Text(
                                    text = "Заряд батареї: $batteryText"
                                )

                                val heartText = when {
                                    !connection.supportsHeartRate ->
                                        "сервіс не підтримується"

                                    connection.heartRate == null ->
                                        "очікування вимірювання"

                                    else ->
                                        "${connection.heartRate} уд/хв"
                                }

                                Text(
                                    text = "Пульс: $heartText"
                                )
                            }

                            OutlinedButton(
                                onClick = viewModel::disconnect
                            ) {
                                Text("Відключитися")
                            }
                        }
                    }
                }

                // Сканування доступне лише без GATT-з'єднання.
                if (
                    connection.phase == GattPhase.DISCONNECTED
                ) {
                    if (scanState.isScanning) {
                        CircularProgressIndicator()

                        Text("Пошук BLE-пристроїв...")

                        OutlinedButton(
                            onClick = viewModel::stopScan
                        ) {
                            Text("Зупинити пошук")
                        }
                    } else {
                        Button(
                            onClick = viewModel::startScan
                        ) {
                            Text("Знайти пристрої")
                        }
                    }

                    scanState.error?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // Відображення знайдених BLE-пристроїв.
                    if (scanState.devices.isNotEmpty()) {
                        Text(
                            text = "Знайдено пристроїв: " +
                                    scanState.devices.size,
                            style =
                                MaterialTheme.typography.titleMedium
                        )

                        scanState.devices.forEach { device ->
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = device.name,
                                        style =
                                            MaterialTheme.typography
                                                .titleMedium
                                    )

                                    Text(
                                        text = "Адреса: ${device.address}"
                                    )

                                    Text(
                                        text = "RSSI: ${device.rssi} dBm"
                                    )

                                    Button(
                                        onClick = {
                                            viewModel.connectTo(device)
                                        },
                                        enabled = !scanState.isScanning
                                    ) {
                                        Text("Підключитися")
                                    }
                                }
                            }
                        }
                    } else if (
                        scanState.hasScanned &&
                        !scanState.isScanning &&
                        scanState.error == null
                    ) {
                        Text(
                            text = "Пристроїв не знайдено."
                        )
                    }
                }
            }
        }

        TextButton(
            onClick = onBack
        ) {
            Text("Назад")
        }
    }
}
