package com.alisha2112.vetcare.data.repository

import com.alisha2112.vetcare.data.local.ObservationEntity
import com.alisha2112.vetcare.data.model.ObservationType
import org.junit.Assert.assertEquals
import org.junit.Test

class ObservationMappersTest {

    @Test
    fun entityIsConvertedToDomainModel() {
        val entity = ObservationEntity(
            id = 7L,
            petName = "Луна",
            type = "TEMPERATURE",
            value = 38.5,
            note = "Планове спостереження",
            recordedAt = "29.10.2026"
        )

        val model = entity.toModel()

        assertEquals(7L, model.id)
        assertEquals("Луна", model.petName)
        assertEquals(
            ObservationType.TEMPERATURE,
            model.type
        )
        assertEquals(38.5, model.value, 0.001)
        assertEquals(
            "Планове спостереження",
            model.note
        )
        assertEquals("29.10.2026", model.recordedAt)
    }

    @Test
    fun domainModelRoundTripPreservesFields() {
        val original = ObservationEntity(
            id = 10L,
            petName = "Рекс",
            type = "PULSE",
            value = 86.0,
            note = "Після відпочинку",
            recordedAt = "29.10.2026"
        )

        val restored = original
            .toModel()
            .toEntity()

        assertEquals(original, restored)
    }
}
