package com.alisha2112.vetcare.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.alisha2112.vetcare.data.local.VetCareDatabase
import com.alisha2112.vetcare.data.model.ObservationType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class RoomObservationRepositoryTest {

    private lateinit var database: VetCareDatabase
    private lateinit var repository: RoomObservationRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            VetCareDatabase::class.java
        ).build()

        repository = RoomObservationRepository(
            database.observationDao(),
            database
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun assertRejected(latitude: Double?, longitude: Double?) {
        try {
            repository.addRecord(
                "Рекс", ObservationType.PULSE, 90.0, "",
                latitude = latitude, longitude = longitude
            )
            fail("Очікувалась відмова для ($latitude, $longitude)")
        } catch (expected: IllegalArgumentException) {
            // очікувано
        }
    }

    @Test
    fun invalidOrPartialCoordinatesAreRejectedWithoutWriting() = runBlocking {
        assertRejected(50.0, null)
        assertRejected(null, 30.0)
        assertRejected(91.0, 30.0)
        assertRejected(-90.5, 30.0)
        assertRejected(50.0, 180.5)
        assertRejected(Double.NaN, 30.0)

        assertTrue(repository.observeRecords().first().isEmpty())
    }

    @Test
    fun boundaryCoordinatesAreAccepted() = runBlocking {
        repository.addRecord(
            "Рекс", ObservationType.PULSE, 90.0, "",
            latitude = -90.0, longitude = 180.0
        )

        val saved = repository.observeRecords().first().single()
        assertEquals(-90.0, saved.latitude!!, 0.0)
        assertEquals(180.0, saved.longitude!!, 0.0)
    }

    @Test
    fun recordWithCoordinatesAndPhotoIsSavedTrimmedAndDated() = runBlocking {
        val photo = "observation_photos/11111111-2222-3333-4444-555555555555.jpg"

        repository.addRecord(
            petName = "  Луна ",
            type = ObservationType.TEMPERATURE,
            value = 38.7,
            note = " Огляд ",
            latitude = 50.45,
            longitude = 30.52,
            photoPath = photo
        )

        val saved = repository.observeRecords().first().single()
        assertEquals("Луна", saved.petName)
        assertEquals("Огляд", saved.note)
        assertEquals(50.45, saved.latitude!!, 0.0)
        assertEquals(30.52, saved.longitude!!, 0.0)
        assertEquals(photo, saved.photoPath)
        assertEquals(
            SimpleDateFormat("dd.MM.yyyy", Locale("uk")).format(Date()),
            saved.recordedAt
        )
    }

    @Test
    fun deleteAndRestoreKeepsIdCoordinatesAndPhoto() = runBlocking {
        val photo = "observation_photos/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee.png"
        repository.addRecord(
            "Рекс", ObservationType.PULSE, 95.0, "BLE",
            latitude = 49.84, longitude = 24.03, photoPath = photo
        )
        repository.addRecord("Луна", ObservationType.STEPS, 2500.0, "Крокомір")

        val original = repository.observeRecords().first().single { it.petName == "Рекс" }

        val deleted = repository.deleteRecord(original.id)
        assertEquals(original, deleted)
        assertEquals(1, repository.observeRecords().first().size)
        assertNull(repository.deleteRecord(original.id))

        repository.restoreRecord(deleted!!)

        val restored = repository.observeRecords().first()
        assertEquals(2, restored.size)
        assertEquals(original, restored.single { it.id == original.id })
    }

    @Test
    fun stepsRecordsWorkWithTypeFilterAndSearch() = runBlocking {
        repository.addRecord("Рекс", ObservationType.STEPS, 1500.0, "Джерело: крокомір")
        repository.addRecord("Рекс", ObservationType.PULSE, 80.0, "Джерело: BLE")

        val steps = repository.observeFiltered(ObservationType.STEPS, "").first()
        assertEquals(1, steps.size)
        assertEquals(ObservationType.STEPS, steps.single().type)

        val search = repository.observeFiltered(null, "крокомір").first()
        assertEquals(listOf(ObservationType.STEPS), search.map { it.type })
    }
}
