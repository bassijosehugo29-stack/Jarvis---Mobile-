package br.com.jarvis.mobile.voice

import java.util.Locale

data class VoiceCandidate(val name: String, val locale: Locale, val network: Boolean, val quality: Int)

/** Prefer installed Brazilian Portuguese; European Portuguese is a useful spoken fallback. */
internal fun portugueseVoices(voices: List<VoiceCandidate>): List<VoiceCandidate> = voices
    .filter { it.locale.language == "pt" }
    .sortedWith(compareBy<VoiceCandidate>({ if (it.locale.country == "BR") 0 else 1 },
        { if (it.network) 1 else 0 }, { -it.quality }, { it.name }))
