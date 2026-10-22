package com.alisha2112.vetcare.data.repository

import com.alisha2112.vetcare.data.model.ObservationType
import com.alisha2112.vetcare.data.model.VetObservation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryObservationRepository @Inject constructor() :
    ObservationRepository {

    private val records = MutableStateFlow(
        listOf(
            VetObservation(
                4,
                "Луна",
                ObservationType.SLEEP,
                8.0,
                "Спокійний сон",
                "22.10.2026"
            ),
            VetObservation(
                3,
                "Рекс",
                ObservationType.PULSE,
                86.0,
                "Після відпочинку",
                "22.10.2026"
            ),
            VetObservation(
                2,
                "Луна",
                ObservationType.FEEDING,
                120.0,
                "Ранкова порція",
                "22.10.2026"
            ),
            VetObservation(
                1,
                "Рекс",
                ObservationType.TEMPERATURE,
                38.5,
                "Планове спостереження",
                "22.10.2026"
            )
        )
    )

    override fun observeRecords() = records.asStateFlow()

    override fun observeRecordsByType(type: ObservationType) =
        records.map { list ->
            list.filter { it.type == type }
        }

    override suspend fun addRecord(
        petName: String,
        type: ObservationType,
        value: Double,
        note: String
    ) {
        records.update { current ->
            val nextId =
                (current.maxOfOrNull { it.id } ?: 0L) + 1L

            val date = SimpleDateFormat(
                "dd.MM.yyyy",
                Locale("uk")
            ).format(Date())

            listOf(
                VetObservation(
                    nextId,
                    petName.trim(),
                    type,
                    value,
                    note.trim(),
                    date
                )
            ) + current
        }
    }

    override suspend fun deleteRecord(id: Long): VetObservation? {
        var removed: VetObservation? = null

        records.update { current ->
            removed = current.firstOrNull { it.id == id }
            current.filterNot { it.id == id }
        }

        return removed
    }

    override suspend fun restoreRecord(record: VetObservation) {
        records.update { current ->
            if (current.any { it.id == record.id }) {
                current
            } else {
                (current + record).sortedByDescending { it.id }
            }
        }
    }
}
