package com.alisha2112.vetcare.ui.map

import com.alisha2112.vetcare.location.routeLengthMeters
import com.alisha2112.vetcare.location.toRoutePoints
import android.location.Location
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alisha2112.vetcare.data.model.VetObservation
import com.alisha2112.vetcare.data.model.formattedValue
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val observationDateFormatter =
    DateTimeFormatter.ofPattern("dd.MM.uuuu")

private fun parseObservationDate(
    date: String
): LocalDate? =
    runCatching {
        LocalDate.parse(
            date,
            observationDateFormatter
        )
    }.getOrNull()

@Composable
fun ObservationsMapScreen(
    records: List<VetObservation>,
    onOpenDetails: (Long) -> Unit,
    onBack: () -> Unit
) {
    var todayOnly by rememberSaveable {
        mutableStateOf(true)
    }

    var selectedRecordIds by remember {
        mutableStateOf<List<Long>>(emptyList())
    }

    val today = LocalDate.now()

    // Залишаємо тільки записи з коректними координатами.
    val locatedRecords = remember(records) {
        records.filter { record ->
            val lat = record.latitude
            val lon = record.longitude

            lat != null &&
                lon != null &&
                lat in -90.0..90.0 &&
                lon in -180.0..180.0
        }.sortedWith(
            compareBy<VetObservation> {
                parseObservationDate(it.recordedAt)
                    ?: LocalDate.MIN
            }.thenBy { it.id }
        )
    }

    // Фільтрація за поточним днем.
    val visibleRecords = remember(
        locatedRecords,
        todayOnly,
        today
    ) {
        if (todayOnly) {
            locatedRecords.filter {
                parseObservationDate(it.recordedAt) == today
            }
        } else {
            locatedRecords
        }
    }

    // Перетворення записів у точки для RouteMap.
    val routePoints = remember(visibleRecords) {
        visibleRecords.map { record ->
            Location("observation").apply {
                latitude = requireNotNull(record.latitude)
                longitude = requireNotNull(record.longitude)
            }
        }
    }

    // Обчислення загальної довжини лінії маршруту.
    val routeDistanceMeters = remember(routePoints) {
        routeLengthMeters(routePoints.toRoutePoints())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← До журналу")
        }

        Text(
            text = "Карта спостережень",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Місця, у яких були зафіксовані " +
                "спостереження за тваринами."
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = todayOnly,
                onClick = {
                    todayOnly = true
                    selectedRecordIds = emptyList()
                },
                label = { Text("Сьогодні") }
            )

            FilterChip(
                selected = !todayOnly,
                onClick = {
                    todayOnly = false
                    selectedRecordIds = emptyList()
                },
                label = { Text("Усі записи") }
            )
        }

        Text(
            text = "Записів із координатами: " +
                visibleRecords.size
        )

        if (visibleRecords.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Немає спостережень " +
                            "із місцезнаходженням",
                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Створіть запис у журналі " +
                            "та прикріпіть GPS-координати.",
                        style =
                            MaterialTheme.typography.bodyMedium
                    )

                    Button(onClick = onBack) {
                        Text("Перейти до журналу")
                    }
                }
            }
        } else {
            Text(
                text = String.format(
                    Locale.US,
                    "Довжина маршруту: %.1f м",
                    routeDistanceMeters
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            RouteMap(
                routePoints = routePoints,
                modifier = Modifier.fillMaxWidth(),
                onMarkerClick = { indices ->
                    selectedRecordIds = indices.mapNotNull {
                        visibleRecords.getOrNull(it)?.id
                    }
                }
            )

            Text(
                text = "Натисніть на маркер, щоб " +
                    "переглянути відповідне спостереження.",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Спостереження на карті",
                style = MaterialTheme.typography.titleMedium
            )

            visibleRecords.forEachIndexed { index, record ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onOpenDetails(record.id)
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${index + 1}. " +
                                record.petName,
                            style =
                                MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "${record.type.title}: " +
                                "${record.formattedValue()} ${record.type.unit}"
                        )

                        Text("Дата: ${record.recordedAt}")

                        Text(
                            text = String.format(
                                Locale.US,
                                "%.6f, %.6f",
                                record.latitude,
                                record.longitude
                            ),
                            style =
                                MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text = "Переглянути деталі →",
                            color =
                                MaterialTheme.colorScheme.primary,
                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Text(
                text = "Лінія з'єднує місця спостережень " +
                    "у порядку їх створення. Вона не є " +
                    "точним GPS-треком переміщення.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    val selectedRecords = visibleRecords.filter {
        it.id in selectedRecordIds
    }

    if (selectedRecords.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {
                selectedRecordIds = emptyList()
            },
            title = {
                Text("Спостереження на карті")
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(
                        rememberScrollState()
                    ),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    selectedRecords.forEach { record ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier =
                                    Modifier.padding(12.dp),
                                verticalArrangement =
                                    Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = record.petName,
                                    style = MaterialTheme
                                        .typography.titleMedium
                                )

                                Text(
                                    text =
                                        "${record.type.title}: " +
                                        "${record.formattedValue()} " +
                                        record.type.unit
                                )

                                Text(
                                    "Дата: ${record.recordedAt}"
                                )

                                if (record.note.isNotBlank()) {
                                    Text(
                                        "Примітка: ${record.note}"
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        selectedRecordIds =
                                            emptyList()

                                        onOpenDetails(record.id)
                                    }
                                ) {
                                    Text("Відкрити запис")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedRecordIds = emptyList()
                    }
                ) {
                    Text("Закрити")
                }
            }
        )
    }
}
