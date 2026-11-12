package com.alisha2112.vetcare.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "observations")
data class ObservationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "pet_name")
    val petName: String,

    val type: String,

    val value: Double,

    val note: String,

    @ColumnInfo(name = "recorded_at")
    val recordedAt: String
)
