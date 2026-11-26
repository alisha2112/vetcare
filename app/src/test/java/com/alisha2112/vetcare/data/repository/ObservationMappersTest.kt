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

    @Test
    fun coordinatesPhotoAndStepsSurviveRoundTrip() {
        val original = ObservationEntity(
            id = 11L,
            petName = "Рекс",
            type = "STEPS",
            value = 4321.0,
            note = "Крокомір",
            recordedAt = "25.11.2026",
            latitude = 50.4501,
            longitude = 30.5234,
            photoPath = "observation_photos/11111111-2222-3333-4444-555555555555.jpg"
        )

        val model = original.toModel()

        assertEquals(ObservationType.STEPS, model.type)
        assertEquals(50.4501, model.latitude!!, 0.0)
        assertEquals(30.5234, model.longitude!!, 0.0)
        assertEquals(original.photoPath, model.photoPath)
        assertEquals(original, model.toEntity())
    }

    @Test
    fun missingOptionalFieldsStayNull() {
        val model = ObservationEntity(
            id = 12L,
            petName = "Луна",
            type = "SLEEP",
            value = 8.0,
            note = "",
            recordedAt = "25.11.2026",
            latitude = 50.0
        ).toModel()

        assertEquals(50.0, model.latitude!!, 0.0)
        assertEquals(null, model.longitude)
        assertEquals(null, model.photoPath)
    }
}
