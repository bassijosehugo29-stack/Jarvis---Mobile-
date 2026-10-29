package br.com.jarvis.mobile

import android.app.Application
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.jarvis.mobile.conversation.*
import br.com.jarvis.mobile.experience.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@RunWith(AndroidJUnit4::class)
class ResponseCancellationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private class DelayedProvider : ConversationClient {
        override val ready = true
        val waiting = CopyOnWriteArrayList<Continuation<String>>()
        override suspend fun answer(message: String): String = suspendCoroutine { waiting.add(it) }
        override fun cancel() = Unit
        override fun clear() = Unit
        override fun release() = Unit
    }
    @Test fun aCancelledProviderReplyCannotSpeakOverANewerTurn() = scenario(false)
    @Test fun aLateProviderErrorCannotReplaceTheCurrentAnswer() = scenario(true)
    private fun scenario(error: Boolean) {
        val inst = InstrumentationRegistry.getInstrumentation(); val target = inst.targetContext
        val provider = DelayedProvider(); val store = ViewModelStore(); lateinit var model: JarvisViewModel
        inst.runOnMainSync {
            model = JarvisViewModel(target.applicationContext as Application, conversationFactory = ConversationFactory { provider })
            store.put("cancelled-replies", model); model.onForeground()
        }
        try {
            compose.waitUntil(8_000) { model.state.value.preferencesReady }
            inst.runOnMainSync {
                model.updateExperience { it.copy(mode = OperationMode.SILENT) }
                model.sendMessage("Gostaria de conversar sobre a primeira pergunta")
            }
            compose.waitUntil(5_000) { provider.waiting.size == 1 && model.state.value.thinking }
            inst.runOnMainSync { model.stop(); model.sendMessage("Gostaria de conversar sobre a segunda pergunta") }
            compose.waitUntil(5_000) { provider.waiting.size == 2 && model.state.value.thinking }
            inst.runOnMainSync {
                if (error) provider.waiting[0].resumeWithException(IllegalStateException("fixture error"))
                else provider.waiting[0].resume("Resposta antiga que deve ser descartada")
            }
            compose.waitForIdle()
            assertTrue(model.state.value.thinking)
            assertFalse(model.state.value.reply.contains("Resposta antiga"))
            inst.runOnMainSync { provider.waiting[1].resume("Esta é a resposta atual") }
            compose.waitUntil(5_000) { !model.state.value.thinking && model.state.value.reply == "Esta é a resposta atual" }
            assertFalse(model.state.value.speaking); assertFalse(model.state.value.enabled)
        } finally {
            inst.runOnMainSync { model.onBackground(); store.clear() }
            runBlocking { ExperienceStore(target).update { ExperienceSettings() } }
        }
    }
}
