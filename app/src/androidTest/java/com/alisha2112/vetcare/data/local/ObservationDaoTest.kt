package com.alisha2112.vetcare.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.alisha2112.vetcare.data.repository.RoomObservationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ObservationDaoTest {

    private lateinit var database: VetCareDatabase
    private lateinit var dao: ObservationDao

    @Before
    fun setUp() {
        val context =
            InstrumentationRegistry.getInstrumentation()
                .targetContext

        database = Room.inMemoryDatabaseBuilder(
            context,
            VetCareDatabase::class.java
        ).build()

        dao = database.observationDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun observation(
        petName: String,
        type: String,
        note: String
    ): ObservationEntity =
        ObservationEntity(
            petName = petName,
            type = type,
            value = 8.0,
            note = note,
            recordedAt = "29.10.2026"
        )

    @Test
    fun insertAndObserveAllReturnsNewestFirst() = runBlocking {
        dao.insert(
            observation(
                "Луна",
                "SLEEP",
                "Спокійний сон"
            )
        )

        dao.insert(
            observation(
                "Рекс",
                "PULSE",
                "Після відпочинку"
            )
        )

        val records = dao.observeAll().first()

        assertEquals(2, records.size)
        assertEquals("Рекс", records[0].petName)
        assertEquals("Луна", records[1].petName)
    }

    @Test
    fun filterByTypeReturnsOnlyMatchingRecords() = runBlocking {
        dao.insert(
            observation("Луна", "SLEEP", "Сон")
        )
        dao.insert(
            observation("Рекс", "PULSE", "Пульс")
        )
        dao.insert(
            observation("Боня", "SLEEP", "Відпочинок")
        )

        val result = dao.observeByType("SLEEP").first()

        assertEquals(2, result.size)
        assertTrue(result.all { it.type == "SLEEP" })
    }

    @Test
    fun noteSearchUsesSqlLike() = runBlocking {
        dao.insert(
            observation(
                "Луна",
                "SLEEP",
                "Спокійний сон"
            )
        )

        dao.insert(
            observation(
                "Рекс",
                "PULSE",
                "Після відпочинку"
            )
        )

        val result =
            dao.observeByNote("відпоч").first()

        assertEquals(1, result.size)
        assertEquals("Рекс", result.single().petName)
    }

    @Test
    fun combinedTypeAndNoteFilterWorks() = runBlocking {
        dao.insert(
            observation(
                "Луна",
                "SLEEP",
                "Після відпочинку"
            )
        )

        dao.insert(
            observation(
                "Рекс",
                "PULSE",
                "Після відпочинку"
            )
        )

        dao.insert(
            observation(
                "Боня",
                "PULSE",
                "Планове вимірювання"
            )
        )

        val result = dao.observeFiltered(
            type = "PULSE",
            query = "відпоч"
        ).first()

        assertEquals(1, result.size)
        assertEquals("Рекс", result.single().petName)
    }

    @Test
    fun deleteAndRestoreRecordPreservesId() = runBlocking {
        val id = dao.insert(
            observation(
                "Луна",
                "TEMPERATURE",
                "Планове спостереження"
            )
        )

        val repository = RoomObservationRepository(
            dao = dao,
            database = database
        )

        val deleted = repository.deleteRecord(id)

        assertNotNull(deleted)
        assertNull(dao.findById(id))

        repository.restoreRecord(
            requireNotNull(deleted)
        )

        val restored = dao.findById(id)

        assertNotNull(restored)
        assertEquals(id, restored?.id)
        assertEquals("Луна", restored?.petName)
    }
}
