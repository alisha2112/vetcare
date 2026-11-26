package com.alisha2112.vetcare.ble

import com.alisha2112.vetcare.data.model.ObservationType
import com.alisha2112.vetcare.data.model.VetObservation
import com.alisha2112.vetcare.data.repository.ObservationRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BlePulseRecorderTest {

    private data class SavedRecord(
        val petName: String,
        val type: ObservationType,
        val value: Double,
        val note: String
    )

    private class FakeRepository : ObservationRepository {
        val records = mutableListOf<SavedRecord>()
        var shouldFail = false
        var writeGate: CompletableDeferred<Unit>? = null

        override fun observeRecords(): Flow<List<VetObservation>> =
            flowOf(emptyList())

        override fun observeRecordsByType(
            type: ObservationType
        ): Flow<List<VetObservation>> = flowOf(emptyList())

        override fun observeFiltered(
            type: ObservationType?,
            query: String
        ): Flow<List<VetObservation>> = flowOf(emptyList())

        override suspend fun addRecord(
            petName: String,
            type: ObservationType,
            value: Double,
            note: String,
            latitude: Double?,
            longitude: Double?,
            photoPath: String?
        ) {
            writeGate?.await()

            if (shouldFail) {
                throw IllegalStateException("Database error")
            }

            records.add(
                SavedRecord(petName, type, value, note)
            )
        }

        override suspend fun deleteRecord(
            id: Long
        ): VetObservation? = null

        override suspend fun restoreRecord(
            record: VetObservation
        ) = Unit
    }

    @Test
    fun savesCorrectPulseRecord() = runTest {
        val repository = FakeRepository()
        val recorder = BlePulseRecorder(repository)

        val result = recorder.save(
            petName = "  Барсик  ",
            pulse = 75,
            deviceName = "BLE Sensor",
            measurementId = 1L
        )

        assertEquals(PulseSaveResult.Saved, result)
        assertEquals(1, repository.records.size)

        val saved = repository.records.single()

        assertEquals("Барсик", saved.petName)
        assertEquals(ObservationType.PULSE, saved.type)
        assertEquals(75.0, saved.value, 0.0)
        assertTrue(saved.note.contains("BLE Sensor"))
    }

    @Test
    fun rejectsInvalidMeasurements() = runTest {
        val repository = FakeRepository()
        val recorder = BlePulseRecorder(repository)

        val blankName = recorder.save(
            "", 75, "Sensor", 1L
        )

        val missingPulse = recorder.save(
            "Барсик", null, "Sensor", 2L
        )

        val invalidId = recorder.save(
            "Барсик", 75, "Sensor", 0L
        )

        assertTrue(blankName is PulseSaveResult.Invalid)
        assertTrue(missingPulse is PulseSaveResult.Invalid)
        assertTrue(invalidId is PulseSaveResult.Invalid)
        assertTrue(repository.records.isEmpty())
    }

    @Test
    fun doesNotSaveSameMeasurementTwice() = runTest {
        val repository = FakeRepository()
        val recorder = BlePulseRecorder(repository)

        val first = recorder.save(
            "Барсик", 75, "Sensor", 10L
        )

        val second = recorder.save(
            "Барсик", 75, "Sensor", 10L
        )

        assertEquals(PulseSaveResult.Saved, first)
        assertEquals(PulseSaveResult.AlreadySaved, second)
        assertEquals(1, repository.records.size)
    }

    @Test
    fun samePulseWithNewMeasurementIdCanBeSaved() = runTest {
        val repository = FakeRepository()
        val recorder = BlePulseRecorder(repository)

        recorder.save("Барсик", 75, "Sensor", 10L)
        recorder.save("Барсик", 75, "Sensor", 11L)

        assertEquals(2, repository.records.size)
    }

    @Test
    fun failedWriteCanBeRetried() = runTest {
        val repository = FakeRepository()
        val recorder = BlePulseRecorder(repository)

        repository.shouldFail = true

        try {
            recorder.save("Барсик", 75, "Sensor", 20L)
            fail("Очікувалася помилка запису")
        } catch (expected: IllegalStateException) {
            // Помилка репозиторію має бути передана викликачу.
        }

        repository.shouldFail = false

        val result = recorder.save(
            "Барсик", 75, "Sensor", 20L
        )

        assertEquals(PulseSaveResult.Saved, result)
        assertEquals(1, repository.records.size)
    }

    @Test
    fun concurrentRequestsSaveOnlyOneRecord() = runTest {
        val repository = FakeRepository()
        val recorder = BlePulseRecorder(repository)
        val gate = CompletableDeferred<Unit>()

        repository.writeGate = gate

        val first = async(start = CoroutineStart.UNDISPATCHED) {
            recorder.save("Барсик", 75, "Sensor", 30L)
        }

        val second = async(start = CoroutineStart.UNDISPATCHED) {
            recorder.save("Барсик", 75, "Sensor", 30L)
        }

        gate.complete(Unit)

        assertEquals(PulseSaveResult.Saved, first.await())
        assertEquals(
            PulseSaveResult.AlreadySaved,
            second.await()
        )
        assertEquals(1, repository.records.size)
    }
}
