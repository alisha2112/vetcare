package com.alisha2112.vetcare.data.export

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.alisha2112.vetcare.data.model.ObservationType
import com.alisha2112.vetcare.data.model.VetObservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ObservationCsvExporterTest {

    private val exporter by lazy {
        val context =
            InstrumentationRegistry.getInstrumentation()
                .targetContext

        ObservationCsvExporter(context)
    }

    @Test
    fun emptyListProducesOnlyHeader() {
        val csv = exporter.toCsv(emptyList())

        assertEquals(
            "id,petName,type,value,note,recordedAt\n",
            csv
        )
    }

    @Test
    fun csvEscapesCommasQuotesAndNewLines() {
        val record = VetObservation(
            id = 1L,
            petName = "Луна, \"Зірка\"",
            type = ObservationType.TEMPERATURE,
            value = 38.5,
            note = "Рядок 1,\nРядок \"2\"",
            recordedAt = "29.10.2026"
        )

        val csv = exporter.toCsv(listOf(record))

        assertTrue(
            csv.startsWith(
                "id,petName,type,value,note,recordedAt\n"
            )
        )

        assertTrue(
            csv.contains("\"Луна, \"\"Зірка\"\"\"")
        )

        assertTrue(
            csv.contains(
                "\"Рядок 1,\nРядок \"\"2\"\"\""
            )
        )

        assertTrue(csv.contains("TEMPERATURE"))
        assertTrue(csv.contains("38.5"))
        assertTrue(csv.contains("29.10.2026"))
    }
}
