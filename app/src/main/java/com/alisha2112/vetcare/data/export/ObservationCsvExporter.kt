package com.alisha2112.vetcare.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.alisha2112.vetcare.data.model.VetObservation
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class ObservationCsvExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun export(
        records: List<VetObservation>
    ): Uri = withContext(Dispatchers.IO) {

        val exportDirectory = File(
            context.filesDir,
            "exports"
        )

        if (!exportDirectory.exists() &&
            !exportDirectory.mkdirs()
        ) {
            throw IOException(
                "Не вдалося створити папку експорту"
            )
        }

        val csvFile = File(
            exportDirectory,
            "vetcare_observations.csv"
        )

        csvFile.writeText(
            text = toCsv(records),
            charset = Charsets.UTF_8
        )

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            csvFile
        )
    }

    fun toCsv(records: List<VetObservation>): String =
        buildString {
            appendLine(
                "id,petName,type,value,note,recordedAt"
            )

            records.forEach { record ->
                val columns = listOf(
                    record.id.toString(),
                    record.petName,
                    record.type.name,
                    record.value.toString(),
                    record.note,
                    record.recordedAt
                )

                appendLine(
                    columns.joinToString(",") { column ->
                        escapeCsv(column)
                    }
                )
            }
        }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.any {
            it == ',' ||
                    it == '"' ||
                    it == '\n' ||
                    it == '\r'
        }

        if (!needsQuotes) return value

        return "\"" +
                value.replace("\"", "\"\"") +
                "\""
    }
}
