package com.alisha2112.vetcare.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.compose.runtime.LaunchedEffect
import com.alisha2112.vetcare.data.photo.ObservationPhotoStorage
import com.alisha2112.vetcare.ui.records.ObservationPhotoPicker
import com.alisha2112.vetcare.ui.records.AddObservationUiState
import com.alisha2112.vetcare.data.model.ObservationType
import com.alisha2112.vetcare.data.model.VetObservation
import com.alisha2112.vetcare.data.model.countByType
import com.alisha2112.vetcare.data.model.formattedValue
import com.alisha2112.vetcare.data.model.sleepQuality
import com.alisha2112.vetcare.data.model.summary
import com.alisha2112.vetcare.data.settings.ThemeMode
import com.alisha2112.vetcare.data.settings.UserSettings
import com.alisha2112.vetcare.ui.permissions.LocationPermissionScreen
import com.alisha2112.vetcare.ui.records.RecordsViewModel
import com.alisha2112.vetcare.ui.weather.WeatherUiState
import com.alisha2112.vetcare.ui.weather.WeatherViewModel
import com.alisha2112.vetcare.ui.ble.BleScreen
import com.alisha2112.vetcare.ui.sensors.BubbleLevelScreen
import com.alisha2112.vetcare.ui.sensors.StepCounterScreen
import com.alisha2112.vetcare.ui.records.ObservationLocationPicker
import com.alisha2112.vetcare.ui.map.ObservationsMapScreen
import com.alisha2112.vetcare.ui.records.ObservationPhotoPreview
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable data object BleRoute
@Serializable data object StepCounterRoute
@Serializable data object BubbleLevelRoute
@Serializable data object LoginRoute
@Serializable data object HomeRoute
@Serializable data object LocationPermissionRoute
@Serializable data object RecordsRoute
@Serializable data object StatisticsRoute
@Serializable data object ProfileRoute
@Serializable
data object ObservationsMapRoute
@Serializable data class DetailsRoute(val recordId: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VetCareApp(
    userSettings: UserSettings,
    onSaveUserName: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val navController = rememberNavController()
    val viewModel: RecordsViewModel = hiltViewModel()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val addState by viewModel.addState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle()
    val filteredRecords by viewModel.filteredRecords.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var role by rememberSaveable { mutableStateOf("Власник тварини") }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val loggedIn = destination?.hasRoute<LoginRoute>() == false

    Scaffold(
        topBar = {
            if (loggedIn) {
                TopAppBar(
                    title = {
                        Text(
                            text = "VetCare",
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    actions = {
                        TextButton(onClick = { showAbout = true }) {
                            Text("Про застосунок")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (loggedIn) {
                NavigationBar {
                    val tabs = listOf(
                        "Головна" to (destination?.hasRoute<HomeRoute>() == true),
                        "Журнал" to (destination?.hasRoute<RecordsRoute>() == true),
                        "Статистика" to (destination?.hasRoute<StatisticsRoute>() == true),
                        "Профіль" to (destination?.hasRoute<ProfileRoute>() == true)
                    )
                    tabs.forEach { (label, selected) ->
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                when (label) {
                                    "Головна" -> navController.popBackStack<HomeRoute>(
                                        inclusive = false
                                    )
                                    "Журнал" -> navController.navigate(RecordsRoute) {
                                        popUpTo<HomeRoute> { inclusive = false }
                                        launchSingleTop = true
                                    }
                                    "Статистика" -> navController.navigate(StatisticsRoute) {
                                        popUpTo<HomeRoute> { inclusive = false }
                                        launchSingleTop = true
                                    }
                                    "Профіль" -> navController.navigate(ProfileRoute) {
                                        popUpTo<HomeRoute> { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = {
                                Text(
                                    when (label) {
                                        "Головна" -> "⌂"
                                        "Журнал" -> "≡"
                                        "Статистика" -> "▤"
                                        else -> "●"
                                    }
                                )
                            },
                            label = { Text(label, maxLines = 1) }
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = LoginRoute,
            modifier = Modifier.padding(padding)
        ) {
            composable<BleRoute> {
                BleScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<StepCounterRoute> {
                StepCounterScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<BubbleLevelRoute> {
                BubbleLevelScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<LoginRoute> {
                LoginContent { selectedRole ->
                    role = selectedRole
                    navController.navigate(HomeRoute) {
                        popUpTo<LoginRoute> { inclusive = true }
                    }
                }
            }

            composable<HomeRoute> {
                val weatherViewModel: WeatherViewModel = hiltViewModel()

                val weatherState by weatherViewModel.uiState
                    .collectAsStateWithLifecycle()

                HomeContent(
                    records = allRecords,
                    userName = userSettings.userName,
                    weatherState = weatherState,
                    onRefreshWeather = weatherViewModel::refresh,
                    onOpenRecords = {
                        navController.navigate(RecordsRoute)
                    },
                    onOpenLocationPermissions = {
                        navController.navigate(LocationPermissionRoute)
                    },
                    onOpenBle = {
                        navController.navigate(BleRoute)
                    },
                    onOpenStepCounter = {
                        navController.navigate(StepCounterRoute)
                    },
                    onOpenBubbleLevel = {
                        navController.navigate(BubbleLevelRoute)
                    }
                )
            }

            composable<LocationPermissionRoute> {
                LocationPermissionScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable<ObservationsMapRoute> {
                ObservationsMapScreen(
                    records = allRecords,
                    onOpenDetails = { recordId ->
                        navController.navigate(
                            DetailsRoute(recordId)
                        )
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable<RecordsRoute> {
                RecordsContent(
                    records = filteredRecords,
                    selectedType = selectedType,
                    searchQuery = searchQuery,
                    onSelectType = viewModel::selectType,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onClearSearch = viewModel::clearSearchQuery,
                    onOpenDetails = {
                        navController.navigate(DetailsRoute(it))
                    },
                    onAdd = viewModel::addRecord,
                    addState = addState,
                    onClearAddResult = viewModel::clearAddResult,
                    isExporting = isExporting,
                    onOpenMap = {
                        navController.navigate(ObservationsMapRoute)
                    },
                    onExport = {
                        viewModel.exportCsv(
                            onReady = { uri ->
                                val sendIntent = Intent(
                                    Intent.ACTION_SEND
                                ).apply {
                                    type = "text/csv"

                                    putExtra(
                                        Intent.EXTRA_STREAM,
                                        uri
                                    )

                                    clipData = ClipData.newRawUri(
                                        "VetCare CSV",
                                        uri
                                    )

                                    addFlags(
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )
                                }

                                try {
                                    val chooser = Intent.createChooser(
                                        sendIntent,
                                        "Поділитися журналом VetCare"
                                    )

                                    chooser.addFlags(
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )

                                    context.startActivity(chooser)

                                } catch (exception: ActivityNotFoundException) {
                                    scope.launch {
                                        snackbar.showSnackbar(
                                            "Немає застосунку для передавання CSV"
                                        )
                                    }
                                }
                            },
                            onError = { message ->
                                scope.launch {
                                    snackbar.showSnackbar(message)
                                }
                            }
                        )
                    }
                )
            }

            composable<StatisticsRoute> {
                StatisticsContent(allRecords)
            }

            composable<ProfileRoute> {
                ProfileContent(
                    role = role,
                    settings = userSettings,
                    onSaveUserName = onSaveUserName,
                    onThemeModeChange = onThemeModeChange,
                    onLogout = {
                        showAbout = false
                        role = "Власник тварини"

                        navController.navigate(LoginRoute) {
                            popUpTo<HomeRoute> {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable<DetailsRoute> { entry ->
                val id = entry.toRoute<DetailsRoute>().recordId

                DetailsContent(
                    record = allRecords.firstOrNull { it.id == id },
                    onBack = {
                        navController.popBackStack()
                    },
                    onShare = { record ->
                        scope.launch {
                            snackbar.showSnackbar(
                                "${record.petName}: ${record.type.title} — ${record.formattedValue()} ${record.type.unit}"
                            )
                        }
                    },
                    onDelete = {
                        viewModel.deleteRecord(id) { deleted ->
                            if (deleted != null) {
                                // Закриваємо деталі лише якщо користувач
                                // ще перебуває на екрані цього запису.
                                val current = navController.currentBackStackEntry
                                if (
                                    current?.destination?.hasRoute<DetailsRoute>() == true &&
                                    current.toRoute<DetailsRoute>().recordId == id
                                ) {
                                    navController.popBackStack()
                                }

                                scope.launch {
                                    // Інакше Snackbar «Запис видалено» стає в чергу
                                    // за поточним і дія «Скасувати» з'являється із запізненням.
                                    snackbar.currentSnackbarData?.dismiss()

                                    val result = snackbar.showSnackbar(
                                        message = "Запис видалено",
                                        actionLabel = "Скасувати",
                                        withDismissAction = true,
                                        duration = SnackbarDuration.Long
                                    )

                                    if (
                                        result ==
                                        androidx.compose.material3.SnackbarResult.ActionPerformed
                                    ) {
                                        viewModel.restoreRecord(deleted)
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = {
                Text("Про VetCare")
            },
            text = {
                Text(
                    "Навчальний прототип АІС «Ветеринарна клініка». " +
                            "Спостереження зберігаються локально на пристрої " +
                            "у базі даних Room. Вхід є демонстраційним " +
                            "і не передбачає реальної авторизації."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text("Зрозуміло")
                }
            }
        )
    }
}

@Composable
private fun LoginContent(
    onLogin: (String) -> Unit
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf("Власник тварини") }
    var error by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "VetCare",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "АІС «Ветеринарна клініка»",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(12.dp))
        Text("Демонстраційний вхід для лабораторної №2")
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                error = false
            },
            label = { Text("Логін") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = error
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                error = false
            },
            label = { Text("Пароль") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            isError = error
        )

        if (error) {
            Text(
                text = "Введіть логін і пароль",
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(12.dp))
        Text("Навчальна роль")

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Власник тварини", "Ветеринар").forEach { option ->
                FilterChip(
                    selected = role == option,
                    onClick = { role = option },
                    label = {
                        Text(
                            if (option == "Власник тварини") {
                                "Власник"
                            } else {
                                "Ветеринар"
                            }
                        )
                    }
                )
            }
        }

        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    error = true
                } else {
                    onLogin(role)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Увійти")
        }

        Text(
            text = "Увага: логін та пароль ніде не перевіряються й не зберігаються.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun HomeContent(
    records: List<VetObservation>,
    userName: String,
    weatherState: WeatherUiState,
    onRefreshWeather: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenLocationPermissions: () -> Unit,
    onOpenBle: () -> Unit,
    onOpenStepCounter: () -> Unit,
    onOpenBubbleLevel: () -> Unit
) {
    var registrations by rememberSaveable {
        mutableIntStateOf(0)
    }

    val recent = records.take(3)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Огляд клініки",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Вітаємо, $userName!",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text("Приклади спостережень за тваринами (не медичні записи)")

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Усього записів: ${records.size}",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "Унікальних тварин: " +
                            records.map { it.petName }.distinct().size
                )

                Spacer(Modifier.height(8.dp))

                Button(onClick = onOpenRecords) {
                    Text("Відкрити журнал")
                }
            }
        }

        WeatherCard(
            state = weatherState,
            onRefresh = onRefreshWeather
        )

        // Практична робота №4 — перевірка дозволів Android.
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Апаратні можливості",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Практична робота №4: " +
                            "перевірка дозволу на геолокацію"
                )

                Button(
                    onClick = onOpenLocationPermissions
                ) {
                    Text("Перевірити дозвіл")
                }

                Button(onClick = onOpenBle) {
                    Text("Bluetooth LE")
                }

                Button(onClick = onOpenStepCounter) {
                    Text("Крокомір")
                }

                Button(onClick = onOpenBubbleLevel) {
                    Text("Бульбашковий рівень")
                }
            }
        }

        Text(
            text = "Останні спостереження",
            style = MaterialTheme.typography.titleMedium
        )

        recent.forEach {
            ObservationCard(
                record = it,
                onClick = onOpenRecords
            )
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Лічильник із лабораторної №1",
                    style = MaterialTheme.typography.titleMedium
                )

                Text("Демонстраційні реєстрації: $registrations")

                Text(
                    if (registrations >= 10) {
                        "Досягнуто 10 реєстрацій!"
                    } else {
                        "До першої цілі: ${10 - registrations}"
                    }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { registrations++ }
                    ) {
                        Text("Додати")
                    }

                    OutlinedButton(
                        onClick = { registrations = 0 },
                        enabled = registrations > 0
                    ) {
                        Text("Скинути")
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherCard(
    state: WeatherUiState,
    onRefresh: () -> Unit
) {
    val locale = Locale.forLanguageTag("uk")

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Погода в Києві",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Демонстраційне місто",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when (state) {
                WeatherUiState.Loading -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Завантажуємо погоду...")
                    }
                }

                is WeatherUiState.Success -> {
                    val report = state.report
                    val weather = report.current

                    if (state.isStale) {
                        Text(
                            text = "Дані застаріли",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error
                        )

                        Text(
                            text = "Показано останню збережену погоду.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (state.isRefreshing) {
                        Text(
                            text = "Перевіряємо оновлення погоди...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (state.warning != null) {
                        Text(
                            text = state.warning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Text(
                        text = weather.description,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = String.format(
                            locale,
                            "%.1f °C",
                            weather.temperatureCelsius
                        ),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Відчувається як: " +
                                String.format(
                                    locale,
                                    "%.1f °C",
                                    weather.feelsLikeCelsius
                                )
                    )

                    Text(
                        text = "Вологість: ${weather.humidityPercent}%"
                    )

                    Text(
                        text = "Вітер: " +
                                String.format(
                                    locale,
                                    "%.1f км/год",
                                    weather.windSpeedKmh
                                )
                    )

                    Text(
                        text = "Станом на: " +
                                weather.observedAt.replace('T', ' ') +
                                " (час Києва)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Прогноз на 3 дні",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        report.forecast.forEach { day ->
                            val dateLabel = LocalDate
                                .parse(day.date)
                                .format(
                                    DateTimeFormatter.ofPattern("dd.MM")
                                )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = dateLabel,
                                    style = MaterialTheme.typography.labelMedium
                                )

                                Text(
                                    text = String.format(
                                        locale,
                                        "↑ %.1f°",
                                        day.maxTemperatureCelsius
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    text = String.format(
                                        locale,
                                        "↓ %.1f°",
                                        day.minTemperatureCelsius
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    val savedAt = Instant
                        .ofEpochMilli(report.fetchedAtMillis)
                        .atZone(ZoneId.of("Europe/Kyiv"))
                        .format(
                            DateTimeFormatter.ofPattern(
                                "dd.MM.yyyy HH:mm"
                            )
                        )

                    Text(
                        text = "Останнє успішне оновлення: $savedAt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is WeatherUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            val canRefresh = when (state) {
                WeatherUiState.Loading -> false
                is WeatherUiState.Success -> !state.isRefreshing
                is WeatherUiState.Error -> true
            }

            OutlinedButton(
                onClick = onRefresh,
                enabled = canRefresh
            ) {
                Text(
                    if (state is WeatherUiState.Error) {
                        "Повторити"
                    } else {
                        "Оновити"
                    }
                )
            }

            Text(
                text = "Джерело: Open-Meteo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ObservationCard(
    record: VetObservation,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = "${record.petName} · ${record.type.title}",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "${record.formattedValue()} ${record.type.unit} · ${record.recordedAt}"
            )

            if (record.note.isNotBlank()) {
                Text(
                    text = record.note,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun RecordsContent(
    records: List<VetObservation>,
    selectedType: ObservationType?,
    searchQuery: String,
    onSelectType: (ObservationType?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onOpenDetails: (Long) -> Unit,
    onOpenMap: () -> Unit,
    onAdd: (
        String,
        ObservationType,
        Double,
        String,
        Double?,
        Double?,
        String?
    ) -> Unit,
    addState: AddObservationUiState,
    onClearAddResult: () -> Unit,
    isExporting: Boolean,
    onExport: () -> Unit
) {
    var showAdd by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(addState.saved) {
        if (addState.saved) {
            showAdd = false
            onClearAddResult()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Журнал спостережень",
                style = MaterialTheme.typography.titleLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onExport,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    enabled = !isExporting
                ) {
                    Text(
                        if (isExporting) {
                            "Експорт..."
                        } else {
                            "Експорт CSV"
                        }
                    )
                }

                Button(
                    onClick = { showAdd = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("+ Новий")
                }
            }
            OutlinedButton(
                onClick = onOpenMap,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Карта спостережень")
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = {
                Text("Пошук за приміткою")
            },
            placeholder = {
                Text("Наприклад: сон")
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 4.dp
                ),
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    TextButton(
                        onClick = onClearSearch
                    ) {
                        Text("✕")
                    }
                }
            }
        )

        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(
                horizontal = 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedType == null,
                    onClick = {
                        onSelectType(null)
                    },
                    label = {
                        Text("Усі")
                    }
                )
            }

            items(ObservationType.entries) { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = {
                        onSelectType(type)
                    },
                    label = {
                        Text(type.title)
                    }
                )
            }
        }

        Text(
            text = "Знайдено записів: ${records.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 4.dp
            )
        )

        if (records.isEmpty()) {
            Text(
                text = "За вашим запитом записів не знайдено",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = records,
                key = { it.id }
            ) { record ->
                ObservationCard(
                    record = record,
                    onClick = {
                        onOpenDetails(record.id)
                    }
                )
            }
        }
    }

    if (showAdd) {
        AddObservationDialog(
            addState = addState,
            onDismiss = {
                showAdd = false
                onClearAddResult()
            },
            onSave = onAdd
        )
    }
}

@Composable
private fun AddObservationDialog(
    addState: AddObservationUiState,
    onDismiss: () -> Unit,
    onSave: (
        String,
        ObservationType,
        Double,
        String,
        Double?,
        Double?,
        String?
    ) -> Unit
) {
    val context = LocalContext.current

    val photoStorage = remember(context) {
        ObservationPhotoStorage(
            context.applicationContext
        )
    }

    var petName by rememberSaveable {
        mutableStateOf("")
    }

    var selectedTypeName by rememberSaveable {
        mutableStateOf(
            ObservationType.TEMPERATURE.name
        )
    }

    var value by rememberSaveable {
        mutableStateOf("")
    }

    var note by rememberSaveable {
        mutableStateOf("")
    }

    // GPS-координати спостереження.
    var latitude by rememberSaveable {
        mutableStateOf<Double?>(null)
    }

    var longitude by rememberSaveable {
        mutableStateOf<Double?>(null)
    }

    var locationLoading by remember {
        mutableStateOf(false)
    }

    // Фотографія поточної чернетки.
    var photoPath by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var photoBusy by rememberSaveable {
        mutableStateOf(false)
    }

    var photoError by remember {
        mutableStateOf<String?>(null)
    }

    val type = ObservationType.valueOf(
        selectedTypeName
    )

    val numeric = value
        .replace(',', '.')
        .toDoubleOrNull()

    val valid =
        petName.isNotBlank() &&
                numeric != null &&
                numeric.isFinite() &&
                numeric > 0

    val saving =
        addState.isSaving || addState.saved

    val busy =
        locationLoading ||
                photoBusy ||
                saving

    // Заміна або прибирання фото чернетки.
    fun changePhoto(newPath: String?) {
        val previousPath = photoPath

        photoPath = newPath
        photoError = null

        if (
            previousPath != null &&
            previousPath != newPath
        ) {
            photoStorage.deletePhoto(previousPath)
        }
    }

    // Скасування створення спостереження.
    fun discardDraft() {
        if (busy) return

        photoStorage.deletePhoto(photoPath)
        photoPath = null

        onDismiss()
    }

    AlertDialog(
        onDismissRequest = ::discardDraft,

        title = {
            Text("Нове спостереження")
        },

        text = {
            if (saving) {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()

                    Text("Збереження спостереження...")
                }
            } else {
                Column(
                    modifier = Modifier
                        .verticalScroll(
                            rememberScrollState()
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = petName,
                        onValueChange = {
                            petName = it
                        },
                        label = {
                            Text("Кличка тварини")
                        },
                        singleLine = true
                    )

                    Text("Тип спостереження")

                    ObservationType.entries.filter {
                        it.manualEntry
                    }.forEach {
                            option ->

                        FilterChip(
                            selected = type == option,
                            onClick = {
                                selectedTypeName =
                                    option.name
                            },
                            label = {
                                Text(option.title)
                            }
                        )
                    }

                    OutlinedTextField(
                        value = value,
                        onValueChange = {
                            value = it
                        },
                        label = {
                            Text(
                                "Значення (${type.unit})"
                            )
                        },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = {
                            note = it
                        },
                        label = {
                            Text("Примітка")
                        }
                    )

                    // Наявна геолокація.
                    ObservationLocationPicker(
                        latitude = latitude,
                        longitude = longitude,
                        onLocationChanged = { lat, lon ->
                            latitude = lat
                            longitude = lon
                        },
                        onLoadingChanged = { loading ->
                            locationLoading = loading
                        }
                    )

                    // Новий компонент фотографій.
                    ObservationPhotoPicker(
                        photoPath = photoPath,
                        onPhotoChanged = ::changePhoto,
                        onBusyChanged = { loading ->
                            photoBusy = loading
                        }
                    )

                    photoError?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme
                                .colorScheme.error,
                            style = MaterialTheme
                                .typography.bodySmall
                        )
                    }

                    addState.error?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme
                                .colorScheme.error,
                            style = MaterialTheme
                                .typography.bodySmall
                        )
                    }
                }
            }
        },

        confirmButton = {
            TextButton(
                onClick = {
                    if (valid && !busy && numeric != null) {
                        if (
                            photoPath != null &&
                            !photoStorage.isPhotoReady(
                                photoPath
                            )
                        ) {
                            photoError =
                                "Фотографія недоступна. " +
                                        "Оберіть її повторно " +
                                        "або приберіть."

                        } else {
                            photoError = null

                            onSave(
                                petName,
                                type,
                                numeric,
                                note,
                                latitude,
                                longitude,
                                photoPath
                            )
                        }
                    }
                },
                enabled = valid && !busy
            ) {
                Text(
                    if (saving) {
                        "Збереження..."
                    } else {
                        "Зберегти"
                    }
                )
            }
        },

        dismissButton = {
            TextButton(
                onClick = ::discardDraft,
                enabled = !busy
            ) {
                Text("Скасувати")
            }
        }
    )
}

@Composable
private fun DetailsContent(
    record: VetObservation?,
    onBack: () -> Unit,
    onShare: (VetObservation) -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by rememberSaveable {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Назад")
        }

        Text(
            text = "Деталі запису",
            style = MaterialTheme.typography.headlineMedium
        )


        if (record == null) {
            Text("Запис не знайдено або видалено")
        } else {
            Text("Тварина: ${record.petName}")
            Text("Тип: ${record.type.title}")
            Text("Значення: ${record.formattedValue()} ${record.type.unit}")
            Text("Дата: ${record.recordedAt}")
            Text("Примітка: ${record.note.ifBlank { "Не вказано" }}")

            Text(
                text = "Місцезнаходження",
                style = MaterialTheme.typography.titleMedium
            )

            val latitude = record.latitude
            val longitude = record.longitude

            if (latitude != null && longitude != null) {
                Text(
                    text = String.format(
                        Locale.US,
                        "Широта: %.6f",
                        latitude
                    )
                )

                Text(
                    text = String.format(
                        Locale.US,
                        "Довгота: %.6f",
                        longitude
                    )
                )
            } else {
                Text(
                    text = "Місцезнаходження не прикріплено",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Фотографія",
                style = MaterialTheme.typography.titleMedium
            )

            val photoPath = record.photoPath

            if (photoPath != null) {
                ObservationPhotoPreview(
                    photoPath = photoPath
                )
            } else {
                Text(
                    text = "Фотографію не прикріплено",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (record.type == ObservationType.SLEEP) {
                Text(
                    text = "Оцінка сну: ${sleepQuality(record.value)}"
                )

                Text(
                    text = "Оцінка є навчальною, не ветеринарним висновком.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = { onShare(record) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Поділитися (Snackbar)")
            }

            OutlinedButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Видалити запис")
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = {
                confirmDelete = false
            },
            title = {
                Text("Видалити запис?")
            },
            text = {
                Text(
                    "Після видалення можна одразу скасувати дію через Snackbar."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    }
                ) {
                    Text("Видалити")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                    }
                ) {
                    Text("Скасувати")
                }
            }
        )
    }
}

@Composable
private fun StatisticsContent(
    records: List<VetObservation>
) {
    val summary = records.summary()
    val counts = records.countByType()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Статистика спостережень",
            style = MaterialTheme.typography.headlineMedium
        )

        Text("Загальна кількість: ${summary.observationCount}")

        counts.forEach { (type, count) ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = type.title,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }

        Text(
            text = "Демонстраційні підсумки",
            style = MaterialTheme.typography.titleMedium
        )

        Text("Корм: ${summary.totalFeedGrams} г")

        Text(
            "Середній пульс за записами: " +
                    "${summary.averagePulse?.toInt() ?: "—"} уд/хв"
        )

        Text(
            "Остання тривалість сну: " +
                    "${summary.latestSleepHours ?: "—"} год"
        )

        Text("Кроки за записами крокоміра: ${summary.totalSteps}")

        Text(
            text = "Дані різних тварин об’єднані лише для демонстрації операцій Kotlin; " +
                    "ці підсумки не можна використовувати як медичні показники.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ProfileContent(
    role: String,
    settings: UserSettings,
    onSaveUserName: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLogout: () -> Unit
) {
    var nameDraft by rememberSaveable(settings.userName) {
        mutableStateOf(
            if (settings.userName == "Користувач") {
                ""
            } else {
                settings.userName
            }
        )
    }

    val normalizedName = nameDraft.trim()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Профіль",
            style = MaterialTheme.typography.headlineMedium
        )

        Text("Обрана демонстраційна роль: $role")

        Text(
            text = "Налаштування зберігаються локально на пристрої.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Особисті налаштування",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = nameDraft,
            onValueChange = {
                nameDraft = it
            },
            label = {
                Text("Ваше ім'я")
            },
            placeholder = {
                Text("Введіть ваше ім'я")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                onSaveUserName(normalizedName)
            },
            enabled = normalizedName.isNotEmpty() &&
                    normalizedName != settings.userName,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Зберегти ім'я")
        }

        Text(
            text = "Збережене ім'я: ${settings.userName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Тема застосунку",
            style = MaterialTheme.typography.titleMedium
        )

        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ThemeMode.entries) { mode ->
                FilterChip(
                    selected = settings.themeMode == mode,
                    onClick = {
                        onThemeModeChange(mode)
                    },
                    label = {
                        Text(mode.title)
                    }
                )
            }
        }

        Text(
            text = "Режим «Як у системі» автоматично " +
                    "використовує тему, вибрану в Android.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Реєстрація користувачів і серверна " +
                    "авторизація будуть додані на подальших етапах.",
            style = MaterialTheme.typography.bodySmall
        )

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Вийти з профілю")
        }
    }
}
