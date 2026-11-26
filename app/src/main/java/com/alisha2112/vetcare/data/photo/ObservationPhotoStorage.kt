package com.alisha2112.vetcare.data.photo

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

data class CameraPhotoTarget(
    val relativePath: String,
    val uri: Uri
)

class ObservationPhotoStorage(context: Context) {

    private val appContext =
        context.applicationContext

    companion object {
        private const val PHOTO_DIRECTORY =
            "observation_photos"

        private val FILE_NAME_PATTERN =
            Regex("[0-9a-f-]{36}\\.[a-z0-9]{1,8}")
    }

    private val photoDirectory: File
        get() = File(
            appContext.filesDir,
            PHOTO_DIRECTORY
        )

    private fun ensureDirectory(): File {
        val directory = photoDirectory

        if (!directory.isDirectory &&
            !directory.mkdirs()
        ) {
            throw IOException(
                "Не вдалося створити папку фотографій"
            )
        }

        return directory
    }

    private fun createPhotoFile(
        extension: String
    ): File {
        val directory = ensureDirectory()

        val file = File(
            directory,
            "${UUID.randomUUID()}.$extension"
        )

        if (!file.createNewFile()) {
            throw IOException(
                "Не вдалося створити файл фотографії"
            )
        }

        return file
    }

    private fun relativePath(file: File): String =
        "$PHOTO_DIRECTORY/${file.name}"

    private fun resolveFile(
        path: String?
    ): File? {
        if (path.isNullOrBlank()) return null

        val prefix = "$PHOTO_DIRECTORY/"

        if (!path.startsWith(prefix)) {
            return null
        }

        val fileName = path.removePrefix(prefix)

        if (!FILE_NAME_PATTERN.matches(fileName)) {
            return null
        }

        val file = File(photoDirectory, fileName)

        return file.takeIf {
            it.canonicalFile.parentFile ==
                photoDirectory.canonicalFile
        }
    }

    // Створення файлу та URI для системної камери.
    fun createCameraTarget(): CameraPhotoTarget {
        val file = createPhotoFile("jpg")

        return try {
            val uri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                file
            )

            CameraPhotoTarget(
                relativePath = relativePath(file),
                uri = uri
            )
        } catch (exception: Exception) {
            file.delete()
            throw exception
        }
    }

    // Копіювання вибраного фото з галереї
    // у приватне сховище VetCare.
    suspend fun importPhoto(
        sourceUri: Uri
    ): String = withContext(Dispatchers.IO) {

        val mimeType =
            appContext.contentResolver.getType(sourceUri)

        if (
            mimeType != null &&
            !mimeType.startsWith("image/")
        ) {
            throw IOException(
                "Вибраний файл не є зображенням"
            )
        }

        val extension = mimeType
            ?.let {
                MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(it)
            }
            ?.lowercase()
            ?.takeIf {
                it.matches(Regex("[a-z0-9]{1,8}"))
            }
            ?: "img"

        val destination = createPhotoFile(extension)

        try {
            val input = appContext.contentResolver
                .openInputStream(sourceUri)
                ?: throw IOException(
                    "Не вдалося відкрити фотографію"
                )

            input.use { source ->
                destination.outputStream().use { output ->
                    source.copyTo(output)
                }
            }

            if (destination.length() == 0L) {
                throw IOException(
                    "Фотографія містить порожні дані"
                )
            }

            relativePath(destination)

        } catch (exception: Exception) {
            destination.delete()
            throw exception
        }
    }

    // Отримання збереженої непорожньої фотографії.
    fun findPhoto(path: String?): File? {
        val file = resolveFile(path)
            ?: return null

        return file.takeIf {
            it.isFile && it.length() > 0L
        }
    }

    fun isPhotoReady(path: String?): Boolean =
        findPhoto(path) != null

    // Видалення непотрібної або скасованої фотографії.
    fun deletePhoto(path: String?): Boolean {
        val file = resolveFile(path)
            ?: return false

        return !file.exists() || file.delete()
    }
}
