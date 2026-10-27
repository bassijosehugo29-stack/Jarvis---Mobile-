package br.com.jarvis.mobile

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class MainActivityVisualTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun launcherStartsWithVisibleHeaderAndMicrophoneOff() {
        compose.onNodeWithTag("jarvis-header").assertIsDisplayed()
        compose.onNodeWithContentDescription("Ativar JARVIS").assertIsDisplayed()
        compose.onNodeWithContentDescription("Configurações").assertIsDisplayed()
        compose.waitForIdle()
        val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        assertNotNull(screenshot)
        saveUiScreenshot("jarvis-home.png", screenshot!!)
    }
}

internal fun saveUiScreenshot(name: String, bitmap: Bitmap) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val directory = File(requireNotNull(context.getExternalFilesDir(null)), "screenshots")
    check(directory.exists() || directory.mkdirs())
    FileOutputStream(File(directory, name)).use {
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
    }
}
