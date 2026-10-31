package com.alisha2112.vetcare.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.alisha2112.vetcare.data.local.ObservationDao
import com.alisha2112.vetcare.data.local.VetCareDatabase
import com.alisha2112.vetcare.data.model.ObservationType
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): VetCareDatabase =
        Room.databaseBuilder(
            context,
            VetCareDatabase::class.java,
            "vetcare.db"
        )
            .addCallback(SeedObservationsCallback())
            .build()

    @Provides
    fun provideObservationDao(
        database: VetCareDatabase
    ): ObservationDao = database.observationDao()
}

private class SeedObservationsCallback :
    RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

        val examples = listOf(
            arrayOf<Any>(
                4L, "Луна",
                ObservationType.SLEEP.name,
                8.0, "Спокійний сон", "22.10.2026"
            ),
            arrayOf<Any>(
                3L, "Рекс",
                ObservationType.PULSE.name,
                86.0, "Після відпочинку", "22.10.2026"
            ),
            arrayOf<Any>(
                2L, "Луна",
                ObservationType.FEEDING.name,
                120.0, "Ранкова порція", "22.10.2026"
            ),
            arrayOf<Any>(
                1L, "Рекс",
                ObservationType.TEMPERATURE.name,
                38.5, "Планове спостереження", "22.10.2026"
            )
        )

        val sql = """
            INSERT INTO observations
            (id, pet_name, type, value, note, recorded_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()

        examples.forEach { args ->
            db.execSQL(sql, args)
        }
    }
}
