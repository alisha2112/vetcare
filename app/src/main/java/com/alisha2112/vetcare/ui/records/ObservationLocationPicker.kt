package com.alisha2112.vetcare.ui.records

import android.Manifest
import android.os.CancellationSignal
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.alisha2112.vetcare.location.LocationReader
import com.alisha2112.vetcare.ui.components.rememberPermissionsState
import java.util.Locale

@Composable
fun ObservationLocationPicker(
    latitude: Double?,
    longitude: Double?,
    onLocationChanged: (Double?, Double?) -> Unit,
    onLoadingChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current

    val reader = remember(context) {
        LocationReader(context.applicationContext)
    }

    val permissionsState = rememberPermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ),
        requireAll = false
    )

    var loading by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    var pendingRequest by remember {
        mutableStateOf<CancellationSignal?>(null)
    }

    var requestGeneration by remember {
        mutableStateOf(0L)
    }

    fun cancelRequest() {
        requestGeneration++
        pendingRequest?.cancel()
        pendingRequest = null
        loading = false
        onLoadingChanged(false)
    }

    fun attachLocation() {
        if (!permissionsState.granted) {
            permissionsState.request()
            return
        }

        cancelRequest()

        error = null
        loading = true
        onLoadingChanged(true)

        val generation = requestGeneration

        pendingRequest = reader.getCurrentLocation(
            onSuccess = { location ->
                if (generation == requestGeneration) {
                    onLocationChanged(
                        location.latitude,
                        location.longitude
                    )

                    pendingRequest = null
                    loading = false
                    onLoadingChanged(false)
                }
            },
            onError = { message ->
                if (generation == requestGeneration) {
                    error = message
                    pendingRequest = null
                    loading = false
                    onLoadingChanged(false)
                }
            }
        )
    }

    LaunchedEffect(permissionsState.granted) {
        if (!permissionsState.granted) {
            cancelRequest()
            onLocationChanged(null, null)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            requestGeneration++
            pendingRequest?.cancel()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Місцезнаходження",
            style = MaterialTheme.typography.titleSmall
        )

        Text(
            text = "Необов'язково. Координати " +
                "зберігаються разом зі спостереженням.",
            style = MaterialTheme.typography.bodySmall
        )

        if (loading) {
            CircularProgressIndicator()

            Text("Визначення координат...")

            OutlinedButton(
                onClick = ::cancelRequest
            ) {
                Text("Скасувати пошук")
            }
        } else {
            if (latitude != null && longitude != null) {
                Text(
                    text = "Місцезнаходження прикріплено",
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    String.format(
                        Locale.US,
                        "%.6f, %.6f",
                        latitude,
                        longitude
                    )
                )

                OutlinedButton(
                    onClick = {
                        cancelRequest()
                        onLocationChanged(null, null)
                        error = null
                    }
                ) {
                    Text("Прибрати місцезнаходження")
                }
            }

            OutlinedButton(
                onClick = ::attachLocation,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (latitude == null) {
                        "Прикріпити місцезнаходження"
                    } else {
                        "Оновити місцезнаходження"
                    }
                )
            }

            if (
                !permissionsState.granted &&
                permissionsState.deniedOnce
            ) {
                Text(
                    text = "Доступ до геолокації не надано. " +
                        "Можна зберегти запис без координат.",
                    style = MaterialTheme.typography.bodySmall
                )

                OutlinedButton(
                    onClick = permissionsState.openSettings
                ) {
                    Text("Відкрити налаштування")
                }
            }
        }

        error?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
