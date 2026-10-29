package com.alisha2112.vetcare.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ObservationDao {

    @Query("SELECT * FROM observations ORDER BY id DESC")
    fun observeAll(): Flow<List<ObservationEntity>>

    @Query(
        "SELECT * FROM observations " +
                "WHERE type = :type ORDER BY id DESC"
    )
    fun observeByType(
        type: String
    ): Flow<List<ObservationEntity>>

    @Query(
        "SELECT * FROM observations " +
                "WHERE id = :id LIMIT 1"
    )
    suspend fun findById(id: Long): ObservationEntity?

    @Insert
    suspend fun insert(record: ObservationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(record: ObservationEntity)

    @Query("DELETE FROM observations WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
