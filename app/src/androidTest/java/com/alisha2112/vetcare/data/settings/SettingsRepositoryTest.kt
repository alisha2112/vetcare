package com.alisha2112.vetcare.data.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import com.alisha2112.vetcare.createTestDataStoreContext

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {

    private val repository by lazy {
        SettingsRepository(
            createTestDataStoreContext()
        )
    }


    @Test
    fun userNameIsSavedAndTrimmed() = runBlocking {
        repository.setUserName("  Тестова користувачка  ")

        val settings = repository.settings.first()

        assertEquals(
            "Тестова користувачка",
            settings.userName
        )

        repository.setUserName("   ")

        val afterBlankInput = repository.settings.first()

        assertEquals(
            "Тестова користувачка",
            afterBlankInput.userName
        )
    }

    @Test
    fun themeModeIsPersisted() = runBlocking {
        repository.setThemeMode(ThemeMode.DARK)

        assertEquals(
            ThemeMode.DARK,
            repository.settings.first().themeMode
        )

        repository.setThemeMode(ThemeMode.LIGHT)

        assertEquals(
            ThemeMode.LIGHT,
            repository.settings.first().themeMode
        )
    }
}
