package br.com.jarvis.mobile.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

data class RecognitionEvents(
    val ready: () -> Unit, val beginning: () -> Unit, val end: () -> Unit,
    val partial: (String) -> Unit, val final: (String, Boolean) -> Unit,
    val error: (Int) -> Unit, val level: (Float) -> Unit
)
interface RecognitionWindow { fun start(); fun requestFinal(); fun close() }
interface RecognitionWindowFactory {
    val available: Boolean
    val onDevice: Boolean
    fun create(events: RecognitionEvents): RecognitionWindow
}

internal fun portugueseRecognitionIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    if (Build.VERSION.SDK_INT >= 33) putStringArrayListExtra(RecognizerIntent.EXTRA_BIASING_STRINGS,
        arrayListOf("Jarvis", "WhatsApp", "ChatGPT", "alarme", "agenda"))
}

/** A fresh recognizer/listener per native window isolates callbacks from cancelled services. */
internal class AndroidRecognitionWindows(context: Context, requireLocal: Boolean) : RecognitionWindowFactory {
    private val app = context.applicationContext
    override val onDevice = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(app)
    override val available = SpeechRecognizer.isRecognitionAvailable(app) && (!requireLocal || onDevice)
    override fun create(events: RecognitionEvents): RecognitionWindow {
        val recognizer = if (Build.VERSION.SDK_INT >= 31 && onDevice)
            SpeechRecognizer.createOnDeviceSpeechRecognizer(app) else SpeechRecognizer.createSpeechRecognizer(app)
        recognizer.setRecognitionListener(object : RecognitionListener {
            fun text(bundle: Bundle?) = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            override fun onReadyForSpeech(params: Bundle?) = events.ready()
            override fun onBeginningOfSpeech() = events.beginning()
            override fun onEndOfSpeech() = events.end()
            override fun onPartialResults(partialResults: Bundle?) = events.partial(text(partialResults))
            override fun onResults(results: Bundle?) = events.final(text(results), true)
            override fun onError(error: Int) = events.error(error)
            override fun onRmsChanged(rmsdB: Float) = events.level(rmsdB)
            override fun onSegmentResults(segmentResults: Bundle) = events.final(text(segmentResults), false)
            override fun onEndOfSegmentedSession() = events.final("", true)
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        return object : RecognitionWindow {
            private var closed = false
            override fun start() {
                check(!closed)
                // Silence extras can be ignored by providers; confirmation belongs to SpeechTurn.
                recognizer.startListening(portugueseRecognitionIntent())
            }
            override fun requestFinal() { if (!closed) recognizer.stopListening() }
            override fun close() {
                if (closed) return
                closed = true
                try { recognizer.cancel() } finally { recognizer.destroy() }
            }
        }
    }
}
