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

    @Test fun personalTextCannotUseOnlineVoiceEvenAfterEngineHasBeenWarmed() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val control=android.net.Uri.parse("content://br.com.jarvis.mobile.test.tts-fixture")
        instrumentation.targetContext.contentResolver.call(control,"network",null,null)
        lateinit var output:SpeechOutput
        var created=false
        val publicDone=CountDownLatch(1);val privateDone=CountDownLatch(1)
        val publicResult=AtomicReference<SpeechResult>();val privateResult=AtomicReference<SpeechResult>()
        try{
            instrumentation.runOnMainSync{
                output=SpeechOutput(compose.activity,{},listOf(instrumentation.context.packageName))
                created=true
                output.speak("Frase pública de teste"){publicResult.set(it);publicDone.countDown()}
            }
            assertTrue(publicDone.await(25,TimeUnit.SECONDS));assertTrue(publicResult.get() is SpeechResult.Completed)
            instrumentation.runOnMainSync{output.speak("Dado pessoal de teste",true){privateResult.set(it);privateDone.countDown()}}
            assertTrue(privateDone.await(10,TimeUnit.SECONDS));assertTrue(privateResult.get().toString(),privateResult.get() is SpeechResult.TextOnly)
        }finally{instrumentation.targetContext.contentResolver.call(control,"local",null,null);if(created)instrumentation.runOnMainSync{output.close()}}
    }

    @Test fun personalTextUsesInstalledLocalVoiceAndProducesRealPcm() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        instrumentation.targetContext.contentResolver.call(android.net.Uri.parse("content://br.com.jarvis.mobile.test.tts-fixture"),"local",null,null)
        val done=CountDownLatch(1);val result=AtomicReference<SpeechResult>()
        lateinit var output:SpeechOutput
        instrumentation.runOnMainSync{
            output=SpeechOutput(compose.activity,{},listOf(instrumentation.context.packageName))
            output.speak("Relatório pessoal de teste",true){result.set(it);done.countDown()}
        }
        try{assertTrue(done.await(25,TimeUnit.SECONDS));assertTrue(result.get() is SpeechResult.Completed)
            assertTrue((result.get() as SpeechResult.Completed).audioBytes>0)
        }finally{instrumentation.runOnMainSync{output.close()}}
    }

    @Test fun selectedVoiceRatePitchAndPlaybackCallbacksReachRealAndroidTts() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val result = AtomicReference<SpeechResult>()
        val done = CountDownLatch(1)
        val started = CountDownLatch(1)
        val range = CountDownLatch(1)
        lateinit var output: SpeechOutput
        instrumentation.runOnMainSync {
            output = SpeechOutput(compose.activity, {}, listOf(instrumentation.context.packageName),
                onPlayback = { if (it) started.countDown() }, onRange = { _, _, _ -> range.countDown() })
            output.configure(ExperienceSettings(speechRate = 0.91f, pitch = 0.83f,
                voiceName = "jarvis-fixture-pt-PT", voiceVolume = 0.6f))
            output.speak("Teste de perfil vocal do JARVIS") { result.set(it); done.countDown() }
        }
        try {
            assertTrue(done.await(25, TimeUnit.SECONDS))
            assertTrue(result.get().toString(), result.get() is SpeechResult.Completed)
            assertTrue(started.await(1, TimeUnit.SECONDS))
            assertTrue("Engine's word range must reach UI callback", range.await(1, TimeUnit.SECONDS))
        } finally { instrumentation.runOnMainSync { output.close() } }
    }

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
