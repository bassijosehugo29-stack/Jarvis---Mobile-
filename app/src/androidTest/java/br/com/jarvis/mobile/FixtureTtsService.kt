package br.com.jarvis.mobile

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

/** Android-test APK only: emits real nonzero PCM and intentionally has no pt-BR voice. */
class FixtureTtsService : TextToSpeechService() {
    override fun onIsLanguageAvailable(lang: String, country: String, variant: String): Int = when {
        lang !in setOf("pt", "por") -> TextToSpeech.LANG_NOT_SUPPORTED
        country in setOf("BR", "BRA") -> TextToSpeech.LANG_MISSING_DATA
        else -> TextToSpeech.LANG_COUNTRY_AVAILABLE
    }
    override fun onGetLanguage(): Array<String> = arrayOf("por", "PRT", "")
    override fun onLoadLanguage(lang: String, country: String, variant: String): Int = onIsLanguageAvailable(lang, country, variant)
    override fun onGetVoices(): MutableList<Voice> = mutableListOf(
        Voice("jarvis-fixture-pt-PT", Locale.forLanguageTag("pt-PT"), Voice.QUALITY_NORMAL, Voice.LATENCY_NORMAL, false, emptySet()))
    override fun onIsValidVoiceName(voiceName: String): Int = if (voiceName == "jarvis-fixture-pt-PT") TextToSpeech.SUCCESS else TextToSpeech.ERROR
    override fun onLoadVoice(voiceName: String): Int = onIsValidVoiceName(voiceName)
    override fun onGetDefaultVoiceNameFor(lang: String, country: String, variant: String): String = "jarvis-fixture-pt-PT"
    override fun onStop() = Unit
    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        if (request.charSequenceText.toString().contains("falha simulada")) {
            callback.error(TextToSpeech.ERROR_SYNTHESIS); return
        }
        callback.start(16_000, AudioFormat.ENCODING_PCM_16BIT, 1)
        val pcm = ByteArray(9_600)
        for (i in 0 until pcm.size / 2) {
            val sample = (sin(2 * PI * 660 * i / 16_000) * 6_000).toInt()
            pcm[i * 2] = (sample and 255).toByte(); pcm[i * 2 + 1] = ((sample shr 8) and 255).toByte()
        }
        var offset = 0
        while (offset < pcm.size) {
            val count = minOf(callback.maxBufferSize, pcm.size - offset)
            if (callback.audioAvailable(pcm, offset, count) != TextToSpeech.SUCCESS) return
            offset += count
        }
        callback.done()
    }
}
