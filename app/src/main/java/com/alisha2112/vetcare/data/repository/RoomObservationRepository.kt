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


    override fun observeFiltered(
        type: ObservationType?,
        query: String
    ): Flow<List<VetObservation>> =
        dao.observeFiltered(
            type = type?.name,
            query = query
        ).map { records ->
            records.map { it.toModel() }
        }

    override suspend fun addRecord(
        petName: String,
        type: ObservationType,
        value: Double,
        note: String,
        latitude: Double?,
        longitude: Double?,
        photoPath: String?
    ) {
        require(
            (latitude == null && longitude == null) ||
                    (
                            latitude != null &&
                                    longitude != null &&
                                    latitude in -90.0..90.0 &&
                                    longitude in -180.0..180.0
                            )
        ) {
            "Некоректні GPS-координати"
        }

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
                recordedAt = date,
                latitude = latitude,
                longitude = longitude,
                photoPath = photoPath
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
