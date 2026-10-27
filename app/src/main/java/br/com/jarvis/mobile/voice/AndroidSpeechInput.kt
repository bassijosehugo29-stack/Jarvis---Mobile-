package br.com.jarvis.mobile.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class AndroidSpeechInput(
    context: Context,
    private val onReady: () -> Unit,
    private val onResult: (String) -> Unit,
    private val onFailure: (Int) -> Unit,
    private val onLevel: (Float) -> Unit = {}
) : SpeechInput, RecognitionListener {
    override val available = SpeechRecognizer.isRecognitionAvailable(context)
    override val onDevice = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
    private var accepting = false
    private var closed = false
    private val recognizer: SpeechRecognizer? = when {
        !available -> null
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && onDevice ->
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        else -> SpeechRecognizer.createSpeechRecognizer(context)
    }?.also { it.setRecognitionListener(this) }

    override fun start() {
        if (closed || accepting) return
        if (recognizer == null) { onFailure(SpeechRecognizer.ERROR_CLIENT); return }
        accepting = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try { recognizer.startListening(intent) }
        catch (_: SecurityException) { accepting = false; onFailure(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) }
        catch (_: RuntimeException) { accepting = false; onFailure(SpeechRecognizer.ERROR_CLIENT) }
    }

    override fun stop() { accepting = false; recognizer?.cancel() }
    override fun close() {
        if (closed) return
        closed = true
        accepting = false
        recognizer?.destroy()
    }
    override fun onReadyForSpeech(params: Bundle?) { if (accepting) onReady() }
    override fun onResults(results: Bundle?) {
        if (!accepting) return
        accepting = false
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
        if (text.isNullOrBlank()) onFailure(SpeechRecognizer.ERROR_NO_MATCH) else onResult(text)
    }
    override fun onError(error: Int) {
        if (!accepting) return
        accepting = false
        onFailure(error)
    }
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) { if (accepting && !closed) onLevel(rmsdB) }
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit
    override fun onPartialResults(partialResults: Bundle?) = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}
