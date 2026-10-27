package br.com.jarvis.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.jarvis.mobile.ui.JarvisScreen
import br.com.jarvis.mobile.ui.JarvisTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JarvisScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun controlsRemainAvailableWithoutMicrophonePermission() {
        var activations = 0
        var chats = 0
        var alarms = 0
        compose.setContent {
            JarvisTheme {
                JarvisScreen(JarvisState(), { activations++ }, { chats++ }, { alarms++ }, {})
            }
        }
        compose.onNodeWithText("ATIVAR JARVIS").performScrollTo().performClick()
        compose.onNodeWithText("Abrir ChatGPT").performScrollTo().performClick()
        compose.onNodeWithText("Criar alarme").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, activations)
            assertEquals(1, chats)
            assertEquals(1, alarms)
        }
    }
    @Test fun activeScreenOffersStopButton() {
        compose.setContent { JarvisTheme { JarvisScreen(JarvisState(enabled = true), {}, {}, {}, {}) } }
        compose.onNodeWithText("DESATIVAR ESCUTA").performScrollTo().assertIsDisplayed()
    }
}
