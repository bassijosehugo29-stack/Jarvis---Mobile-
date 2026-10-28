package br.com.jarvis.mobile.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

/** Optional foreground-only stop detector. Refuses speaker/unknown routes and any cloud recognizer. */
class VoiceInterruption(private val context: Context, private val onActive: (Boolean, String) -> Unit,
                        private val onInterrupt: () -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private var input: SpeechInput? = null
    private var active = false
    private var spoken = ""
    private val watchRoute = object : Runnable {
        override fun run() {
            if (!active) return
            if (!privateRoute()) { stop(); onActive(false, "Interrupção por toque · saída de áudio alterada"); return }
            handler.postDelayed(this, 300L)
        }
    }
    private val restart = Runnable { if (active && privateRoute()) input?.start() }
    fun privateRoute(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return false
        return try {
            val devices = context.getSystemService(AudioManager::class.java).getAudioDevicesForAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            devices.isNotEmpty() && devices.all { it.type in setOf(AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_USB_HEADSET) }
        } catch (_: RuntimeException) { false }
    }
    fun start(text: String) {
        stop()
        if (!privateRoute() || ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            onActive(false, "Para interromper por voz: Android 13+, fone com fio/USB e reconhecimento local. Toque no núcleo continua disponível."); return
        }
        spoken = text
        try {
            input = AndroidSpeechInput(context,
                onReady = { if (active) onActive(true, "Diga Jarvis, pare para interromper · microfone ativo") },
                onResult = { heard ->
                    if (active && privateRoute() && InterruptionPolicy.accepts(heard, spoken)) { stop(); onInterrupt() }
                    else if (active) handler.postDelayed(restart, 300L)
                },
                onFailure = { error ->
                    if (error in setOf(android.speech.SpeechRecognizer.ERROR_NO_MATCH, android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                        if (active) handler.postDelayed(restart, 400L)
                    } else { stop(); onActive(false, "O reconhecedor local não permitiu interrupção nesta saída. Use o núcleo.") }
                }, requireOnDevice = true)
            if (input?.available != true || input?.onDevice != true) { stop(); onActive(false, "Reconhecimento local pt-BR indisponível para interrupção. Use o núcleo."); return }
            active = true; input?.start(); handler.postDelayed(watchRoute, 300L)
        } catch (_: RuntimeException) { stop(); onActive(false, "Interrupção por toque disponível neste aparelho.") }
    }
    fun stop() {
        active = false; handler.removeCallbacks(restart); handler.removeCallbacks(watchRoute)
        input?.close(); input = null; spoken = ""; onActive(false, "")
    }
}
