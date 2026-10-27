package br.com.jarvis.mobile.voice

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class VoiceSelectionTest {
    private fun voice(name: String, locale: String, network: Boolean = false, quality: Int = 300) =
        VoiceCandidate(name, Locale.forLanguageTag(locale), network, quality)
    @Test fun prefersBrazilianPortugueseAndInstalledVoice() {
        val br = voice("local-br", "pt-BR")
        assertEquals(br, portugueseVoices(listOf(voice("english", "en-US"), voice("network-br", "pt-BR", true, 500),
            voice("local-pt", "pt-PT"), br)).first())
    }
    @Test fun europeanPortugueseWorksWhenBrazilianVoiceIsMissing() {
        val pt = voice("local-pt", "pt-PT")
        assertEquals(listOf(pt), portugueseVoices(listOf(voice("english", "en-US"), pt)))
    }
    @Test fun neverTreatsAnotherLanguageAsPortuguese() {
        assertTrue(portugueseVoices(listOf(voice("english", "en-US"), voice("espanol", "es-ES"))).isEmpty())
    }
}
