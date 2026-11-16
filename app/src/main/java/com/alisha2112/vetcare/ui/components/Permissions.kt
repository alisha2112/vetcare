package com.alisha2112.vetcare.ui.components

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

data class PermissionsState(
    val granted: Boolean,
    val deniedOnce: Boolean,
    val request: () -> Unit,
    val openSettings: () -> Unit
)

private fun Context.hasPermissions(
    permissions: List<String>,
    requireAll: Boolean
): Boolean {
    val results = permissions.map { permission ->
        ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    return if (requireAll) {
        results.all { it }
    } else {
        results.any { it }
    }
}

@Composable
fun rememberPermissionsState(
    permissions: List<String>,
    requireAll: Boolean = true
): PermissionsState {
    val context = LocalContext.current
    val requestedPermissions = remember(permissions) {
        permissions.distinct()
    }

    var granted by remember(
        requestedPermissions,
        requireAll
    ) {
        mutableStateOf(
            context.hasPermissions(
                requestedPermissions,
                requireAll
            )
        )
    }

    var deniedOnce by rememberSaveable(
        requestedPermissions
    ) {
        mutableStateOf(false)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        granted = context.hasPermissions(
            requestedPermissions,
            requireAll
        )

        if (!granted) {
            deniedOnce = true
        }
    }

    // Повторно перевіряємо дозволи після повернення
    // користувача з налаштувань Android.
    LifecycleResumeEffect(
        requestedPermissions,
        requireAll
    ) {
        granted = context.hasPermissions(
            requestedPermissions,
            requireAll
        )

        onPauseOrDispose { }
    }

    return PermissionsState(
        granted = granted,
        deniedOnce = deniedOnce,
        request = {
            if (requestedPermissions.isNotEmpty()) {
                launcher.launch(
                    requestedPermissions.toTypedArray()
                )
            }
        },
        openSettings = {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            ).apply {
                data = Uri.parse(
                    "package:${context.packageName}"
                )
            }

            context.startActivity(intent)
        }
    )
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    state: PermissionsState,
    modifier: Modifier = Modifier
) {
    if (state.granted) return

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )

            if (state.deniedOnce) {
                Text(
                    text = "Доступ не надано. Можна повторити запит або змінити дозвіл у налаштуваннях.",
                    style = MaterialTheme.typography.bodySmall
                )

                Button(
                    onClick = state.request
                ) {
                    Text("Повторити запит")
                }

                OutlinedButton(
                    onClick = state.openSettings
                ) {
                    Text("Відкрити налаштування")
                }
            } else {
                Button(
                    onClick = state.request
                ) {
                    Text("Надати дозвіл")
                }
            }
        }
    }
}
