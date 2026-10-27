package br.com.jarvis.mobile.voice

/** Replaceable input boundary; an eventual foreground service must own its own lifecycle. */
interface SpeechInput {
    val available: Boolean
    val onDevice: Boolean
    fun start()
    fun stop()
    fun close()
}
