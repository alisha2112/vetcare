package com.alisha2112.vetcare.ui.records

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.alisha2112.vetcare.data.photo.ObservationPhotoStorage
import java.io.File

@Composable
fun ObservationPhotoPreview(
    photoPath: String,
    modifier: Modifier = Modifier,
    allowFullScreen: Boolean = true
) {
    val context = LocalContext.current

    val storage = remember(context) {
        ObservationPhotoStorage(
            context.applicationContext
        )
    }

    val photoFile = remember(storage, photoPath) {
        storage.findPhoto(photoPath)
    }

    var showFullScreen by rememberSaveable(photoPath) {
        mutableStateOf(false)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
        if (photoFile == null) {
            Text(
                text = "Фотографія недоступна",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            PhotoImage(
                file = photoFile,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )

            if (allowFullScreen) {
                OutlinedButton(
                    onClick = {
                        showFullScreen = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Переглянути фотографію")
                }
            }
        }
    }

    if (
        allowFullScreen &&
        showFullScreen &&
        photoFile != null
    ) {
        AlertDialog(
            onDismissRequest = {
                showFullScreen = false
            },
            title = {
                Text("Фотографія спостереження")
            },
            text = {
                PhotoImage(
                    file = photoFile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFullScreen = false
                    }
                ) {
                    Text("Закрити")
                }
            }
        )
    }
}

@Composable
private fun PhotoImage(
    file: File,
    modifier: Modifier = Modifier
) {
    SubcomposeAsyncImage(
        model = file,
        contentDescription =
            "Фотографія спостереження за твариною",
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                MaterialTheme.colorScheme.surfaceVariant
            ),
        contentScale = ContentScale.Fit,

        loading = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        },

        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Не вдалося відобразити фотографію",
                    color =
                        MaterialTheme.colorScheme.error,
                    style =
                        MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}
