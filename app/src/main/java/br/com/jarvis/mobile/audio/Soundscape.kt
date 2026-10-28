package br.com.jarvis.mobile.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import br.com.jarvis.mobile.experience.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SoundscapeSnapshot(
    val ready: Boolean, val musicPlaying: Boolean, val effectsPlaying: Boolean,
    val musicGain: Float, val effectsGain: Float, val musicFrames: Int
)

/** Static PCM loops: no decoding/network work during playback. Owned only by the visible Activity. */
class Soundscape(private val focus: AssistantAudioFocus, private val onError: (String) -> Unit = {}) {
    private val main = Handler(Looper.getMainLooper())
    private var ambientPcm: ShortArray? = null
    private var cues: Map<SoundCue, ShortArray> = emptyMap()
    private var music: AudioTrack? = null
    private var effect: AudioTrack? = null
    private var processing: AudioTrack? = null
    private var musicGain = 0f
    private var effectsGain = 0f
    private var settings = ExperienceSettings()
    private var phase = CorePhase.IDLE
    private var foreground = false
    private var closed = false
    private var previousPhase: CorePhase? = null
    private var effectEnd: Runnable? = null
    private var ramp: Runnable? = null

    suspend fun prepare() {
        if (closed || ambientPcm != null) return
        val bank = withContext(Dispatchers.Default) {
            OriginalScore.ambient() to SoundCue.entries.associateWith(OriginalScore::cue)
        }
        if (closed) return
        ambientPcm = bank.first; cues = bank.second
        update(settings, phase, foreground)
    }

    fun update(settings: ExperienceSettings, phase: CorePhase, foreground: Boolean) {
        this.settings = settings.sanitized(); this.phase = phase; this.foreground = foreground
        if (closed) return
        val mix = soundMix(this.settings, phase, foreground)
        effectsGain = mix.effects
        if (mix.music == 0f) stopMusic()
        else if (ambientPcm != null) {
            if (music == null) music = makeTrack(ambientPcm!!, loop = true, gain = 0f)
            rampMusic(mix.music)
        }
        if (mix.effects == 0f) stopEffects()
        else {
            effect?.setVolume(mix.effects)
            processing?.setVolume(mix.effects * 0.40f)
        }
        if (phase != CorePhase.PROCESSING || this.settings.mode != OperationMode.CINEMATIC || mix.effects == 0f) {
            processing = release(processing)
        }
        if (mix.effects > 0f && cues.isNotEmpty() && phase != previousPhase) {
            when (phase) {
                CorePhase.INITIALIZING -> if (this.settings.mode == OperationMode.CINEMATIC) cue(SoundCue.BOOT)
                CorePhase.PROCESSING -> if (this.settings.mode == OperationMode.CINEMATIC) {
                    processing = makeTrack(cues.getValue(SoundCue.PROCESS), loop = true, gain = mix.effects * 0.40f)
                } else cue(SoundCue.PROCESS)
                CorePhase.ERROR -> cue(SoundCue.ERROR)
                else -> Unit
            }
        }
        if (ambientPcm != null) previousPhase = if (foreground) phase else null
        releaseFocusWhenIdle()
    }

    fun cue(cue: SoundCue) {
        if (closed || !foreground || effectsGain <= 0f || settings.mode == OperationMode.SILENT) return
        val pcm = cues[cue] ?: return
        effectEnd?.let(main::removeCallbacks)
        effect = release(effect)
        effect = makeTrack(pcm, loop = false, gain = effectsGain)
        effectEnd = Runnable { effect = release(effect); releaseFocusWhenIdle() }.also {
            main.postDelayed(it, pcm.size * 1000L / OriginalScore.SAMPLE_RATE + 40L)
        }
    }

    private fun makeTrack(pcm: ShortArray, loop: Boolean, gain: Float): AudioTrack? {
        if (!focus.acquire(AssistantAudioFocus.Owner.SOUNDS)) return null
        var track: AudioTrack? = null
        try {
            track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(OriginalScore.SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(pcm.size * 2).build()
            // MODE_STATIC starts as NO_STATIC_DATA. The first write completes initialization.
            check(track.state != AudioTrack.STATE_UNINITIALIZED)
            check(track.write(pcm, 0, pcm.size, AudioTrack.WRITE_BLOCKING) == pcm.size)
            check(track.state == AudioTrack.STATE_INITIALIZED)
            if (loop) check(track.setLoopPoints(0, pcm.size, -1) == AudioTrack.SUCCESS)
            track.setVolume(gain.coerceIn(0f, 1f)); track.play()
            return track
        } catch (_: RuntimeException) {
            release(track)
            onError("O Android não iniciou o ambiente sonoro. Voz e comandos continuam disponíveis.")
            return null
        }
    }

    private fun rampMusic(target: Float) {
        ramp?.let(main::removeCallbacks)
        if (music == null) return
        val from = musicGain
        var step = 0
        ramp = object : Runnable {
            override fun run() {
                if (music == null || closed) return
                step++
                musicGain = from + (target - from) * (step / 6f).coerceAtMost(1f)
                music?.setVolume(musicGain)
                if (step < 6) main.postDelayed(this, 30L)
            }
        }.also(main::post)
    }
    private fun release(track: AudioTrack?): AudioTrack? {
        if (track != null) { runCatching { track.stop() }; track.release() }
        return null
    }
    private fun stopMusic() {
        ramp?.let(main::removeCallbacks); ramp = null
        music = release(music); musicGain = 0f
    }
    private fun stopEffects() {
        effectEnd?.let(main::removeCallbacks); effectEnd = null
        effect = release(effect); processing = release(processing)
    }
    private fun releaseFocusWhenIdle() {
        if (music == null && effect == null && processing == null) focus.release(AssistantAudioFocus.Owner.SOUNDS)
    }
    fun stop() {
        foreground = false; previousPhase = null; effectsGain = 0f
        stopMusic(); stopEffects(); focus.release(AssistantAudioFocus.Owner.SOUNDS)
    }
    fun snapshot() = SoundscapeSnapshot(ambientPcm != null, music != null, effect != null || processing != null,
        musicGain, effectsGain, music?.playbackHeadPosition ?: 0)
    fun close() { stop(); closed = true; ambientPcm = null; cues = emptyMap() }
}
