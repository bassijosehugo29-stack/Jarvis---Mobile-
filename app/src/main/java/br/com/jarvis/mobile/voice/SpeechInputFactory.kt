package br.com.jarvis.mobile.voice

import android.content.Context

data class SpeechCallbacks(val onReady: () -> Unit, val onResult: (String) -> Unit,
                           val onFailure: (Int) -> Unit, val onLevel: (Float) -> Unit = {})
fun interface SpeechInputFactory {
    fun create(context: Context, callbacks: SpeechCallbacks, requireLocal: Boolean): SpeechInput
}
object AndroidSpeechInputFactory : SpeechInputFactory {
    override fun create(context: Context, callbacks: SpeechCallbacks, requireLocal: Boolean): SpeechInput =
        AndroidSpeechInput(context, callbacks.onReady, callbacks.onResult, callbacks.onFailure, callbacks.onLevel, requireLocal)
}
