package com.alisha2112.vetcare.ui.permissions

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alisha2112.vetcare.ui.components.PermissionCard
import com.alisha2112.vetcare.ui.components.rememberPermissionsState

@Composable
fun LocationPermissionScreen(
    onBack: () -> Unit
) {
    val permissionsState = rememberPermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ),
        requireAll = false
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Геолокація",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Перевірка дозволу на визначення " +
                "місцезнаходження для майбутньої " +
                "роботи з картою та маршрутами."
        )

        if (permissionsState.granted) {
            Text(
                text = "Доступ до геолокації надано.",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Дозволено приблизне або точне " +
                    "місцезнаходження."
            )
        } else {
            PermissionCard(
                title = "Потрібен дозвіл",
                description = "VetCare використовуватиме " +
                    "геолокацію для відображення " +
                    "місця спостереження та маршруту.",
                state = permissionsState
            )
        }

        Button(onClick = onBack) {
            Text("Назад")
        }
    }
}
