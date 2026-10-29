package com.alisha2112.vetcare.ui

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.alisha2112.vetcare.data.model.ObservationType
import com.alisha2112.vetcare.data.model.VetObservation
import com.alisha2112.vetcare.data.model.countByType
import com.alisha2112.vetcare.data.model.sleepQuality
import com.alisha2112.vetcare.data.model.summary
import com.alisha2112.vetcare.ui.records.RecordsViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import com.alisha2112.vetcare.data.settings.ThemeMode
import com.alisha2112.vetcare.data.settings.UserSettings

@Serializable data object LoginRoute
@Serializable data object HomeRoute
@Serializable data object RecordsRoute
@Serializable data object StatisticsRoute
@Serializable data object ProfileRoute
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
                                    // Home is already in the back stack after demo sign-in.
                                    // Pop to it rather than navigating to (and restoring) a saved entry.
                                    "Головна" -> navController.popBackStack<HomeRoute>(inclusive = false)
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
                            icon = { Text(when (label) {
                                "Головна" -> "⌂"
                                "Журнал" -> "≡"
                                "Статистика" -> "▤"
                                else -> "●"
                            }) },
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
            composable<LoginRoute> {
                LoginContent { selectedRole ->
                    role = selectedRole
                    navController.navigate(HomeRoute) {
                        popUpTo<LoginRoute> { inclusive = true }
                    }
                }
            }

            composable<HomeRoute> {
                HomeContent(
                    records = allRecords,
                    userName = userSettings.userName,
                    onOpenRecords = {
                        navController.navigate(RecordsRoute)
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
                    onAdd = viewModel::addRecord
                )
            }

            composable<StatisticsRoute> { StatisticsContent(allRecords) }

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
                    onBack = { navController.popBackStack() },
                    onShare = { record ->
                        scope.launch { snackbar.showSnackbar(
                            "${record.petName}: ${record.type.title} — ${record.value} ${record.type.unit}"
                        ) }
                    },
                    onDelete = {
                        viewModel.deleteRecord(id) { deleted ->
                            if (deleted != null) {
                                navController.popBackStack()
                                scope.launch {
                                    val result = snackbar.showSnackbar(
                                        message = "Запис видалено",
                                        actionLabel = "Скасувати",
                                        withDismissAction = true,
                                        duration = SnackbarDuration.Long
                                    )
                                    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
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
            title = { Text("Про VetCare") },
            text = {
                Text("Навчальний прототип АІС «Ветеринарна клініка». " +
                        "Спостереження зберігаються локально на пристрої " +
                        "у базі даних Room. Вхід є демонстраційним " +
                        "і не передбачає реальної авторизації.")
            },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Зрозуміло") } }
        )
    }
}

@Composable
private fun LoginContent(onLogin: (String) -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf("Власник тварини") }
    var error by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("VetCare", style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary)
        Text("АІС «Ветеринарна клініка»", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Text("Демонстраційний вхід для лабораторної №2")
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = username, onValueChange = { username = it; error = false },
            label = { Text("Логін") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            isError = error
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it; error = false },
            label = { Text("Пароль") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            visualTransformation = PasswordVisualTransformation(), isError = error
        )
        if (error) Text("Введіть логін і пароль", color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Text("Навчальна роль")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Власник тварини", "Ветеринар").forEach { option ->
                FilterChip(selected = role == option, onClick = { role = option },
                    label = { Text(if (option == "Власник тварини") "Власник" else "Ветеринар") })
            }
        }
        Button(
            onClick = { if (username.isBlank() || password.isBlank()) error = true else onLogin(role) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Увійти") }
        Text("Увага: логін та пароль ніде не перевіряються й не зберігаються.",
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HomeContent(
    records: List<VetObservation>,
    userName: String,
    onOpenRecords: () -> Unit
) {
    var registrations by rememberSaveable { mutableIntStateOf(0) }
    val recent = records.take(3)
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Огляд клініки", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Вітаємо, $userName!",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text("Приклади спостережень за тваринами (не медичні записи)")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Усього записів: ${records.size}", style = MaterialTheme.typography.titleLarge)
                Text("Унікальних тварин: ${records.map { it.petName }.distinct().size}")
                Spacer(Modifier.height(8.dp))
                Button(onClick = onOpenRecords) { Text("Відкрити журнал") }
            }
        }
        Text("Останні спостереження", style = MaterialTheme.typography.titleMedium)
        recent.forEach { ObservationCard(it, onClick = onOpenRecords) }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Лічильник із лабораторної №1", style = MaterialTheme.typography.titleMedium)
                Text("Демонстраційні реєстрації: $registrations")
                Text(if (registrations >= 10) "Досягнуто 10 реєстрацій!"
                else "До першої цілі: ${10 - registrations}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { registrations++ }) { Text("Додати") }
                    OutlinedButton(onClick = { registrations = 0 }, enabled = registrations > 0) {
                        Text("Скинути")
                    }
                }
            }
        }
    }
}

