package com.alisha2112.vetcare.data.repository

import androidx.room.withTransaction
import com.alisha2112.vetcare.data.local.ObservationDao
import com.alisha2112.vetcare.data.local.ObservationEntity
import com.alisha2112.vetcare.data.local.VetCareDatabase
import com.alisha2112.vetcare.data.model.ObservationType
import com.alisha2112.vetcare.data.model.VetObservation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomObservationRepository @Inject constructor(
    private val dao: ObservationDao,
    private val database: VetCareDatabase
) : ObservationRepository {

    override fun observeRecords(): Flow<List<VetObservation>> =
        dao.observeAll().map { records ->
            records.map { it.toModel() }
        }

    override fun observeRecordsByType(
        type: ObservationType
    ): Flow<List<VetObservation>> =
        dao.observeByType(type.name).map { records ->
            records.map { it.toModel() }
        }

    override suspend fun addRecord(
        petName: String,
        type: ObservationType,
        value: Double,
        note: String
    ) {
        val date = SimpleDateFormat(
            "dd.MM.yyyy",
            Locale("uk")
        ).format(Date())

        dao.insert(
            ObservationEntity(
                petName = petName.trim(),
                type = type.name,
                value = value,
                note = note.trim(),
                recordedAt = date
            )
        )
    }

    override suspend fun deleteRecord(
        id: Long
    ): VetObservation? =
        database.withTransaction {
            val record = dao.findById(id)
                ?: return@withTransaction null

            dao.deleteById(id)
            record.toModel()
        }

    override suspend fun restoreRecord(
        record: VetObservation
    ) {
        dao.insertOrReplace(record.toEntity())
    }
}
