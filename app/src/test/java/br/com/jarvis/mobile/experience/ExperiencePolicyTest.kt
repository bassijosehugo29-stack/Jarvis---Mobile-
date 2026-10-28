package br.com.jarvis.mobile.experience

import br.com.jarvis.mobile.JarvisState
import org.junit.Assert.*
import org.junit.Test

class ExperiencePolicyTest {
    @Test fun assistantDefaultsPreferBatteryAndVoice() {
        val s = ExperienceSettings()
        assertEquals(OperationMode.ASSISTANT, s.mode)
        assertTrue(s.voiceAllowed)
        assertEquals(0f, soundMix(s, CorePhase.IDLE, true).music, 0f)
        assertTrue(soundMix(s, CorePhase.IDLE, true).effects > 0f)
    }
    @Test fun silentHardGateCoversEveryStateEvenWithAllSwitchesOn() {
        val s = ExperienceSettings(mode = OperationMode.SILENT, assistantMusic = true, cinematicMusic = true)
        assertFalse(s.voiceAllowed)
        CorePhase.entries.forEach { assertEquals(SoundMix(), soundMix(s, it, true)) }
    }
    @Test fun cinematicDucksSpeechAndMutesCaptureAndBackground() {
        val s = ExperienceSettings(mode = OperationMode.CINEMATIC, musicVolume = 0.5f)
        val idle = soundMix(s, CorePhase.IDLE, true)
        val speaking = soundMix(s, CorePhase.RESPONDING, true)
        assertEquals(0.5f, idle.music, 0.001f)
        assertEquals(0.06f, speaking.music, 0.001f)
        assertEquals(0f, speaking.effects, 0f)
        assertEquals(SoundMix(), soundMix(s, CorePhase.LISTENING, true))
        CorePhase.entries.forEach { assertEquals(SoundMix(), soundMix(s, it, false)) }
    }
    @Test fun channelsAreIndependentAndMusicPreferenceIsPerMode() {
        val s = ExperienceSettings(mode = OperationMode.CINEMATIC, voiceEnabled = false, effectsEnabled = false)
        assertFalse(s.voiceAllowed)
        assertTrue(soundMix(s, CorePhase.IDLE, true).music > 0f)
        assertEquals(0f, soundMix(s, CorePhase.IDLE, true).effects, 0f)
        assertFalse(s.copy(mode = OperationMode.ASSISTANT).musicEnabled)
        assertFalse(s.withMusic(false).musicEnabled)
        assertTrue(s.withMusic(false).copy(mode = OperationMode.ASSISTANT).withMusic(true).musicEnabled)
    }
    @Test fun invalidPersistedNumbersAreBoundedAndFinite() {
        val s = ExperienceSettings(voiceVolume = Float.NaN, musicVolume = 8f, effectsVolume = -1f,
            speechRate = Float.POSITIVE_INFINITY, pitch = 0f).sanitized()
        assertEquals(0.90f, s.voiceVolume, 0f); assertEquals(1f, s.musicVolume, 0f)
        assertEquals(0f, s.effectsVolume, 0f); assertEquals(0.90f, s.speechRate, 0f)
        assertEquals(0.70f, s.pitch, 0f)
    }
    @Test fun coreFollowsPlaybackRatherThanPretendingSynthesisIsAudible() {
        assertEquals(CorePhase.INITIALIZING, JarvisState().corePhase)
        val base = JarvisState(initializing = false)
        assertEquals(CorePhase.IDLE, base.corePhase)
        assertEquals(CorePhase.LISTENING, base.copy(listening = true).corePhase)
        assertEquals(CorePhase.PROCESSING, base.copy(thinking = true).corePhase)
        assertEquals(CorePhase.PROCESSING, base.copy(speaking = true).corePhase)
        assertEquals(CorePhase.RESPONDING, base.copy(speaking = true, voicePlaying = true).corePhase)
        assertEquals(CorePhase.ERROR, base.copy(visualError = true, voicePlaying = true).corePhase)
    }
}
