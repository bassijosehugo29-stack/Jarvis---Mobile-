package br.com.jarvis.mobile

import android.app.Application
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import android.os.SystemClock
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelStore
import br.com.jarvis.mobile.actions.AppAction
import br.com.jarvis.mobile.experience.*
import br.com.jarvis.mobile.voice.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList
import org.json.JSONObject

/** Native events are fixtures; actual Handler timing/cancellation and production turn routing run
 * on Android. This does not claim validation of an acoustic microphone or Samsung recognizer.
 */
@RunWith(AndroidJUnit4::class)
class PreservedListeningTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private class Windows : RecognitionWindowFactory {
        override val available = true; override val onDevice = true
        val sessions = CopyOnWriteArrayList<Window>()
        override fun create(events: RecognitionEvents) = Window(events).also { sessions.add(it) }
    }
    private class Window(val events: RecognitionEvents) : RecognitionWindow {
        @Volatile var closed = false
        @Volatile var finalRequested = false
        override fun start() = events.ready()
        override fun requestFinal() { finalRequested = true }
        override fun close() {
            closed = true
            // Deliberately adversarial, synchronous late callback from a cancelled native window.
            events.error(SpeechRecognizer.ERROR_CLIENT)
        }
    }
    @Test fun threeSecondWhatsAppPauseIsOneRequestAndNeverAPrematureResponse() {
        val factory = Windows(); val replies = CopyOnWriteArrayList<String>(); val failures = CopyOnWriteArrayList<Int>()
        lateinit var input: AndroidSpeechInput
        var firstSpeechAt = 0L
        var continuationAt = 0L
        compose.runOnUiThread {
            input = AndroidSpeechInput(compose.activity, {}, replies::add, failures::add, factory = factory)
            input.start()
            firstSpeechAt = SystemClock.elapsedRealtime()
            factory.sessions[0].events.run { beginning(); partial("Jarvis, eu preciso que você abra o WhatsApp"); end(); final("Jarvis, eu preciso que você abra o WhatsApp", true) }
        }
        try {
            compose.waitUntil(3_000) { factory.sessions.size >= 2 }
            SystemClock.sleep((firstSpeechAt + 3_000 - SystemClock.elapsedRealtime()).coerceAtLeast(0))
            assertTrue(replies.isEmpty())
            compose.runOnUiThread {
                continuationAt = SystemClock.elapsedRealtime()
                factory.sessions.last().events.run { beginning(); partial("e procure uma conversa."); end(); final("e procure uma conversa.", true) }
            }
            assertTrue("The full requested pause must actually elapse", continuationAt - firstSpeechAt >= 3_000L)
            assertTrue(replies.isEmpty())
            compose.waitUntil(10_000) { replies.isNotEmpty() }
            assertEquals(listOf("Jarvis, eu preciso que você abra o WhatsApp e procure uma conversa."), replies.toList())
            compose.runOnUiThread { factory.sessions.first().events.final("Jarvis, eu preciso que você abra o WhatsApp", true) }
            assertEquals(1, replies.size); assertTrue(failures.isEmpty()); assertTrue(factory.sessions.all { it.closed })
            writeEvidence(continuationAt - firstSpeechAt, replies)
        } finally { compose.runOnUiThread { input.close() } }
    }
    @Test fun cancellationDropsPendingResultsAndAllLateCallbacks() {
        val factory = Windows(); val replies = CopyOnWriteArrayList<String>(); val failures = CopyOnWriteArrayList<Int>()
        lateinit var input: AndroidSpeechInput
        compose.runOnUiThread {
            input = AndroidSpeechInput(compose.activity, {}, replies::add, failures::add, factory = factory)
            input.start(); factory.sessions.first().events.run { beginning(); partial("Jarvis WhatsApp"); end(); final("Jarvis WhatsApp", true) }
            input.close()
            factory.sessions.first().events.run { ready(); beginning(); partial("atrasado"); final("atrasado", true); error(7) }
        }
        SystemClock.sleep(3_500)
        assertTrue(replies.isEmpty()); assertTrue(failures.isEmpty()); assertTrue(factory.sessions.all { it.closed })
    }
    @Test fun aTruncatedFinalIsKeptAsAnUnconfirmedDraftWithoutAnAction() {
        val factory = Windows(); val replies = CopyOnWriteArrayList<String>(); val failures = CopyOnWriteArrayList<Int>()
        val drafts = CopyOnWriteArrayList<ListeningDraft>(); lateinit var input: AndroidSpeechInput
        compose.runOnUiThread {
            input = AndroidSpeechInput(compose.activity, {}, replies::add, failures::add, onDraft = drafts::add, factory = factory)
            input.start()
            factory.sessions.first().events.run { beginning(); partial("Jarvis, eu preciso que você abra o WhatsApp e procure uma conversa."); end(); final("Jarvis, eu preciso que você abra o WhatsApp", true) }
        }
        assertTrue(replies.isEmpty()); assertEquals(1, failures.size)
        assertEquals(ListeningPhase.UNCONFIRMED, drafts.last().phase)
        assertEquals("Jarvis, eu preciso que você abra o WhatsApp e procure uma conversa.", drafts.last().text)
        compose.runOnUiThread { input.close() }
    }
    @Test fun explicitFastLaneStillHasAnImmediateFinalPath() {
        val factory = Windows(); val replies = CopyOnWriteArrayList<String>(); lateinit var input: AndroidSpeechInput
        compose.runOnUiThread {
            input = AndroidSpeechInput(compose.activity, {}, replies::add, { fail("Unexpected recognizer failure") },
                naturalPauses = false, factory = factory)
            input.start(); factory.sessions.first().events.final("Jarvis pare", true)
            assertEquals(listOf("Jarvis pare"), replies.toList())
            input.close()
        }
    }
    @Test fun officialAndroidRequestEnablesBrazilianPortuguesePartials() {
        val request = portugueseRecognitionIntent()
        assertEquals("pt-BR", request.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
        assertTrue(request.getBooleanExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false))
        assertTrue(request.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false))
    }
    @Test fun localDictationNeverFallsBackToANetworkFactory() {
        val replies = CopyOnWriteArrayList<String>(); val failures = CopyOnWriteArrayList<Int>()
        val networkOnly = object : RecognitionWindowFactory {
            override val available = true; override val onDevice = false
            override fun create(events: RecognitionEvents): RecognitionWindow = error("Must never open network dictation")
        }
        compose.runOnUiThread {
            val input = AndroidSpeechInput(compose.activity, {}, replies::add, failures::add,
                requireOnDevice = true, factory = networkOnly)
            assertFalse(input.available); input.start(); input.close()
        }
        assertTrue(replies.isEmpty()); assertEquals(1, failures.size)
    }
    @Test fun productionViewModelRetainsAnUnconfirmedRequestWithoutActionsOrSpeech() {
        val inst = InstrumentationRegistry.getInstrumentation(); val target = inst.targetContext
        android.os.ParcelFileDescriptor.AutoCloseInputStream(inst.uiAutomation.executeShellCommand(
            "pm grant " + target.packageName + " android.permission.RECORD_AUDIO")).use { it.readBytes() }
        val factory = Windows(); val actions = CopyOnWriteArrayList<AppAction>(); val store = ViewModelStore()
        lateinit var model: JarvisViewModel; lateinit var collector: Job
        inst.runOnMainSync {
            model = JarvisViewModel(target.applicationContext as Application, SpeechInputFactory { context, cb, local ->
                AndroidSpeechInput(context, cb.onReady, cb.onResult, cb.onFailure, cb.onLevel, local,
                    onDraft = cb.onDraft, factory = factory)
            })
            store.put("preserved-listening", model); model.onForeground()
            collector = CoroutineScope(Dispatchers.Main.immediate).launch { model.actions.collect { actions.add(it) } }
        }
        try {
            compose.waitUntil(8_000) { model.state.value.preferencesReady }
            inst.runOnMainSync { model.updateExperience { it.copy(mode = OperationMode.SILENT) }; model.start() }
            compose.waitUntil(8_000) { model.state.value.listening && factory.sessions.isNotEmpty() }
            inst.runOnMainSync {
                factory.sessions.first().events.run {
                    beginning(); partial("Jarvis, eu preciso que você abra o WhatsApp e procure uma conversa.")
                    end(); final("Jarvis, eu preciso que você abra o WhatsApp", true)
                }
            }
            assertFalse(model.state.value.enabled); assertFalse(model.state.value.speaking)
            assertEquals("Jarvis, eu preciso que você abra o WhatsApp e procure uma conversa.", model.state.value.heard)
            assertTrue(actions.isEmpty())
            assertTrue(model.state.value.notice.contains("transcrição foi preservada"))
            SystemClock.sleep(1_200)
            assertFalse(model.state.value.enabled); assertEquals(1, factory.sessions.size)
        } finally {
            inst.runOnMainSync { model.onBackground(); collector.cancel(); store.clear() }
            runBlocking { ExperienceStore(target).update { ExperienceSettings() } }
        }
    }
    private fun writeEvidence(pauseMillis: Long, replies: List<String>) {
        val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "preserved-listening-evidence.json")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/JARVIS-testes")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        requireNotNull(resolver.openOutputStream(uri)).bufferedWriter().use { out ->
            out.write(JSONObject().put("source", "Native ASR events fixture; production AndroidSpeechInput and real Handler, no acoustic microphone")
                .put("pause_ms", pauseMillis).put("request_count", replies.size)
                .put("complete_request", replies.single()).put("premature_request_count", 0)
                .put("cyberbot_original_verified", false).toString(2))
        }
        values.clear(); values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }
}
