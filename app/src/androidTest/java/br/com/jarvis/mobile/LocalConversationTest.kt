package br.com.jarvis.mobile

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.jarvis.mobile.conversation.LocalConversation
import br.com.jarvis.mobile.conversation.ModelStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalConversationTest {
    @Test fun verifiedModelGeneratesAnswersAndRemembersTheConversation() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val models = ModelStore(context)
        // Fixture is downloaded and SHA-256 checked by CI; shell stream is confined to androidTest.
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("cat /data/local/tmp/jarvis-conversation-model.gguf")).use {
            models.importStream(it)
        }
        assertTrue(models.state.value.ready)
        val conversation = LocalConversation(models)
        try {
            val first = conversation.answer("Meu nome é Rafael. Responda em português: qual é meu nome?")
            val second = conversation.answer("E como eu me chamo? Responda usando meu nome.")
            exportTranscript("Usuário: Meu nome é Rafael. Qual é meu nome?\nJARVIS: $first\n\nUsuário: E como eu me chamo?\nJARVIS: $second\n")
            assertTrue("First generated reply: $first", first.contains("Rafael", ignoreCase = true))
            assertTrue("Generated reply must use previous dialogue: $second", second.contains("Rafael", ignoreCase = true))
            assertFalse(first.contains("<|im_start|>"))
            assertFalse(second.contains("<|im_end|>"))
        } finally { conversation.release() }
    }

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