@Composable
private fun ObservationCard(record: VetObservation, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(14.dp)) {
            Text("${record.petName} · ${record.type.title}",
                style = MaterialTheme.typography.titleMedium)
            Text("${record.value} ${record.type.unit} · ${record.recordedAt}")
            if (record.note.isNotBlank()) Text(record.note, style = MaterialTheme.typography.bodySmall)
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
    onAdd: (String, ObservationType, Double, String) -> Unit
) {
    var showAdd by rememberSaveable {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Журнал спостережень",
                style = MaterialTheme.typography.titleLarge
            )

            Button(
                onClick = { showAdd = true }
            ) {
                Text("+ Новий")
            }
        }

        // Пошук за приміткою
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

        // Фільтрація за типом спостереження
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(
                horizontal = 16.dp
            ),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
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
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
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
            onDismiss = {
                showAdd = false
            },
            onSave = { name, type, value, note ->
                onAdd(name, type, value, note)
                showAdd = false
            }
        )
    }
}


@Composable
private fun AddObservationDialog(
    onDismiss: () -> Unit,
    onSave: (String, ObservationType, Double, String) -> Unit
) {
    var petName by rememberSaveable { mutableStateOf("") }
    var selectedTypeName by rememberSaveable { mutableStateOf(ObservationType.TEMPERATURE.name) }
    var value by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    val type = ObservationType.valueOf(selectedTypeName)
    val numeric = value.replace(',', '.').toDoubleOrNull()
    val valid = petName.isNotBlank() && numeric != null && numeric.isFinite() && numeric > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Нове спостереження") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(petName, { petName = it }, label = { Text("Кличка тварини") },
                    singleLine = true)
                Text("Тип спостереження")
                ObservationType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { selectedTypeName = option.name },
                        label = { Text(option.title) }
                    )
                }
                OutlinedTextField(
                    value = value, onValueChange = { value = it },
                    label = { Text("Значення (${type.unit})") }, singleLine = true
                )
                OutlinedTextField(note, { note = it }, label = { Text("Примітка") })
            }
        },
        confirmButton = {
            TextButton(onClick = { if (numeric != null) onSave(petName, type, numeric, note) },
                enabled = valid) { Text("Зберегти") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}

@Composable
private fun DetailsContent(
    record: VetObservation?,
    onBack: () -> Unit,
    onShare: (VetObservation) -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← До журналу") }
        Text("Деталі запису", style = MaterialTheme.typography.headlineMedium)
        if (record == null) {
            Text("Запис не знайдено або видалено")
        } else {
            Text("Тварина: ${record.petName}")
            Text("Тип: ${record.type.title}")
            Text("Значення: ${record.value} ${record.type.unit}")
            Text("Дата: ${record.recordedAt}")
            Text("Примітка: ${record.note.ifBlank { "Не вказано" }}")
            if (record.type == ObservationType.SLEEP) {
                Text("Оцінка сну: ${sleepQuality(record.value)}")
                Text("Оцінка є навчальною, не ветеринарним висновком.",
                    style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = { onShare(record) }, modifier = Modifier.fillMaxWidth()) {
                Text("Поділитися (Snackbar)")
            }
            OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Видалити запис")
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Видалити запис?") },
            text = { Text("Після видалення можна одразу скасувати дію через Snackbar.") },
            confirmButton = { TextButton(onClick = {
                confirmDelete = false
                onDelete()
            }) { Text("Видалити") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Скасувати") } }
        )
    }
}

@Composable
private fun StatisticsContent(records: List<VetObservation>) {
    val summary = records.summary()
    val counts = records.countByType()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Статистика спостережень", style = MaterialTheme.typography.headlineMedium)
        Text("Загальна кількість: ${summary.observationCount}")
        counts.forEach { (type, count) ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(type.title, modifier = Modifier.weight(1f))
                    Text(count.toString(), style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        Text("Демонстраційні підсумки", style = MaterialTheme.typography.titleMedium)
        Text("Корм: ${summary.totalFeedGrams} г")
        Text("Середній пульс за записами: ${summary.averagePulse?.toInt() ?: "—"} уд/хв")
        Text("Остання тривалість сну: ${summary.latestSleepHours ?: "—"} год")
        Text("Дані різних тварин об’єднані лише для демонстрації операцій Kotlin; ці підсумки не можна використовувати як медичні показники.",
            style = MaterialTheme.typography.bodySmall)
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
            onValueChange = { nameDraft = it },
            label = { Text("Ваше ім'я") },
            placeholder = { Text("Введіть ваше ім'я") },
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
