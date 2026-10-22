package com.alisha2112.vetcare.data.repository

import com.alisha2112.vetcare.data.model.ObservationType
import com.alisha2112.vetcare.data.model.VetObservation
import kotlinx.coroutines.flow.Flow

interface ObservationRepository {

    fun observeRecords(): Flow<List<VetObservation>>

    fun observeRecordsByType(
        type: ObservationType
    ): Flow<List<VetObservation>>

    suspend fun addRecord(
        petName: String,
        type: ObservationType,
        value: Double,
        note: String
    )

    suspend fun deleteRecord(id: Long): VetObservation?

    suspend fun restoreRecord(record: VetObservation)
}
