package com.alisha2112.vetcare.ui.records

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.alisha2112.vetcare.data.photo.ObservationPhotoStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun ObservationPhotoPicker(
    photoPath: String?,
    onPhotoChanged: (String?) -> Unit,
    onBusyChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val storage = remember(context) {
        ObservationPhotoStorage(
            context.applicationContext
        )
    }

    val currentOnPhotoChanged by rememberUpdatedState(
        onPhotoChanged
    )

    // Шлях до файлу, у який камера записує фото.
    var pendingCameraPath by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var isPicking by rememberSaveable {
        mutableStateOf(false)
    }

    var isImporting by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    val busy =
        pendingCameraPath != null ||
            isPicking ||
            isImporting

    LaunchedEffect(busy) {
        onBusyChanged(busy)
    }

    // 1. Отримання результату системної камери.
    val cameraLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->

            val path = pendingCameraPath
            pendingCameraPath = null

            if (path != null) {
                if (
                    success &&
                    storage.isPhotoReady(path)
                ) {
                    currentOnPhotoChanged(path)
                    error = null
                } else {
                    storage.deletePhoto(path)

                    if (success) {
                        error =
                            "Не вдалося отримати фотографію"
                    }
                }
            }
        }

    // 2. Отримання результату Photo Picker.
    val galleryLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.PickVisualMedia()
        ) { uri ->

            isPicking = false

            if (uri != null) {
                isImporting = true
                error = null

                scope.launch {
                    try {
                        val importedPath =
                            storage.importPhoto(uri)

                        currentOnPhotoChanged(importedPath)

                    } catch (
                        exception: CancellationException
                    ) {
                        throw exception

                    } catch (exception: Exception) {
                        error =
                            "Не вдалося завантажити фотографію"

                    } finally {
                        isImporting = false
                    }
                }
            }
        }

    // 3. Запуск системної камери.
    fun takePhoto() {
        if (busy) return

        error = null

        try {
            val target = storage.createCameraTarget()

            pendingCameraPath = target.relativePath
            onBusyChanged(true)

            cameraLauncher.launch(target.uri)

        } catch (exception: Exception) {
            storage.deletePhoto(pendingCameraPath)
            pendingCameraPath = null

            error =
                "Не вдалося відкрити системну камеру"
        }
    }

    // 4. Запуск системної галереї.
    fun selectPhoto() {
        if (busy) return

        error = null
        isPicking = true
        onBusyChanged(true)

        try {
            galleryLauncher.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts
                        .PickVisualMedia.ImageOnly
                )
            )
        } catch (exception: Exception) {
            isPicking = false

            error =
                "Не вдалося відкрити галерею"
        }
    }

    // 5. Інтерфейс вибору фотографії.
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Фотографія",
            style = MaterialTheme.typography.titleSmall
        )

        Text(
            text = "Необов'язково. Додайте фотографію " +
                "до спостереження за твариною.",
            style = MaterialTheme.typography.bodySmall
        )

        if (busy) {
            CircularProgressIndicator()

            Text(
                text = when {
                    pendingCameraPath != null ->
                        "Очікування фотографії..."

                    isPicking ->
                        "Очікування вибору зображення..."

                    else ->
                        "Завантаження фотографії..."
                },
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            if (photoPath != null) {
                Text(
                    text = "Фотографію прикріплено",
                    color = MaterialTheme.colorScheme.primary,
                    style =
                        MaterialTheme.typography.bodyMedium
                )

                ObservationPhotoPreview(
                    photoPath = photoPath,
                    allowFullScreen = false
                )

                OutlinedButton(
                    onClick = {
                        currentOnPhotoChanged(null)
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Прибрати фотографію")
                }
            }

            OutlinedButton(
                onClick = ::takePhoto,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (photoPath == null) {
                        "Зробити фото"
                    } else {
                        "Зробити нове фото"
                    }
                )
            }

            OutlinedButton(
                onClick = ::selectPhoto,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (photoPath == null) {
                        "Обрати з галереї"
                    } else {
                        "Обрати інше фото"
                    }
                )
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
