package com.alisha2112.vetcare.ui.sensors

import android.view.Surface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alisha2112.vetcare.sensors.BubbleLevelMath
import com.alisha2112.vetcare.sensors.LevelReading
import java.util.Locale

@Composable
fun BubbleLevelScreen(
    onBack: () -> Unit,
    viewModel: BubbleLevelViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Акселерометр працює лише поки екран у стані RESUMED.
    LifecycleResumeEffect(viewModel) {
        viewModel.start()
        onPauseOrDispose { viewModel.stop() }
    }

    // Поворот екрана: осі сенсора перетворюються в осі дисплея.
    val configuration = LocalConfiguration.current
    val view = LocalView.current
    val rotationDegrees = remember(configuration, view) {
        when (view.display?.rotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
    }

    val reading = state.sample?.let {
        BubbleLevelMath.reading(it, rotationDegrees)
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
            text = "Бульбашковий рівень",
            style = MaterialTheme.typography.headlineMedium
        )

        when {
            !state.sensorAvailable -> MessageCard(
                "Акселерометр відсутній на цьому пристрої. " +
                        "Бульбашковий рівень недоступний."
            )

            state.error != null -> MessageCard("Помилка: ${state.error}")

            else -> {
                LevelIndicator(
                    reading = reading,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(max = 320.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )

                LevelValues(reading)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Як користуватися",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Покладіть пристрій екраном догори на поверхню. " +
                            "Бульбашка зміщується до піднятого краю; коли вона " +
                            "в центральному колі, поверхня приблизно горизонтальна.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Навчальна реалізація на основі акселерометра " +
                            "без калібрування — не є професійним вимірювальним " +
                            "інструментом.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun MessageCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun LevelIndicator(
    reading: LevelReading?,
    modifier: Modifier = Modifier
) {
    val outline = MaterialTheme.colorScheme.outline
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val levelColor = MaterialTheme.colorScheme.tertiary
    val bubbleColor = if (reading?.isLevel == true) levelColor else primary

    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val bubbleRadius = radius * 0.14f
        val centerZoneRadius = bubbleRadius * 1.6f
        val strokeWidth = 2.dp.toPx()

        drawCircle(color = surface, radius = radius, center = center)
        drawCircle(
            color = outline,
            radius = radius - strokeWidth / 2f,
            center = center,
            style = Stroke(strokeWidth)
        )

        // Горизонтальна та вертикальна осі.
        drawLine(
            color = outline,
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = strokeWidth / 2f
        )
        drawLine(
            color = outline,
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = strokeWidth / 2f
        )

        // Центральна контрольна область.
        drawCircle(
            color = if (reading?.isLevel == true) levelColor else outline,
            radius = centerZoneRadius,
            center = center,
            style = Stroke(strokeWidth)
        )

        if (reading != null) {
            // Бульбашка не виходить за межі індикатора.
            val travel = radius - bubbleRadius - strokeWidth
            val bubbleCenter = Offset(
                x = center.x + reading.bubbleX * travel,
                y = center.y + reading.bubbleY * travel
            )

            drawCircle(
                color = bubbleColor.copy(alpha = 0.85f),
                radius = bubbleRadius,
                center = bubbleCenter
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = bubbleRadius * 0.35f,
                center = bubbleCenter + Offset(-bubbleRadius * 0.3f, -bubbleRadius * 0.3f)
            )
        }
    }
}

@Composable
private fun LevelValues(reading: LevelReading?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (reading == null) {
                Text("Очікування показників акселерометра…")
                return@Column
            }

            Text(
                text = "Нахил X: ${formatAngle(reading.tiltXDegrees)}°",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Нахил Y: ${formatAngle(reading.tiltYDegrees)}°",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (reading.isLevel) {
                    "Пристрій розташований горизонтально"
                } else {
                    "Пристрій нахилено"
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                color = if (reading.isLevel) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

private fun formatAngle(value: Float): String =
    String.format(Locale.US, "%.1f", value)
