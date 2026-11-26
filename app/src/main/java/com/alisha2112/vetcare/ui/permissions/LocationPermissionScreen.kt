package com.alisha2112.vetcare.ui.permissions

import com.alisha2112.vetcare.location.routeLengthMeters
import com.alisha2112.vetcare.location.toRoutePoints
import android.Manifest
import android.location.Location
import android.os.CancellationSignal
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alisha2112.vetcare.location.DailyRouteStore
import com.alisha2112.vetcare.location.LocationReader
import com.alisha2112.vetcare.ui.components.PermissionCard
import com.alisha2112.vetcare.ui.components.rememberPermissionsState
import com.alisha2112.vetcare.ui.map.RouteMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LocationPermissionScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val reader = remember(context) {
        LocationReader(context.applicationContext)
    }

    val routeStore = remember(context) {
        DailyRouteStore(context.applicationContext)
    }

    val permissionsState = rememberPermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ),
        requireAll = false
    )

    // Поточний календарний день.
    var today by remember {
        mutableStateOf(LocalDate.now().toString())
    }

    LifecycleResumeEffect(Unit) {
        today = LocalDate.now().toString()

        onPauseOrDispose { }
    }

    // Отримання маршруту з DataStore.
    val routeResult by routeStore.route
        .collectAsStateWithLifecycle(initialValue = null)

    val activeRoute = routeResult
        ?.getOrNull()
        ?.takeIf { it.date == today }

    val routePoints = activeRoute?.points.orEmpty()
    val startLocation = routePoints.firstOrNull()
    val currentLocation = activeRoute?.currentLocation

    val routeReadError =
        routeResult?.isFailure == true

    var loading by remember {
        mutableStateOf(false)
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    var pendingRequest by remember {
        mutableStateOf<CancellationSignal?>(null)
    }

    // Ідентифікатор запиту захищає від
    // обробки результатів скасованих вимірювань.
    var requestGeneration by remember {
        mutableStateOf(0L)
    }

    fun cancelRequest() {
        requestGeneration++
        pendingRequest?.cancel()
        pendingRequest = null
        loading = false
    }

    fun measureLocation() {
        if (isSaving) return

        cancelRequest()

        today = LocalDate.now().toString()
        error = null
        loading = true

        val generation = requestGeneration

        pendingRequest = reader.getCurrentLocation(
            onSuccess = { location ->
                if (generation == requestGeneration) {
                    loading = false
                    pendingRequest = null
                    isSaving = true

                    today = LocalDate.now().toString()

                    scope.launch {
                        try {
                            routeStore.recordPoint(location)
                        } catch (
                            exception: CancellationException
                        ) {
                            throw exception
                        } catch (exception: Exception) {
                            error =
                                "Не вдалося зберегти GPS-точку"
                        } finally {
                            isSaving = false
                        }
                    }
                }
            },
            onError = { message ->
                if (generation == requestGeneration) {
                    error = message
                    loading = false
                    pendingRequest = null
                }
            }
        )
    }

    fun resetRoute() {
        if (isSaving) return

        cancelRequest()
        error = null
        isSaving = true

        today = LocalDate.now().toString()

        scope.launch {
            try {
                routeStore.clearRoute()
            } catch (
                exception: CancellationException
            ) {
                throw exception
            } catch (exception: Exception) {
                error = "Не вдалося очистити маршрут"
            } finally {
                isSaving = false
            }
        }
    }

    LaunchedEffect(permissionsState.granted) {
        if (!permissionsState.granted) {
            cancelRequest()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            requestGeneration++
            pendingRequest?.cancel()
        }
    }

    // Відстань від першої точки до поточної.
    val distanceMeters = remember(
        startLocation,
        currentLocation
    ) {
        if (startLocation == null ||
            currentLocation == null
        ) {
            null
        } else {
            val results = FloatArray(1)

            Location.distanceBetween(
                startLocation.latitude,
                startLocation.longitude,
                currentLocation.latitude,
                currentLocation.longitude,
                results
            )

            results[0]
        }
    }

    // Сумарна довжина пройденого маршруту.
    val routeDistanceMeters = remember(routePoints) {
        routeLengthMeters(routePoints.toRoutePoints())
    }

    val formattedDate = remember(today) {
        LocalDate.parse(today).format(
            DateTimeFormatter.ofPattern("dd.MM.yyyy")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Геолокація",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Визначення координат пристрою " +
                    "та обчислення відстані від початкової точки."
        )

        if (!permissionsState.granted) {
            PermissionCard(
                title = "Потрібен дозвіл",
                description = "Надайте доступ до " +
                        "місцезнаходження для вимірювання " +
                        "координат і маршруту.",
                state = permissionsState
            )
        } else {
            Text(
                text = "Доступ до геолокації надано.",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Маршрут дня: $formattedDate",
                style = MaterialTheme.typography.titleMedium
            )

            when {
                routeResult == null -> {
                    CircularProgressIndicator()

                    Text("Відновлення маршруту...")
                }

                routeReadError -> {
                    Text(
                        text = "Не вдалося прочитати маршрут",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                loading -> {
                    CircularProgressIndicator()

                    Text("Визначення координат...")

                    OutlinedButton(
                        onClick = ::cancelRequest
                    ) {
                        Text("Скасувати пошук")
                    }
                }

                isSaving -> {
                    CircularProgressIndicator()

                    Text("Збереження маршруту...")
                }

                else -> {
                    Button(
                        onClick = ::measureLocation,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (startLocation == null) {
                                "Зафіксувати початкову точку"
                            } else {
                                "Оновити координати"
                            }
                        )
                    }
                }
            }

            error?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            currentLocation?.let { location ->
                Text(
                    text = "Поточні координати",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    String.format(
                        Locale.US,
                        "Широта: %.6f",
                        location.latitude
                    )
                )

                Text(
                    String.format(
                        Locale.US,
                        "Довгота: %.6f",
                        location.longitude
                    )
                )

                Text(
                    String.format(
                        Locale.US,
                        "Точність: %.1f м",
                        location.accuracy
                    )
                )
            }

            startLocation?.let { start ->
                Text(
                    text = "Початкова точка",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    String.format(
                        Locale.US,
                        "%.6f, %.6f",
                        start.latitude,
                        start.longitude
                    )
                )
            }

            distanceMeters?.let { distance ->
                Text(
                    text = String.format(
                        Locale.US,
                        "Відстань від початку: %.1f м",
                        distance
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (routePoints.isNotEmpty()) {
                Text(
                    text = "Маршрут",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Кількість точок: ${routePoints.size}"
                )

                Text(
                    text = String.format(
                        Locale.US,
                        "Загальна довжина маршруту: %.1f м",
                        routeDistanceMeters
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Зафіксовані GPS-точки",
                    style = MaterialTheme.typography.titleSmall
                )

                routePoints.forEachIndexed { index, point ->
                    Text(
                        text = String.format(
                            Locale.US,
                            "%d. %.6f, %.6f",
                            index + 1,
                            point.latitude,
                            point.longitude
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                RouteMap(
                    routePoints = routePoints,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Маршрут збережено на пристрої.",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (routePoints.isNotEmpty() || routeReadError) {
                OutlinedButton(
                    onClick = ::resetRoute,
                    enabled = !isSaving
                ) {
                    Text("Скинути вимірювання")
                }
            }

            Text(
                text = "GPS-точки зберігаються автоматично " +
                        "після кожного успішного вимірювання. " +
                        "Маршрут поточного дня відновлюється " +
                        "після повторного відкриття застосунку.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = onBack,
            enabled = !isSaving
        ) {
            Text("Назад")
        }
    }
}
