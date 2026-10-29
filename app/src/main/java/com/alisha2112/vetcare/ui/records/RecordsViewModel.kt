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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecordsViewModel @Inject constructor(
    private val repository: ObservationRepository,
    private val csvExporter: ObservationCsvExporter
) : ViewModel() {

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
        note: String
    ) {
        if (
            petName.isBlank() ||
            !value.isFinite() ||
            value <= 0
        ) return

        viewModelScope.launch {
            repository.addRecord(
                petName,
                type,
                value,
                note
            )
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
