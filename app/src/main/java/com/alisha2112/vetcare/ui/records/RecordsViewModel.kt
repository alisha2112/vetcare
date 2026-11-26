package com.alisha2112.vetcare.ui.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alisha2112.vetcare.data.model.ObservationType
import com.alisha2112.vetcare.data.model.VetObservation
import com.alisha2112.vetcare.data.repository.ObservationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.net.Uri
import com.alisha2112.vetcare.data.export.ObservationCsvExporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

data class AddObservationUiState(
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecordsViewModel @Inject constructor(
    private val repository: ObservationRepository,
    private val csvExporter: ObservationCsvExporter
) : ViewModel() {
    private val _addState =
        MutableStateFlow(AddObservationUiState())

    val addState = _addState.asStateFlow()

    fun clearAddResult() {
        if (!_addState.value.isSaving) {
            _addState.value = AddObservationUiState()
        }
    }
    private val _selectedType =
        MutableStateFlow<ObservationType?>(null)

    val selectedType: StateFlow<ObservationType?> =
        _selectedType.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    val searchQuery: StateFlow<String> =
        _searchQuery.asStateFlow()

    val allRecords: StateFlow<List<VetObservation>> =
        repository.observeRecords().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val filteredRecords: StateFlow<List<VetObservation>> =
        combine(
            selectedType,
            searchQuery
        ) { type, query ->
            type to query.trim()
        }.flatMapLatest { (type, query) ->
            repository.observeFiltered(type, query)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )
    private val _isExporting = MutableStateFlow(false)

    val isExporting: StateFlow<Boolean> =
        _isExporting.asStateFlow()

    fun selectType(type: ObservationType?) {
        _selectedType.value = type
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    fun addRecord(
        petName: String,
        type: ObservationType,
        value: Double,
        note: String,
        latitude: Double? = null,
        longitude: Double? = null,
        photoPath: String? = null
    ) {
        if (
            _addState.value.isSaving ||
            _addState.value.saved
        ) return

        if (
            petName.isBlank() ||
            !value.isFinite() ||
            value <= 0
        ) {
            _addState.value = AddObservationUiState(
                error = "Перевірте введені дані"
            )
            return
        }

        val validCoordinates =
            (latitude == null && longitude == null) ||
                    (
                            latitude != null &&
                                    longitude != null &&
                                    latitude in -90.0..90.0 &&
                                    longitude in -180.0..180.0
                            )

        if (!validCoordinates) {
            _addState.value = AddObservationUiState(
                error = "Некоректні GPS-координати"
            )
            return
        }

        _addState.value = AddObservationUiState(
            isSaving = true
        )

        viewModelScope.launch {
            try {
                repository.addRecord(
                    petName = petName,
                    type = type,
                    value = value,
                    note = note,
                    latitude = latitude,
                    longitude = longitude,
                    photoPath = photoPath
                )

                _addState.value = AddObservationUiState(
                    saved = true
                )

            } catch (exception: CancellationException) {
                _addState.value = AddObservationUiState()
                throw exception

            } catch (exception: Exception) {
                _addState.value = AddObservationUiState(
                    error = "Не вдалося зберегти спостереження. " +
                            "Спробуйте ще раз."
                )
            }
        }
    }

    fun deleteRecord(
        id: Long,
        onDeleted: (VetObservation?) -> Unit
    ) {
        viewModelScope.launch {
            onDeleted(repository.deleteRecord(id))
        }
    }

    fun restoreRecord(record: VetObservation) {
        viewModelScope.launch {
            repository.restoreRecord(record)
        }
    }

    fun exportCsv(
        onReady: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        if (_isExporting.value) return

        _isExporting.value = true

        viewModelScope.launch {
            try {
                // Отримуємо всі записи, а не filteredRecords
                val records = repository
                    .observeRecords()
                    .first()

                if (records.isEmpty()) {
                    onError("Немає записів для експорту")
                    return@launch
                }

                val uri = csvExporter.export(records)

                onReady(uri)

            } catch (exception: CancellationException) {
                throw exception

            } catch (exception: Exception) {
                onError(
                    "Не вдалося експортувати CSV-файл"
                )

            } finally {
                _isExporting.value = false
            }
        }
    }

}
