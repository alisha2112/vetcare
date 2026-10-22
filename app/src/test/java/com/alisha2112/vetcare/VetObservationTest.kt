
package com.alisha2112.vetcare

import com.alisha2112.vetcare.data.model.DaySummary
import com.alisha2112.vetcare.data.model.ObservationType
import com.alisha2112.vetcare.data.model.VetObservation
import com.alisha2112.vetcare.data.model.countByType
import com.alisha2112.vetcare.data.model.sleepQuality
import com.alisha2112.vetcare.data.model.summary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VetObservationTest {

    private val records = listOf(
        VetObservation(
            4, "Луна", ObservationType.SLEEP,
            8.0, "", "22.10.2026"
        ),
        VetObservation(
            3, "Рекс", ObservationType.PULSE,
            80.0, "", "22.10.2026"
        ),
        VetObservation(
            2, "Рекс", ObservationType.PULSE,
            100.0, "", "22.10.2026"
        ),
        VetObservation(
            1, "Луна", ObservationType.FEEDING,
            120.0, "", "22.10.2026"
        )
    )

    @Test
    fun sleepQualityUsesTeachingRanges() {
        assertEquals("Недостатньо", sleepQuality(5.9))
        assertEquals(
            "Норма (навчальний приклад)",
            sleepQuality(6.0)
        )
        assertEquals(
            "Норма (навчальний приклад)",
            sleepQuality(9.0)
        )
        assertEquals(
            "Забагато (навчальний приклад)",
            sleepQuality(9.1)
        )
        assertEquals(
            "Некоректне значення",
            sleepQuality(-1.0)
        )
        assertEquals(
            "Некоректне значення",
            sleepQuality(Double.NaN)
        )
    }

    @Test
    fun summaryAggregatesDemonstrationRecords() {
        val result = records.summary()

        assertEquals(
            DaySummary(
                totalFeedGrams = 120.0,
                averagePulse = 90.0,
                latestSleepHours = 8.0,
                observationCount = 4
            ),
            result
        )
    }

    @Test
    fun emptySummaryIsSafe() {
        val result = emptyList<VetObservation>().summary()

        assertEquals(0.0, result.totalFeedGrams, 0.0)
        assertNull(result.averagePulse)
        assertNull(result.latestSleepHours)
        assertEquals(0, result.observationCount)
    }

    @Test
    fun countsIncludeZeroForMissingTypes() {
        val counts = records.countByType()

        assertEquals(2, counts[ObservationType.PULSE])
        assertEquals(1, counts[ObservationType.SLEEP])
        assertEquals(1, counts[ObservationType.FEEDING])
        assertEquals(0, counts[ObservationType.TEMPERATURE])
    }

    @Test
    fun dataClassCopyPreservesOriginal() {
        val original = records.first()

        val modified = original.copy(
            note = "Відредагована примітка"
        )

        assertEquals("", original.note)
        assertEquals("Відредагована примітка", modified.note)
    }
}
