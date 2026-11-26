package com.alisha2112.vetcare.data.photo

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ObservationPhotoStorageTest {

    private val context =
        InstrumentationRegistry.getInstrumentation()
            .targetContext

    private val storage =
        ObservationPhotoStorage(context)

    @Test
    fun createsValidCameraTarget() {
        val target = storage.createCameraTarget()

        try {
            assertEquals(
                "content",
                target.uri.scheme
            )

            assertEquals(
                "${context.packageName}.fileprovider",
                target.uri.authority
            )

            assertTrue(
                target.relativePath.startsWith(
                    "observation_photos/"
                )
            )

            // Порожній файл ще не є готовим фото.
            assertFalse(
                storage.isPhotoReady(target.relativePath)
            )

        } finally {
            storage.deletePhoto(target.relativePath)
        }
    }

    @Test
    fun createsUniqueFileNames() {
        val first = storage.createCameraTarget()
        val second = storage.createCameraTarget()

        try {
            assertNotEquals(
                first.relativePath,
                second.relativePath
            )

            assertNotEquals(
                first.uri,
                second.uri
            )
        } finally {
            storage.deletePhoto(first.relativePath)
            storage.deletePhoto(second.relativePath)
        }
    }

    @Test
    fun importsAndDeletesPhoto() = runBlocking {
        val target = storage.createCameraTarget()
        var importedPath: String? = null

        try {
            // Створюємо справжнє тестове JPEG-зображення.
            val bitmap = Bitmap.createBitmap(
                16,
                16,
                Bitmap.Config.ARGB_8888
            )

            try {
                val output = requireNotNull(
                    context.contentResolver
                        .openOutputStream(target.uri)
                )

                output.use {
                    assertTrue(
                        bitmap.compress(
                            Bitmap.CompressFormat.JPEG,
                            90,
                            it
                        )
                    )
                }
            } finally {
                bitmap.recycle()
            }

            assertTrue(
                storage.isPhotoReady(target.relativePath)
            )

            val copiedPath =
                storage.importPhoto(target.uri)

            importedPath = copiedPath

            val original =
                storage.findPhoto(target.relativePath)

            val imported =
                storage.findPhoto(copiedPath)

            assertNotNull(original)
            assertNotNull(imported)

            assertNotEquals(
                target.relativePath,
                copiedPath
            )

            assertArrayEquals(
                requireNotNull(original).readBytes(),
                requireNotNull(imported).readBytes()
            )

            assertTrue(
                storage.deletePhoto(copiedPath)
            )

            assertNull(
                storage.findPhoto(copiedPath)
            )

        } finally {
            storage.deletePhoto(target.relativePath)
            importedPath?.let {
                storage.deletePhoto(it)
            }
        }
    }

    @Test
    fun rejectsInvalidPaths() {
        assertNull(
            storage.findPhoto(null)
        )

        assertNull(
            storage.findPhoto(
                "observation_photos/../secret.txt"
            )
        )

        assertNull(
            storage.findPhoto(
                "/etc/passwd"
            )
        )

        assertFalse(
            storage.deletePhoto(
                "observation_photos/../secret.txt"
            )
        )
    }
}
