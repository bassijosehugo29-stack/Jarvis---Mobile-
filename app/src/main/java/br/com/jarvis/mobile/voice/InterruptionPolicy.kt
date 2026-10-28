package br.com.jarvis.mobile.voice

import br.com.jarvis.mobile.domain.voiceText

/** Only an explicit stop phrase is actionable during playback. Echo never becomes a command. */
object InterruptionPolicy {
    fun accepts(heard: String, spoken: String): Boolean {
        val h = voiceText(heard)
        return h in setOf("jarvis pare", "jarvis parar", "jarvis escute", "jarvis me escute") &&
            !voiceText(spoken).contains(h)
    }
}
