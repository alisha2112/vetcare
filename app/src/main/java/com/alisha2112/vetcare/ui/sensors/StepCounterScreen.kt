package com.alisha2112.vetcare.ui.sensors

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alisha2112.vetcare.sensors.StepSessionStatus
import com.alisha2112.vetcare.ui.components.PermissionCard
import com.alisha2112.vetcare.ui.components.PermissionsState
import com.alisha2112.vetcare.ui.components.rememberPermissionsState

@Composable
fun StepCounterScreen(
    onBack: () -> Unit,
    viewModel: StepCounterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // ACTIVITY_RECOGNITION є runtime-дозволом лише з Android 10.
    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listOf(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            emptyList()
        }
    }

    val permissionState = rememberPermissionsState(permissions)

    // Слухач сенсора активний лише поки екран на передньому плані.
    LifecycleResumeEffect(viewModel) {
        viewModel.onForegroundChanged(true)
        onPauseOrDispose { viewModel.onForegroundChanged(false) }
    }

    // Дозвіл відкликано під час вимірювання.
    LaunchedEffect(permissionState.granted) {
        if (!permissionState.granted && state.session.isMeasuring) {
            viewModel.stopMeasurement()
        }
    }

    var showSaveDialog by rememberSaveable { mutableStateOf(false) }

    // Діалог закривається після успішного збереження.
    LaunchedEffect(state.savedSessionId) {
        if (state.isCurrentSessionSaved) showSaveDialog = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Назад")
        }

        Text(
            text = "Крокомір",
            style = MaterialTheme.typography.headlineMedium
        )

        if (!state.sensorAvailable) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Апаратний крокомір (TYPE_STEP_COUNTER) відсутній " +
                            "на цьому пристрої. Вимірювання кроків недоступне.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        } else {
            PermissionCard(
                title = "Дозвіл на фізичну активність",
                description = "Для отримання показників крокоміра Android " +
                        "вимагає дозвіл на розпізнавання фізичної активності.",
                state = permissionState
            )

            StepCounterCard(
                state = state,
                permissionState = permissionState
            )

            Button(
                onClick = viewModel::startMeasurement,
                enabled = permissionState.granted && !state.session.isMeasuring,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Почати вимірювання")
            }

            OutlinedButton(
                onClick = viewModel::stopMeasurement,
                enabled = state.session.isMeasuring,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Зупинити вимірювання")
            }

            OutlinedButton(
                onClick = viewModel::resetCounter,
                enabled = state.session.status != StepSessionStatus.IDLE &&
                        !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Скинути лічильник")
            }

            Button(
                onClick = {
                    viewModel.clearSaveMessage()
                    showSaveDialog = true
                },
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Зберегти в журнал")
            }

            state.saveMessage?.let { message ->
                if (!showSaveDialog) {
                    Text(
                        text = message,
                        color = if (state.saveIsError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Як це працює",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Апаратний сенсор TYPE_STEP_COUNTER повертає загальну " +
                            "кількість кроків від останнього перезавантаження. " +
                            "На початку сеансу перше значення стає базовим, а кроки " +
                            "сеансу — це різниця між поточним і базовим значенням.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Вимірювання триває лише поки екран відкритий. Кроки, " +
                            "зроблені у фоні, не зараховуються. Це навчальна " +
                            "реалізація, а не медичний прилад.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (showSaveDialog) {
        SaveStepsDialog(
            steps = state.session.steps,
            isSaving = state.isSaving,
            errorMessage = state.saveMessage?.takeIf { state.saveIsError },
            onDismiss = {
                if (!state.isSaving) showSaveDialog = false
            },
            onSave = viewModel::save
        )
    }
}

@Composable
private fun StepCounterCard(
    state: StepCounterUiState,
    permissionState: PermissionsState
) {
    val status = when {
        !permissionState.granted && permissionState.deniedOnce ->
            "Дозвіл відхилено"
        !permissionState.granted ->
            "Дозвіл не надано"
        state.error != null ->
            "Помилка: ${state.error}"
        else -> when (state.session.status) {
            StepSessionStatus.IDLE -> "Сенсор доступний. Вимірювання не розпочато"
            StepSessionStatus.WAITING_FOR_SENSOR -> "Очікування першого показника сенсора…"
            StepSessionStatus.ACTIVE -> "Триває вимірювання"
            StepSessionStatus.STOPPED -> "Вимірювання зупинено"
        }
    }

    val isProblem = !permissionState.granted || state.error != null

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Сенсор: доступний",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = state.session.steps.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "кроків за сеанс",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = status,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = if (isProblem) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun SaveStepsDialog(
    steps: Long,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (petName: String, note: String) -> Unit
) {
    var petName by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Зберегти в журнал") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Кроків за сеанс: $steps")

                OutlinedTextField(
                    value = petName,
                    onValueChange = { petName = it.take(50) },
                    label = { Text("Кличка тварини") },
                    singleLine = true,
                    isError = petName.isNotEmpty() && petName.isBlank(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(200) },
                    label = { Text("Примітка (необов'язково)") },
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(petName, note) },
                enabled = petName.isNotBlank() && !isSaving
            ) {
                Text(if (isSaving) "Збереження…" else "Зберегти")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Скасувати")
            }
        }
    )
}
