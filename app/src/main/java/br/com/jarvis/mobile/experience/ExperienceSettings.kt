package br.com.jarvis.mobile.experience

import br.com.jarvis.mobile.voice.VoiceProfile

enum class OperationMode(val title: String, val shortTitle: String, val detail: String) {
    SILENT("Modo Silencioso", "SILÊNCIO", "Sem som · uma escuta por toque"),
    ASSISTANT("Modo Assistente", "ASSISTENTE", "Voz e efeitos discretos · música opcional"),
    CINEMATIC("Modo Cinematográfico", "CINEMA", "Núcleo completo · ambiente original")
}

enum class CorePhase(val label: String) {
    INITIALIZING("INICIALIZANDO"), IDLE("EM ESPERA"), LISTENING("ESCUTANDO"),
    PROCESSING("PROCESSANDO"), RESPONDING("RESPONDENDO"), ERROR("ATENÇÃO")
}

/** Persistent preferences. Silent is a hard audio gate, regardless of individual switches. */
data class ExperienceSettings(
    val mode: OperationMode = OperationMode.ASSISTANT,
    val voiceEnabled: Boolean = true,
    val effectsEnabled: Boolean = true,
    val assistantMusic: Boolean = false,
    val cinematicMusic: Boolean = true,
    val voiceVolume: Float = 0.90f,
    val musicVolume: Float = 0.16f,
    val effectsVolume: Float = 0.25f,
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val enginePackage: String = "",
    val voiceName: String = "",
    val voiceProfile: VoiceProfile = VoiceProfile.OFFICIAL_A,
    val voiceInterruption: Boolean = false
) {
    val voiceAllowed get() = mode != OperationMode.SILENT && voiceEnabled && voiceVolume > 0f
    val musicEnabled get() = when (mode) {
        OperationMode.SILENT -> false
        OperationMode.ASSISTANT -> assistantMusic
        OperationMode.CINEMATIC -> cinematicMusic
    }
    fun withMusic(enabled: Boolean) = when (mode) {
        OperationMode.CINEMATIC -> copy(cinematicMusic = enabled)
        else -> copy(assistantMusic = enabled)
    }
    fun sanitized() = copy(
        voiceVolume = voiceVolume.safe(0.90f, 0f, 1f),
        musicVolume = musicVolume.safe(0.16f, 0f, 1f),
        effectsVolume = effectsVolume.safe(0.25f, 0f, 1f),
        speechRate = speechRate.safe(1.0f, 0.70f, 1.15f),
        pitch = if(voiceProfile==VoiceProfile.OFFICIAL_A)1.0f else pitch.safe(1.0f, 0.70f, 1.10f)
    )
}

private fun Float.safe(default: Float, min: Float, max: Float) = if (isFinite()) coerceIn(min, max) else default

data class SoundMix(val music: Float = 0f, val effects: Float = 0f)

/** Silence during capture avoids feeding the assistant's own sound back into SpeechRecognizer. */
fun soundMix(settings: ExperienceSettings, phase: CorePhase, foreground: Boolean): SoundMix {
    val s = settings.sanitized()
    if (!foreground || s.mode == OperationMode.SILENT || phase == CorePhase.LISTENING) return SoundMix()
    val duck = when (phase) {
        CorePhase.RESPONDING -> 0.12f
        CorePhase.INITIALIZING, CorePhase.PROCESSING -> 0.55f
        else -> 1f
    }
    return SoundMix(
        music = if (s.musicEnabled) s.musicVolume * duck else 0f,
        effects = if (s.effectsEnabled && phase != CorePhase.RESPONDING) s.effectsVolume else 0f
    )
}
