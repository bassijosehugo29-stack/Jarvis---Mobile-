package br.com.jarvis.mobile.voice

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioFormat
import android.os.SystemClock
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import br.com.jarvis.mobile.audio.AssistantAudioFocus
import br.com.jarvis.mobile.experience.ExperienceSettings
import java.util.Locale

data class VoiceStatus(val ready: Boolean, val message: String, val engine: String = "", val locale: String = "", val failure: Boolean = false)
data class VoiceOption(val name: String, val label: String, val locale: String, val network: Boolean)
data class VoiceEngine(val packageName: String, val label: String)
sealed interface SpeechResult {
    data class Completed(val audioBytes: Long) : SpeechResult
    data class Failed(val message: String) : SpeechResult
    data object Suppressed : SpeechResult
    data class TextOnly(val reason: String) : SpeechResult
}

/** Explicit media routing, language/engine failover and visible failures, never silent success. */
class SpeechOutput(
    private val context: Context,
    private val onStatus: (VoiceStatus) -> Unit,
    private val enginePackages: List<String>? = null,
    audioSession: AssistantAudioFocus? = null,
    private val onVoices: (List<VoiceOption>) -> Unit = {},
    private val onPlayback: (Boolean) -> Unit = {},
    private val onRange: (Int, Int, Int) -> Unit = { _, _, _ -> },
    private val onLevel: (Float?) -> Unit = {}
) {
    private val main = Handler(Looper.getMainLooper())
    private val audio = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private val ownsFocus = audioSession == null
    private val focus = audioSession ?: AssistantAudioFocus(context) {
        if (completion != null) fail("A fala foi interrompida por outro áudio. Tente novamente, senhor.")
    }
    private var settings = ExperienceSettings()
    private val official get()=settings.voiceProfile==VoiceProfile.OFFICIAL_A && enginePackages==null
    private var inspecting = false
    private var localOnly = false
    private var engine: TextToSpeech? = null
    private var selectedPackage = ""
    private var selectedLabel = ""
    private var selectedLocale = ""
    private var ready = false
    private var closed = false
    private var initializing = false
    private var generation = 0L
    @Volatile private var currentId: String? = null
    private val envelope = PcmEnvelope()
    private var playbackStart = 0L
    private val levelTicker = object : Runnable {
        override fun run() {
            if (currentId == null || playbackStart == 0L) return
            onLevel(envelope.levelAt(SystemClock.uptimeMillis() - playbackStart))
            main.postDelayed(this, 50L)
        }
    }
    private var audioBytes = 0L
    private var text = ""
    private var completion: ((SpeechResult) -> Unit)? = null
    private val tried = mutableSetOf<String>()
    private val initTimeout = Runnable { nextEngine("O mecanismo de voz demorou para iniciar.") }
    private val speechTimeout = Runnable { fail("O mecanismo de voz não confirmou a reprodução. Confira a saída de mídia e teste a voz nos ajustes.") }

    fun configure(value: ExperienceSettings) {
        val next = value.sanitized()
        val engineChanged = next.enginePackage != settings.enginePackage || next.voiceProfile != settings.voiceProfile
        val changed = next.enginePackage != settings.enginePackage || next.voiceName != settings.voiceName || next.voiceProfile != settings.voiceProfile
        settings = next
        if (!settings.voiceAllowed) stop()
        if (changed) {
            stop(); engine?.shutdown(); engine = null; ready = false
            if (engineChanged) onVoices(emptyList())
        }
    }
    fun engines(): List<VoiceEngine> = packages().map { name ->
        val label = runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(name, 0)).toString() }.getOrDefault(name)
        VoiceEngine(name, label)
    }
    fun loadVoices() {
        if (closed || initializing || completion != null) return
        if (ready) { publishVoices(); return }
        inspecting = true; tried.clear(); nextEngine()
    }

    fun speak(message: String, done: (SpeechResult) -> Unit) = speak(message, false, done)
    fun speak(message: String, requireLocal: Boolean, done: (SpeechResult) -> Unit) {
        stop()
        localOnly = requireLocal
        if (closed) { done(SpeechResult.Failed("A saída de voz está fechada.")); return }
        if (!settings.voiceAllowed) { done(SpeechResult.Suppressed); return }
        if (ownsFocus) focus.allowAfterUserAction()
        text = message
        completion = done
        tried.clear()
        if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            fail("O volume de mídia está zerado. Aumente o volume com o JARVIS aberto e teste a voz."); return
        }
        if (ready) {
            tried += selectedPackage
            if (selectPortuguese()) play() else nextEngine("Não há uma voz local instalada nesse mecanismo.")
        }
        else if (!initializing) nextEngine()
    }

    @Suppress("DEPRECATION")
    private fun packages(): List<String> {
        enginePackages?.let { return it.distinct() }
        if(official)return if(OfficialVoiceA.installation(context)==VoiceAInstallation.READY)listOf(OfficialVoiceA.PACKAGE)else emptyList()
        val manager = context.packageManager
        val intent = Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE)
        val found = if (Build.VERSION.SDK_INT >= 33) manager.queryIntentServices(intent, PackageManager.ResolveInfoFlags.of(0))
            else manager.queryIntentServices(intent, 0)
        val available = found.mapNotNull { it.serviceInfo?.packageName }.distinct()
        val preferred = Settings.Secure.getString(context.contentResolver, Settings.Secure.TTS_DEFAULT_SYNTH)
        return (listOfNotNull(settings.enginePackage.takeIf { it.isNotBlank() }, preferred, "com.google.android.tts", "com.samsung.SMT") + available)
            .distinct().filter { it in available }
    }

    private fun nextEngine(reason: String = "Não há uma voz em português pronta para uso.") {
        main.removeCallbacks(initTimeout)
        main.removeCallbacks(speechTimeout)
        engine?.stop(); engine?.shutdown(); engine = null
        ready = false
        initializing = false
        currentId = null
        onPlayback(false)
        focus.release(AssistantAudioFocus.Owner.VOICE)
        if (closed || (completion == null && !inspecting)) return
        val candidate = try { packages().firstOrNull { it !in tried } } catch (_: RuntimeException) { null }
        if (candidate == null) {
            if(official){
                val message=OfficialVoiceA.unavailableMessage(context)
                onStatus(VoiceStatus(false,message,OfficialVoiceA.PACKAGE,"pt-BR",failure=true))
                finish(SpeechResult.TextOnly(message));return
            }
            if (localOnly) {
                val message = "Dados pessoais somente em texto: instale uma voz portuguesa local nos ajustes do Android."
                onStatus(VoiceStatus(false, message, selectedPackage, selectedLocale))
                finish(SpeechResult.TextOnly(message)); return
            }
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
                    current.setSpeechRate(settings.speechRate)
                    current.setPitch(if(official)1f else settings.pitch)
                    current.setOnUtteranceProgressListener(listener)
                    ready = true
                    inspecting = false
                    publishVoices()
                    onStatus(VoiceStatus(true, if(official)"Voz A pronta • Executivo clássico • pt-BR local" else "Voz pronta • $selectedLabel • $selectedLocale", selectedPackage, selectedLocale))
                    if (completion != null) play()
                }
            }, candidate)
        } catch (_: RuntimeException) { initializing = false; nextEngine("Não foi possível iniciar a voz.") }
    }

    private fun selectPortuguese(): Boolean {
        val current = engine ?: return false
        return try {
            val voices = current.voices.orEmpty().associateBy { it.name }
            val allowed = voices.values.filter { !localOnly || (!it.isNetworkConnectionRequired && TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features) }
            if(official){
                val approved=allowed.firstOrNull{it.name==OfficialVoiceA.VOICE&&it.locale.toLanguageTag()=="pt-BR"&&!it.isNetworkConnectionRequired&&TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features}?:return false
                if(current.setVoice(approved)!=TextToSpeech.SUCCESS)return false
                selectedLocale="pt-BR";return true
            }
            val choices = portugueseVoices(allowed.map { VoiceCandidate(it.name, it.locale, it.isNetworkConnectionRequired, it.quality) })
                .sortedBy { if (it.name == settings.voiceName) 0 else 1 }
            for (voice in choices) {
                if (current.setVoice(voices.getValue(voice.name)) == TextToSpeech.SUCCESS) {
                    selectedLocale = voice.locale.toLanguageTag(); return true
                }
            }
            if (localOnly) return false // Language fallback does not prove that the voice works offline.
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
        if (!focus.acquire(AssistantAudioFocus.Owner.VOICE)) {
            fail("O Android não liberou o áudio agora. Termine chamadas ou outro áudio e teste novamente."); return
        }
        onStatus(VoiceStatus(true, if(official)"Voz A pronta • Executivo clássico • pt-BR local" else "Voz pronta • $selectedLabel • $selectedLocale", selectedPackage, selectedLocale))
        currentId = "jarvis-${++generation}"
        envelope.reset(); playbackStart = 0L
        audioBytes = 0L
        engine?.setSpeechRate(settings.speechRate)
        engine?.setPitch(if(official)1f else settings.pitch)
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, settings.voiceVolume)
        }
        main.removeCallbacks(speechTimeout)
        main.postDelayed(speechTimeout, (text.length * 120L + 8_000L).coerceIn(20_000L, 90_000L))
        if (engine?.speak(text, TextToSpeech.QUEUE_FLUSH, params, currentId) != TextToSpeech.SUCCESS) {
            nextEngine("O mecanismo recusou a reprodução da fala.")
        }
    }

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            main.post { if (utteranceId != null && utteranceId == currentId) {
                playbackStart = SystemClock.uptimeMillis(); onPlayback(true)
                main.removeCallbacks(levelTicker); main.post(levelTicker)
            } }
        }
        override fun onBeginSynthesis(utteranceId: String?, sampleRateInHz: Int, audioFormat: Int, channelCount: Int) {
            if (utteranceId != null && utteranceId == currentId)
                envelope.reset(sampleRateInHz, channelCount, audioFormat == AudioFormat.ENCODING_PCM_16BIT)
        }
        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            main.post { if (utteranceId != null && utteranceId == currentId) onRange(start, end, text.length) }
        }
        override fun onAudioAvailable(utteranceId: String?, audio: ByteArray?) {
            if (utteranceId != null && utteranceId == currentId && audio != null) envelope.append(audio)
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
        inspecting = false
        onStatus(VoiceStatus(false, message, selectedPackage, selectedLocale, failure = true))
        finish(SpeechResult.Failed(message))
    }
    private fun finish(result: SpeechResult) {
        main.removeCallbacks(levelTicker); playbackStart = 0L; onLevel(null)
        main.removeCallbacks(speechTimeout)
        main.removeCallbacks(initTimeout)
        engine?.stop()
        onPlayback(false)
        focus.release(AssistantAudioFocus.Owner.VOICE)
        currentId = null
        val callback = completion
        completion = null
        callback?.invoke(result)
    }
    fun stop() {
        main.removeCallbacks(levelTicker); playbackStart = 0L; onLevel(null)
        main.removeCallbacks(speechTimeout)
        main.removeCallbacks(initTimeout)
        currentId = null
        completion = null
        generation++
        engine?.stop()
        inspecting = false
        onPlayback(false)
        focus.release(AssistantAudioFocus.Owner.VOICE)
        if (initializing) { engine?.shutdown(); engine = null; initializing = false; ready = false }
    }
    private fun publishVoices() {
        val options = runCatching { engine?.voices.orEmpty().filter { it.locale.language == "pt" }
            .sortedWith(compareBy({ if (it.locale.country == "BR") 0 else 1 }, { it.name }))
            .map { VoiceOption(it.name, "${it.name} · ${it.locale.toLanguageTag()}" + if (it.isNetworkConnectionRequired) " · rede" else " · local",
                it.locale.toLanguageTag(), it.isNetworkConnectionRequired) } }.getOrDefault(emptyList())
        onVoices(options)
    }
    fun close() { closed = true; stop(); engine?.shutdown(); engine = null; if (ownsFocus) focus.close() }
}
