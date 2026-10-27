package br.com.jarvis.mobile

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
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
            JarvisTheme { JarvisScreen(JarvisState(), { activations++ }, { chats++ }, { alarms++ }, {}) }
        }
        compose.onNodeWithContentDescription("Ativar JARVIS").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Abrir ChatGPT").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Criar alarme").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, activations)
            assertEquals(1, chats)
            assertEquals(1, alarms)
        }
    }

    @Test fun activeScreenOffersPauseControl() {
        compose.setContent { JarvisTheme { JarvisScreen(JarvisState(enabled = true), {}, {}, {}, {}) } }
        compose.onNodeWithContentDescription("Pausar escuta").performScrollTo().assertIsDisplayed()
    }

    @Test fun guidanceAndPermissionActionAreAvailableFromSettings() {
        var settings = 0
        compose.setContent { JarvisTheme { JarvisScreen(JarvisState(), {}, {}, {}, { settings++ }) } }
        compose.onNodeWithContentDescription("Configurações").performClick()
        compose.onNodeWithText("Voz e permissões").assertIsDisplayed()
        compose.onNodeWithText("Abrir permissões do aplicativo").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, settings) }
    }

    @Test fun headerDoesNotScrollOutWithLongConversation() {
        compose.setContent {
            JarvisTheme {
                JarvisScreen(JarvisState(heard = "Jarvis, criar um alarme. ".repeat(20),
                    reply = "Confirme o horário no relógio, senhor."), {}, {}, {}, {})
            }
        }
        val before = compose.onNodeWithTag("jarvis-header").fetchSemanticsNode().boundsInRoot.top
        compose.onNodeWithContentDescription("Criar alarme").performScrollTo()
        compose.onNodeWithTag("jarvis-header").assertIsDisplayed()
        val after = compose.onNodeWithTag("jarvis-header").fetchSemanticsNode().boundsInRoot.top
        assertEquals(before, after, 0.1f)
    }

    @Test fun largeTextKeepsActionsReachable() {
        compose.setContent {
            val current = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(current.density, 1.5f)) {
                JarvisTheme { JarvisScreen(JarvisState(), {}, {}, {}, {}) }
            }
        }
        compose.onNodeWithTag("jarvis-header").assertIsDisplayed()
        compose.onNodeWithContentDescription("Ativar JARVIS").assertIsDisplayed()
        compose.waitForIdle()
        saveUiScreenshot("jarvis-large-text.png", compose.onRoot().captureToImage().asAndroidBitmap())
        compose.onNodeWithContentDescription("Criar alarme").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("jarvis-header").assertIsDisplayed()
    }
}
