package br.com.jarvis.mobile.voice

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

data class VoiceStatus(val ready: Boolean, val message: String, val engine: String = "", val locale: String = "", val failure: Boolean = false)
sealed interface SpeechResult {
    data class Completed(val audioBytes: Long) : SpeechResult
    data class Failed(val message: String) : SpeechResult
}

/** Explicit media routing, language/engine failover and visible failures, never silent success. */
class SpeechOutput(
    private val context: Context,
    private val onStatus: (VoiceStatus) -> Unit,
    private val enginePackages: List<String>? = null
) {
    private val main = Handler(Looper.getMainLooper())
    private val audio = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes).setOnAudioFocusChangeListener { change ->
            if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                main.post { if (currentId != null) fail("A fala foi interrompida por outro áudio. Tente novamente, senhor.") }
            }
        }.build()
    private var engine: TextToSpeech? = null
    private var selectedPackage = ""
    private var selectedLabel = ""
    private var selectedLocale = ""
    private var ready = false
    private var closed = false
    private var initializing = false
    private var generation = 0L
    private var currentId: String? = null
    private var audioBytes = 0L
    private var text = ""
    private var completion: ((SpeechResult) -> Unit)? = null
    private val tried = mutableSetOf<String>()
    private val initTimeout = Runnable { nextEngine("O mecanismo de voz demorou para iniciar.") }
    private val speechTimeout = Runnable { fail("O mecanismo de voz não confirmou a reprodução. Confira a saída de mídia e teste a voz nos ajustes.") }

    fun speak(message: String, done: (SpeechResult) -> Unit) {
        stop()
        if (closed) { done(SpeechResult.Failed("A saída de voz está fechada.")); return }
        text = message
        completion = done
        tried.clear()
        if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            fail("O volume de mídia está zerado. Aumente o volume com o JARVIS aberto e teste a voz."); return
        }
        if (ready) { tried += selectedPackage; play() }
        else if (!initializing) nextEngine()
    }

    @Suppress("DEPRECATION")
    private fun packages(): List<String> {
        enginePackages?.let { return it.distinct() }
        val manager = context.packageManager
        val intent = Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE)
        val found = if (Build.VERSION.SDK_INT >= 33) manager.queryIntentServices(intent, PackageManager.ResolveInfoFlags.of(0))
            else manager.queryIntentServices(intent, 0)
        val available = found.mapNotNull { it.serviceInfo?.packageName }.distinct()
        val preferred = Settings.Secure.getString(context.contentResolver, Settings.Secure.TTS_DEFAULT_SYNTH)
        return (listOfNotNull(preferred, "com.google.android.tts", "com.samsung.SMT") + available)
            .distinct().filter { it in available }
    }

    private fun nextEngine(reason: String = "Não há uma voz em português pronta para uso.") {
        main.removeCallbacks(initTimeout)
        main.removeCallbacks(speechTimeout)
        engine?.stop(); engine?.shutdown(); engine = null
        ready = false
        initializing = false
        currentId = null
        audio.abandonAudioFocusRequest(focus)
        if (closed || completion == null) return
        val candidate = try { packages().firstOrNull { it !in tried } } catch (_: RuntimeException) { null }
        if (candidate == null) {
            fail("$reason Abra os ajustes de voz, instale uma voz em português e use Testar voz."); return
        }
        tried += candidate
        selectedPackage = candidate
        selectedLabel = try { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(candidate, 0)).toString() }
            catch (_: PackageManager.NameNotFoundException) { candidate }
        initializing = true
        val token = ++generation
        onStatus(VoiceStatus(false, "Preparando a voz em português…"))
        main.postDelayed(initTimeout, 8_000L)
        try {
            engine = TextToSpeech(context.applicationContext, { status ->
                main.post {
                    if (closed || token != generation || !initializing) return@post
                    main.removeCallbacks(initTimeout)
                    initializing = false
                    if (status != TextToSpeech.SUCCESS || !selectPortuguese()) {
                        nextEngine("Não encontrei dados de voz em português nesse mecanismo."); return@post
                    }
                    val current = engine ?: return@post
                    current.setAudioAttributes(attributes)
                    current.setSpeechRate(0.96f)
                    current.setOnUtteranceProgressListener(listener)
                    ready = true
                    onStatus(VoiceStatus(true, "Voz pronta • $selectedLabel • $selectedLocale", selectedPackage, selectedLocale))
                    if (completion != null) play()
                }
            }, candidate)
        } catch (_: RuntimeException) { initializing = false; nextEngine("Não foi possível iniciar a voz.") }
    }

    private fun selectPortuguese(): Boolean {
        val current = engine ?: return false
        return try {
            val voices = current.voices.orEmpty().associateBy { it.name }
            val choices = portugueseVoices(voices.values.map { VoiceCandidate(it.name, it.locale, it.isNetworkConnectionRequired, it.quality) })
            for (voice in choices) {
                if (current.setVoice(voices.getValue(voice.name)) == TextToSpeech.SUCCESS) {
                    selectedLocale = voice.locale.toLanguageTag(); return true
                }
            }
            for (locale in listOf(Locale.forLanguageTag("pt-BR"), Locale.forLanguageTag("pt-PT"), Locale("pt"))) {
                if (current.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE && current.setLanguage(locale) >= TextToSpeech.LANG_AVAILABLE) {
                    selectedLocale = locale.toLanguageTag(); return true
                }
            }
            false
        } catch (_: RuntimeException) { false }
    }

    private fun play() {
        if (completion == null || closed || !ready) return
        if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            fail("O volume de mídia está zerado. Aumente o volume com o JARVIS aberto."); return
        }
        if (audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            fail("O Android não liberou o áudio agora. Termine chamadas ou outro áudio e teste novamente."); return
        }
        currentId = "jarvis-${++generation}"
        audioBytes = 0L
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f)
        }
        main.removeCallbacks(speechTimeout)
        main.postDelayed(speechTimeout, (text.length * 120L + 8_000L).coerceIn(20_000L, 90_000L))
        if (engine?.speak(text, TextToSpeech.QUEUE_FLUSH, params, currentId) != TextToSpeech.SUCCESS) {
            nextEngine("O mecanismo recusou a reprodução da fala.")
        }
    }

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit
        override fun onAudioAvailable(utteranceId: String?, audio: ByteArray?) {
            val count = audio?.size?.toLong() ?: 0L
            main.post { if (utteranceId != null && utteranceId == currentId) audioBytes += count }
        }
        override fun onDone(utteranceId: String?) {
            main.post { if (utteranceId != null && utteranceId == currentId) finish(SpeechResult.Completed(audioBytes)) }
        }
        @Deprecated("Android callback")
        override fun onError(utteranceId: String?) { onError(utteranceId, TextToSpeech.ERROR_SYNTHESIS) }
        override fun onError(utteranceId: String?, errorCode: Int) {
            main.post { if (utteranceId != null && utteranceId == currentId) nextEngine("A voz falhou ao sintetizar o áudio (código $errorCode).") }
        }
    }

    private fun fail(message: String) {
        onStatus(VoiceStatus(false, message, selectedPackage, selectedLocale, failure = true))
        finish(SpeechResult.Failed(message))
    }
    private fun finish(result: SpeechResult) {
        main.removeCallbacks(speechTimeout)
        main.removeCallbacks(initTimeout)
        engine?.stop()
        audio.abandonAudioFocusRequest(focus)
        currentId = null
        val callback = completion
        completion = null
        callback?.invoke(result)
    }
    fun stop() {
        main.removeCallbacks(speechTimeout)
        main.removeCallbacks(initTimeout)
        currentId = null
        completion = null
        generation++
        engine?.stop()
        audio.abandonAudioFocusRequest(focus)
        if (initializing) { engine?.shutdown(); engine = null; initializing = false; ready = false }
    }
    fun close() { closed = true; stop(); engine?.shutdown(); engine = null }
}
