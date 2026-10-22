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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecordsViewModel @Inject constructor(
    private val repository: ObservationRepository
) : ViewModel() {

    private val _selectedType =
        MutableStateFlow<ObservationType?>(null)

    val selectedType: StateFlow<ObservationType?> =
        _selectedType.asStateFlow()

    val allRecords: StateFlow<List<VetObservation>> =
        repository.observeRecords().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val filteredRecords: StateFlow<List<VetObservation>> =
        selectedType.flatMapLatest { type ->
            if (type == null) {
                repository.observeRecords()
            } else {
                repository.observeRecordsByType(type)
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    fun selectType(type: ObservationType?) {
        _selectedType.value = type
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
}
