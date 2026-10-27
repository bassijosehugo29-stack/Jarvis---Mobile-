package br.com.jarvis.mobile;

import android.media.AudioFormat;
import android.speech.tts.SynthesisCallback;
import android.speech.tts.SynthesisRequest;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeechService;
import android.speech.tts.Voice;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Test-APK service runs in its own process: use only Java/Android classes, no target-APK runtime. */
public class FixtureTtsService extends TextToSpeechService {
    @Override public int onIsLanguageAvailable(String language, String country, String variant) {
        if (!language.equals("pt") && !language.equals("por")) return TextToSpeech.LANG_NOT_SUPPORTED;
        if (country.equals("BR") || country.equals("BRA")) return TextToSpeech.LANG_MISSING_DATA;
        return TextToSpeech.LANG_COUNTRY_AVAILABLE;
    }
    @Override public String[] onGetLanguage() { return new String[]{"por", "PRT", ""}; }
    @Override public int onLoadLanguage(String language, String country, String variant) { return onIsLanguageAvailable(language, country, variant); }
    @Override public List<Voice> onGetVoices() {
        return Collections.singletonList(new Voice("jarvis-fixture-pt-PT", Locale.forLanguageTag("pt-PT"),
            Voice.QUALITY_NORMAL, Voice.LATENCY_NORMAL, false, Collections.<String>emptySet()));
    }
    @Override public int onIsValidVoiceName(String voiceName) { return voiceName.equals("jarvis-fixture-pt-PT") ? TextToSpeech.SUCCESS : TextToSpeech.ERROR; }
    @Override public int onLoadVoice(String voiceName) { return onIsValidVoiceName(voiceName); }
    @Override public String onGetDefaultVoiceNameFor(String language, String country, String variant) { return "jarvis-fixture-pt-PT"; }
    @Override public void onStop() { }
    @Override public void onSynthesizeText(SynthesisRequest request, SynthesisCallback callback) {
        if (request.getCharSequenceText().toString().contains("falha simulada")) {
            callback.error(TextToSpeech.ERROR_SYNTHESIS); return;
        }
        if (callback.start(16000, AudioFormat.ENCODING_PCM_16BIT, 1) != TextToSpeech.SUCCESS) return;
        byte[] pcm = new byte[9600];
        for (int i = 0; i < pcm.length / 2; i++) {
            int sample = (int) (Math.sin(2 * Math.PI * 660 * i / 16000) * 6000);
            pcm[i * 2] = (byte) (sample & 255); pcm[i * 2 + 1] = (byte) ((sample >> 8) & 255);
        }
        for (int offset = 0; offset < pcm.length; ) {
            int count = Math.min(callback.getMaxBufferSize(), pcm.length - offset);
            if (callback.audioAvailable(pcm, offset, count) != TextToSpeech.SUCCESS) return;
            offset += count;
        }
        callback.done();
    }
}
