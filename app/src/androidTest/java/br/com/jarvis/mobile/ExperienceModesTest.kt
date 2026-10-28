package br.com.jarvis.mobile

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.os.SystemClock
import br.com.jarvis.mobile.experience.*
import org.junit.Assert.*
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class ExperienceModesTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun model() = ViewModelProvider(compose.activity)[JarvisViewModel::class.java]
    @After fun reset() {
        runBlocking { ExperienceStore(InstrumentationRegistry.getInstrumentation().targetContext).update { ExperienceSettings() } }
    }
    @Test fun threeModesSwitchImmediatelyWithoutStartingMicrophoneAndSilentRepliesInText() {
        compose.waitUntil(8_000) { model().state.value.preferencesReady }
        for (mode in listOf(OperationMode.CINEMATIC, OperationMode.ASSISTANT, OperationMode.SILENT)) {
            compose.onNodeWithTag("mode-${mode.name}").performClick()
            compose.runOnIdle {
                assertEquals(mode, model().state.value.experience.mode)
                assertFalse(model().state.value.listening)
                assertFalse(model().state.value.enabled)
                assertFalse(model().state.value.voicePlaying)
            }
            if (mode == OperationMode.CINEMATIC) {
                compose.waitUntil(5_000) { !model().state.value.initializing }
                compose.waitForIdle()
                SystemClock.sleep(400) // Let RenderThread latch the composed window before capture.
                saveUiScreenshot("jarvis-cinema.png", requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()))
            }
            if (mode == OperationMode.SILENT) {
                compose.waitForIdle()
                SystemClock.sleep(400)
                saveUiScreenshot("jarvis-silencioso.png", requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()))
            }
        }
        compose.onNodeWithTag("jarvis-message").performScrollTo().performTextInput("Jarvis")
        compose.onNodeWithText("Converse com o JARVIS").assertExists()
        compose.runOnUiThread { model().sendMessage("Jarvis") }
        compose.waitUntil(5_000) { model().state.value.reply == "Às suas ordens, senhor" }
        compose.runOnIdle {
            assertFalse(model().state.value.speaking); assertFalse(model().state.value.voicePlaying)
            assertFalse(model().state.value.enabled); assertFalse(model().state.value.listening)
        }
    }
    @Test fun movingActivityOutOfForegroundStopsVoiceAndMicrophone() {
        compose.waitUntil(8_000) { model().state.value.preferencesReady }
        compose.runOnUiThread {
            model().updateExperience { ExperienceSettings(enginePackage = InstrumentationRegistry.getInstrumentation().context.packageName) }
            model().testVoice()
            assertTrue("Real TTS initialization should be pending", model().state.value.speaking)
            model().onBackground()
        }
        compose.runOnIdle {
            assertFalse(model().state.value.enabled); assertFalse(model().state.value.listening)
            assertFalse(model().state.value.speaking); assertFalse(model().state.value.voicePlaying)
        }
    }
}
