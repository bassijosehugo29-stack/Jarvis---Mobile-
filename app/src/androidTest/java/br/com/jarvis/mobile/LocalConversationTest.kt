package br.com.jarvis.mobile

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performImeAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.jarvis.mobile.conversation.ModelStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalConversationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun verifiedModelGeneratesAnswersAndRemembersTheConversation() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val models = ModelStore(context)
        // Fixture is downloaded and SHA-256 checked by CI; shell stream is confined to androidTest.
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("cat /data/local/tmp/jarvis-conversation-model.gguf")).use {
            models.importStream(it)
        }
        assertTrue(models.state.value.ready)
        val first = ask("Meu nome é Rafael. Responda em português: qual é meu nome?")
        val second = ask("E como eu me chamo? Responda usando meu nome.")
        exportTranscript("Usuário: Meu nome é Rafael. Qual é meu nome?\nJARVIS: $first\n\nUsuário: E como eu me chamo?\nJARVIS: $second\n")
        assertTrue("First generated reply shown by Activity: $first", first.contains("Rafael", ignoreCase = true))
        assertTrue("Reply must use previous dialogue: $second", second.contains("Rafael", ignoreCase = true))
        assertFalse(first.contains("<|im_start|>"))
        assertFalse(second.contains("<|im_end|>"))
    }

    private fun ask(question: String): String {
        compose.waitUntil(35_000) { runCatching { compose.onNodeWithTag("jarvis-message").assertIsEnabled() }.isSuccess }
        compose.onNodeWithTag("jarvis-message").performScrollTo().performTextInput(question)
        // Use the keyboard Send action so IME/inset transitions cannot move a touch target.
        compose.onNodeWithTag("jarvis-message").performImeAction()
        compose.waitUntil(5_000) { runCatching {
            compose.onNodeWithTag("jarvis-heard").fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text }.contains(question)
        }.getOrDefault(false) }
        compose.waitUntil(100_000) { reply().contains("Rafael", ignoreCase = true) &&
            runCatching { compose.onNodeWithTag("jarvis-message").assertIsEnabled() }.isSuccess }
        return reply()
    }
    private fun reply(): String = runCatching {
        compose.onNodeWithTag("jarvis-reply").fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text }
    }.getOrDefault("")

    private fun exportTranscript(text: String) {
        if (Build.VERSION.SDK_INT < 29) return
        val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "conversa-local-testada.txt")
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/JARVIS-testes")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        requireNotNull(resolver.openOutputStream(uri)).bufferedWriter().use { it.write(text) }
        values.clear(); values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }
}
