package br.com.jarvis.mobile

import br.com.jarvis.mobile.voice.VoiceEngine
import br.com.jarvis.mobile.voice.VoiceOption
import br.com.jarvis.mobile.experience.*

data class JarvisState(
    val enabled: Boolean = false,
    val listening: Boolean = false,
    val speaking: Boolean = false,
    val status: String = "Sistema pronto",
    val heard: String = "",
    val reply: String = "Às suas ordens, senhor. Ative o núcleo para falar ou escreva uma mensagem.",
    val voiceInfo: String = "Microfone desligado • escuta apenas com o app visível",
    val notice: String = "",
    val audioLevel: Float = 0f,
    val speechLevel: Float? = null,
    val interruptListening: Boolean = false,
    val interruptionInfo: String = "Interrupção por voz opcional nos ajustes",
    val thinking: Boolean = false,
    val working: Boolean = false,
    val speechInfo: String = "Use Testar voz nos ajustes para conferir o som.",
    val voiceIssue: String = "",
    val privateReply: Boolean = false,
    val privateDictation: Boolean = false,
    val modelReady: Boolean = false,
    val modelBusy: Boolean = false,
    val modelInfo: String = "Prepare a conversa offline nos ajustes • download gratuito de 491 MB",
    val experience: ExperienceSettings = ExperienceSettings(),
    val preferencesReady: Boolean = false,
    val initializing: Boolean = true,
    val visualError: Boolean = false,
    val voicePlaying: Boolean = false,
    val speechProgress: Float = 0f,
    val speechPulse: Int = 0,
    val engines: List<VoiceEngine> = emptyList(),
    val voices: List<VoiceOption> = emptyList()
) {
    val corePhase get() = when {
        visualError -> CorePhase.ERROR
        initializing -> CorePhase.INITIALIZING
        listening -> CorePhase.LISTENING
        voicePlaying -> CorePhase.RESPONDING
        thinking || speaking || working -> CorePhase.PROCESSING
        else -> CorePhase.IDLE
    }
    val soundPhase get() = when {
        interruptListening -> CorePhase.LISTENING
        listening -> CorePhase.LISTENING
        speaking -> CorePhase.RESPONDING
        thinking || working -> CorePhase.PROCESSING
        else -> corePhase
    }
}

