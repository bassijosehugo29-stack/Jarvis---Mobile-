package br.com.jarvis.mobile

import android.media.AudioManager
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.jarvis.mobile.voice.SpeechOutput
import br.com.jarvis.mobile.voice.SpeechResult
import br.com.jarvis.mobile.voice.VoiceStatus
import br.com.jarvis.mobile.experience.ExperienceSettings
import br.com.jarvis.mobile.experience.OperationMode
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class SpeechOutputTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun silentAndVoiceDisabledNeverInitializeOrSynthesize() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var statuses = 0
        instrumentation.runOnMainSync {
            val output = SpeechOutput(compose.activity, { statuses++ }, listOf("intentionally.invalid.engine"))
            for (settings in listOf(ExperienceSettings(mode = OperationMode.SILENT),
                ExperienceSettings(voiceEnabled = false), ExperienceSettings(voiceVolume = 0f))) {
                output.configure(settings)
                var result: SpeechResult? = null
                output.speak("Isto deve permanecer em silêncio.") { result = it }
                assertEquals(SpeechResult.Suppressed, result)
            }
            assertEquals(0, statuses)
            output.close()
        }
    }

    @Test fun producesAudioWithPortugueseFallbackAndMediaVolumeRouting() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val result = AtomicReference<SpeechResult>()
        val selected = AtomicReference<VoiceStatus>()
        val done = CountDownLatch(1)
        lateinit var output: SpeechOutput
        instrumentation.runOnMainSync {
            assertEquals(AudioManager.STREAM_MUSIC, compose.activity.volumeControlStream)
            output = SpeechOutput(compose.activity, { if (it.ready) selected.set(it) }, listOf(instrumentation.context.packageName))
            output.speak("Às suas ordens, senhor") { result.set(it); done.countDown() }
        }
        try {
            assertTrue("TTS callback must complete", done.await(25, TimeUnit.SECONDS))
            assertTrue(result.get().toString(), result.get() is SpeechResult.Completed)
            assertTrue("Engine produced nonzero PCM", (result.get() as SpeechResult.Completed).audioBytes > 0)
            assertEquals("pt-PT", selected.get().locale)
        } finally { instrumentation.runOnMainSync { output.close() } }
    }
    @Test fun synthesisFailureBecomesVisibleFailureInsteadOfSilentSuccess() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val result = AtomicReference<SpeechResult>()
        val status = AtomicReference<VoiceStatus>()
        val done = CountDownLatch(1)
        lateinit var output: SpeechOutput
        instrumentation.runOnMainSync {
            output = SpeechOutput(compose.activity, { status.set(it) }, listOf(instrumentation.context.packageName))
            output.speak("falha simulada") { result.set(it); done.countDown() }
        }
        try {
            assertTrue(done.await(25, TimeUnit.SECONDS))
            assertTrue(result.get() is SpeechResult.Failed)
            assertTrue(status.get().failure)
            assertTrue(status.get().message.contains("voz"))
        } finally { instrumentation.runOnMainSync { output.close() } }
    }
}
