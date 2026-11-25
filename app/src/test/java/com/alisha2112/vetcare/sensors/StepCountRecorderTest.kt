package com.alisha2112.vetcare.sensors

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

class StepCountRecorderTest {

    private data class SavedRecord(
        val petName: String,
        val type: ObservationType,
        val value: Double,
        val note: String,
        val latitude: Double?,
        val photoPath: String?
    )

    private class FakeRepository : ObservationRepository {
        val records = mutableListOf<SavedRecord>()
        var shouldFail = false
        var writeGate: CompletableDeferred<Unit>? = null

        override fun observeRecords(): Flow<List<VetObservation>> = flowOf(emptyList())

        override fun observeRecordsByType(type: ObservationType): Flow<List<VetObservation>> =
            flowOf(emptyList())

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
            if (shouldFail) throw IllegalStateException("Database error")
            records.add(SavedRecord(petName, type, value, note, latitude, photoPath))
        }

        override suspend fun deleteRecord(id: Long): VetObservation? = null

        override suspend fun restoreRecord(record: VetObservation) = Unit
    }

    @Test
    fun `valid result is saved as STEPS observation`() = runTest {
        val repository = FakeRepository()
        val recorder = StepCountRecorder(repository)

        val result = recorder.save(
            petName = "  Рекс ",
            steps = 1234L,
            sessionId = 1L,
            note = " Прогулянка "
        )

        assertEquals(StepSaveResult.Saved, result)
        val saved = repository.records.single()
        assertEquals("Рекс", saved.petName)
        assertEquals(ObservationType.STEPS, saved.type)
        assertEquals(1234.0, saved.value, 0.0)
        assertTrue(saved.note.contains("TYPE_STEP_COUNTER"))
        assertTrue(saved.note.endsWith("Прогулянка"))
        assertEquals(null, saved.latitude)
        assertEquals(null, saved.photoPath)
    }

    @Test
    fun `blank or too long pet name is rejected`() = runTest {
        val repository = FakeRepository()
        val recorder = StepCountRecorder(repository)

        assertTrue(recorder.save("   ", 10L, 1L) is StepSaveResult.Invalid)
        assertTrue(recorder.save("а".repeat(51), 10L, 1L) is StepSaveResult.Invalid)
        assertTrue(repository.records.isEmpty())
    }

    @Test
    fun `zero or negative steps are rejected`() = runTest {
        val repository = FakeRepository()
        val recorder = StepCountRecorder(repository)

        assertTrue(recorder.save("Рекс", 0L, 1L) is StepSaveResult.Invalid)
        assertTrue(recorder.save("Рекс", -5L, 2L) is StepSaveResult.Invalid)
        assertTrue(recorder.save("Рекс", 5L, 0L) is StepSaveResult.Invalid)
        assertTrue(repository.records.isEmpty())
    }

    @Test
    fun `same session is not saved twice`() = runTest {
        val repository = FakeRepository()
        val recorder = StepCountRecorder(repository)

        assertEquals(StepSaveResult.Saved, recorder.save("Рекс", 100L, 7L))
        assertEquals(StepSaveResult.AlreadySaved, recorder.save("Рекс", 100L, 7L))
        assertEquals(1, repository.records.size)
    }

    @Test
    fun `new session can be saved after previous one`() = runTest {
        val repository = FakeRepository()
        val recorder = StepCountRecorder(repository)

        recorder.save("Рекс", 100L, 1L)
        assertEquals(StepSaveResult.Saved, recorder.save("Рекс", 50L, 2L))
        assertEquals(2, repository.records.size)
    }

    @Test
    fun `concurrent double click saves only one record`() = runTest {
        val repository = FakeRepository()
        repository.writeGate = CompletableDeferred()
        val recorder = StepCountRecorder(repository)

        val first = async(start = CoroutineStart.UNDISPATCHED) {
            recorder.save("Рекс", 100L, 3L)
        }
        val second = async(start = CoroutineStart.UNDISPATCHED) {
            recorder.save("Рекс", 100L, 3L)
        }

        repository.writeGate?.complete(Unit)

        val results = listOf(first.await(), second.await())
        assertEquals(1, results.count { it == StepSaveResult.Saved })
        assertEquals(1, results.count { it == StepSaveResult.AlreadySaved })
        assertEquals(1, repository.records.size)
    }

    @Test
    fun `failed write can be retried`() = runTest {
        val repository = FakeRepository()
        repository.shouldFail = true
        val recorder = StepCountRecorder(repository)

        try {
            recorder.save("Рекс", 100L, 4L)
            fail("Очікувався виняток репозиторію")
        } catch (exception: IllegalStateException) {
            // очікувано
        }

        repository.shouldFail = false
        assertEquals(StepSaveResult.Saved, recorder.save("Рекс", 100L, 4L))
        assertEquals(1, repository.records.size)
    }
}
