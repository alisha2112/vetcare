package com.alisha2112.vetcare.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.alisha2112.vetcare.data.model.ObservationType
import com.alisha2112.vetcare.data.repository.RoomObservationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Перевірка міграцій Room 1 → 2 → 3 на окремій тестовій базі
 * (реальна база vetcare.db не використовується).
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val testDb = "migration-test.db"

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation,
        VetCareDatabase::class.java
    )

    // Тестова база не залишається в каталозі застосунку.
    @After
    fun deleteTestDatabase() {
        val context = instrumentation.targetContext
        val lockFile = java.io.File(context.getDatabasePath(testDb).path + ".lck")
        context.deleteDatabase(testDb)
        lockFile.delete()
    }

    private fun SupportSQLiteDatabase.insertV1(
        id: Long,
        petName: String,
        type: String,
        value: Double,
        note: String,
        recordedAt: String
    ) {
        execSQL(
            "INSERT INTO observations (id, pet_name, type, value, note, recorded_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf<Any>(id, petName, type, value, note, recordedAt)
        )
    }

    private fun SupportSQLiteDatabase.insertV1Samples() {
        insertV1(1, "Луна", "SLEEP", 8.5, "Спокійний сон", "20.10.2026")
        insertV1(2, "Рекс", "PULSE", 92.0, "", "21.10.2026")
        insertV1(5, "Мурка", "FEEDING", 120.0, "Корм, \"кома\"", "22.10.2026")
    }

    private fun SupportSQLiteDatabase.columnNames(): List<String> =
        query("PRAGMA table_info(observations)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(nameIndex))
            }
        }

    private fun SupportSQLiteDatabase.assertV1DataPreserved() {
        query(
            "SELECT id, pet_name, type, value, note, recorded_at " +
                    "FROM observations ORDER BY id"
        ).use { cursor ->
            assertEquals(3, cursor.count)

            cursor.moveToNext()
            assertEquals(1L, cursor.getLong(0))
            assertEquals("Луна", cursor.getString(1))
            assertEquals("SLEEP", cursor.getString(2))
            assertEquals(8.5, cursor.getDouble(3), 0.0)
            assertEquals("Спокійний сон", cursor.getString(4))
            assertEquals("20.10.2026", cursor.getString(5))

            cursor.moveToNext()
            assertEquals(2L, cursor.getLong(0))
            assertEquals("Рекс", cursor.getString(1))
            assertEquals("", cursor.getString(4))

            cursor.moveToNext()
            assertEquals(5L, cursor.getLong(0))
            assertEquals("Корм, \"кома\"", cursor.getString(4))
        }
    }

    @Test
    fun migrate1To2_keepsDataAndAddsNullCoordinates() {
        helper.createDatabase(testDb, 1).use { db ->
            db.insertV1Samples()
            assertFalse(db.columnNames().contains("latitude"))
        }

        // validateDroppedTables + перевірка схеми за 2.json.
        helper.runMigrationsAndValidate(
            testDb, 2, true, DatabaseMigrations.MIGRATION_1_2
        ).use { db ->
            assertTrue(db.columnNames().containsAll(listOf("latitude", "longitude")))
            db.assertV1DataPreserved()

            db.query(
                "SELECT COUNT(*) FROM observations " +
                        "WHERE latitude IS NULL AND longitude IS NULL"
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(3, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate2To3_keepsCoordinatesAndAddsNullPhotoPath() {
        helper.createDatabase(testDb, 2).use { db ->
            db.execSQL(
                "INSERT INTO observations " +
                        "(id, pet_name, type, value, note, recorded_at, latitude, longitude) " +
                        "VALUES (1, 'Рекс', 'TEMPERATURE', 38.6, 'З GPS', '23.10.2026', 50.4501, 30.5234)"
            )
            db.execSQL(
                "INSERT INTO observations " +
                        "(id, pet_name, type, value, note, recorded_at, latitude, longitude) " +
                        "VALUES (2, 'Луна', 'SLEEP', 7.0, 'Без GPS', '23.10.2026', NULL, NULL)"
            )
        }

        helper.runMigrationsAndValidate(
            testDb, 3, true, DatabaseMigrations.MIGRATION_2_3
        ).use { db ->
            assertTrue(db.columnNames().contains("photo_path"))

            db.query(
                "SELECT id, pet_name, latitude, longitude, photo_path " +
                        "FROM observations ORDER BY id"
            ).use { cursor ->
                assertEquals(2, cursor.count)

                cursor.moveToNext()
                assertEquals(1L, cursor.getLong(0))
                assertEquals("Рекс", cursor.getString(1))
                assertEquals(50.4501, cursor.getDouble(2), 1e-9)
                assertEquals(30.5234, cursor.getDouble(3), 1e-9)
                assertTrue(cursor.isNull(4))

                cursor.moveToNext()
                assertEquals(2L, cursor.getLong(0))
                assertTrue(cursor.isNull(2))
                assertTrue(cursor.isNull(3))
                assertTrue(cursor.isNull(4))
            }
        }
    }

    @Test
    fun migrate1To3_fullChainReadableByCurrentRoomModel() = runBlocking {
        helper.createDatabase(testDb, 1).use { db ->
            db.insertV1Samples()
        }

        helper.runMigrationsAndValidate(
            testDb,
            3,
            true,
            DatabaseMigrations.MIGRATION_1_2,
            DatabaseMigrations.MIGRATION_2_3
        ).use { db ->
            db.assertV1DataPreserved()
        }

        val database = openMigratedDatabase()
        try {
            val records = RoomObservationRepository(
                database.observationDao(),
                database
            ).observeRecords().first()

            assertEquals(listOf(5L, 2L, 1L), records.map { it.id })

            val sleep = records.single { it.id == 1L }
            assertEquals("Луна", sleep.petName)
            assertEquals(ObservationType.SLEEP, sleep.type)
            assertEquals(8.5, sleep.value, 0.0)
            assertEquals("20.10.2026", sleep.recordedAt)
            assertNull(sleep.latitude)
            assertNull(sleep.longitude)
            assertNull(sleep.photoPath)
        } finally {
            database.close()
        }
    }

    @Test
    fun migratedDatabaseSupportsAllCurrentOperations() = runBlocking {
        helper.createDatabase(testDb, 1).use { db ->
            db.insertV1Samples()
        }

        val database = openMigratedDatabase()
        try {
            val dao = database.observationDao()
            val repository = RoomObservationRepository(dao, database)

            repository.addRecord("Бім", ObservationType.TEMPERATURE, 38.4, "Звичайний")
            repository.addRecord(
                "Бім", ObservationType.PULSE, 88.0, "З GPS",
                latitude = 49.8397, longitude = 24.0297
            )
            repository.addRecord(
                "Бім", ObservationType.FEEDING, 150.0, "З фото",
                photoPath = "observation_photos/11111111-2222-3333-4444-555555555555.jpg"
            )
            repository.addRecord("Бім", ObservationType.STEPS, 1234.0, "Крокомір")

            val records = repository.observeRecords().first()
            assertEquals(7, records.size)

            val gps = records.single { it.note == "З GPS" }
            assertEquals(49.8397, gps.latitude!!, 1e-9)
            assertEquals(24.0297, gps.longitude!!, 1e-9)

            val photo = records.single { it.note == "З фото" }
            assertEquals(
                "observation_photos/11111111-2222-3333-4444-555555555555.jpg",
                photo.photoPath
            )

            val steps = repository.observeRecordsByType(ObservationType.STEPS).first()
            assertEquals(1, steps.size)
            assertEquals(1234.0, steps.single().value, 0.0)

            // Видалення та відновлення з усіма полями.
            val full = records.single { it.note == "З GPS" }
                .copy(photoPath = "observation_photos/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee.png")
            repository.restoreRecord(full)

            val deleted = repository.deleteRecord(full.id)
            assertEquals(full, deleted)
            assertNull(dao.findById(full.id))

            repository.restoreRecord(requireNotNull(deleted))
            val restored = repository.observeRecords().first().single { it.id == full.id }
            assertEquals(full, restored)

            // Старий запис після міграції теж видаляється й відновлюється.
            val old = repository.deleteRecord(2L)
            assertNotNull(old)
            repository.restoreRecord(old!!)
            assertEquals(old, repository.observeRecords().first().single { it.id == 2L })
        } finally {
            database.close()
        }
    }

    private fun openMigratedDatabase(): VetCareDatabase =
        Room.databaseBuilder(
            instrumentation.targetContext,
            VetCareDatabase::class.java,
            testDb
        )
            .addMigrations(
                DatabaseMigrations.MIGRATION_1_2,
                DatabaseMigrations.MIGRATION_2_3
            )
            .build()
            .also { helper.closeWhenFinished(it) }
}
