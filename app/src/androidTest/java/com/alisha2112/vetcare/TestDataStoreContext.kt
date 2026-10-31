package com.alisha2112.vetcare

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

fun createTestDataStoreContext(): Context {

    val appContext =
        InstrumentationRegistry.getInstrumentation()
            .targetContext
            .applicationContext

    val testFilesDir = File(
        appContext.cacheDir,
        "vetcare_instrumented_test_files"
    ).apply {
        check(exists() || mkdirs()) {
            "Не вдалося створити тестову директорію DataStore"
        }
    }

    return object : ContextWrapper(appContext) {

        override fun getApplicationContext(): Context = this

        override fun getFilesDir(): File = testFilesDir
    }
}
