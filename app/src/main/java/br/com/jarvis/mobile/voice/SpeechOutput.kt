package br.com.jarvis.mobile.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class SpeechOutput(context: Context, private val onStatus: (String) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private var closed = false
    private var ready = false
    private var completion: (() -> Unit)? = null
    private var pending: Pair<String, () -> Unit>? = null
    private var generation = 0L
    private var currentId: String? = null
    private var initialized = false
    private var engine: TextToSpeech? = null
    private val deadline = Runnable { engine?.stop(); finish() }

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            main.post {
                if (!closed) {
                    initialized = true
                    val language = if (status == TextToSpeech.SUCCESS)
                        engine?.setLanguage(Locale.forLanguageTag("pt-BR")) else null
                    ready = language != null && language >= TextToSpeech.LANG_AVAILABLE
                    if (!ready) onStatus("Voz pt-BR indisponível. Instale uma voz nas configurações de texto para fala.")
                    pending?.let { (text, done) -> pending = null; speak(text, done) }
                }
            }
        }
        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) { main.post { if (utteranceId == currentId) finish() } }
            @Deprecated("Android callback")
            override fun onError(utteranceId: String?) {
                main.post {
                    if (utteranceId == currentId) {
                        onStatus("Não foi possível reproduzir a voz. A resposta aparece na tela.")
                        finish()
                    }
                }
            }
        })
    }

    fun speak(text: String, done: () -> Unit) {
        stop()
        if (closed) { done(); return }
        if (!initialized) {
            pending = text to done
            main.postDelayed(deadline, 6_000L)
            return
        }
        if (!ready) { done(); return }
        completion = done
        currentId = "jarvis-${++generation}"
        main.postDelayed(deadline, 15_000L)
        if (engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, currentId) == TextToSpeech.ERROR) finish()
    }
    private fun finish() {
        main.removeCallbacks(deadline)
        currentId = null
        val callback = completion ?: pending?.second
        completion = null
        pending = null
        callback?.invoke()
    }
    fun stop() {
        main.removeCallbacks(deadline)
        completion = null
        pending = null
        currentId = null
        engine?.stop()
    }
    fun close() { closed = true; stop(); engine?.shutdown() }
}
