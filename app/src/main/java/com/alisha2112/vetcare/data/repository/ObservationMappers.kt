package com.alisha2112.vetcare.data.repository

import com.alisha2112.vetcare.data.local.ObservationEntity
import com.alisha2112.vetcare.data.model.ObservationType
import com.alisha2112.vetcare.data.model.VetObservation

fun ObservationEntity.toModel(): VetObservation =
    VetObservation(
        id = id,
        petName = petName,
        type = ObservationType.valueOf(type),
        value = value,
        note = note,
        recordedAt = recordedAt
    )

fun VetObservation.toEntity(): ObservationEntity =
    ObservationEntity(
        id = id,
        petName = petName,
        type = type.name,
        value = value,
        note = note,
        recordedAt = recordedAt
    )
