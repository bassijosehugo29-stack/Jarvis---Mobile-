package br.com.jarvis.mobile

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.jarvis.mobile.audio.*
import br.com.jarvis.mobile.experience.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SoundscapeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun realAudioTrackPlaysDucksAndStopsForSilentCaptureAndBackground() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val focus = AssistantAudioFocus(context)
        val sounds = Soundscape(focus)
        val cinema = ExperienceSettings(mode = OperationMode.CINEMATIC, musicVolume = 0.5f)
        try {
            withContext(Dispatchers.Main) { sounds.prepare(); sounds.update(cinema, CorePhase.IDLE, true) }
            SystemClock.sleep(350)
            instrumentation.runOnMainSync {
                val playing = sounds.snapshot()
                assertTrue(playing.ready); assertTrue(playing.musicPlaying)
                assertTrue("AudioTrack must advance real frames", playing.musicFrames > 0)
                assertEquals(0.5f, playing.musicGain, 0.01f)
                sounds.update(cinema, CorePhase.RESPONDING, true)
            }
            SystemClock.sleep(300)
            instrumentation.runOnMainSync {
                assertEquals(0.06f, sounds.snapshot().musicGain, 0.01f)
                assertFalse(sounds.snapshot().effectsPlaying)
                sounds.update(cinema, CorePhase.PROCESSING, true)
                assertTrue(sounds.snapshot().effectsPlaying)
                sounds.update(cinema.copy(mode = OperationMode.SILENT), CorePhase.PROCESSING, true)
                assertFalse(sounds.snapshot().musicPlaying); assertFalse(sounds.snapshot().effectsPlaying)
                sounds.update(cinema, CorePhase.IDLE, true)
                sounds.update(cinema, CorePhase.LISTENING, true)
                assertFalse(sounds.snapshot().musicPlaying); assertFalse(sounds.snapshot().effectsPlaying)
                sounds.update(cinema, CorePhase.IDLE, true)
                sounds.update(cinema, CorePhase.IDLE, false)
                assertFalse(sounds.snapshot().musicPlaying); assertFalse(sounds.snapshot().effectsPlaying)
            }
        } finally { instrumentation.runOnMainSync { sounds.close(); focus.close() } }
    }
}
