package br.com.jarvis.mobile.voice

import org.junit.Assert.*
import org.junit.Test

class PcmEnvelopeTest {
    @Test fun envelopeTracksPlaybackTimeAndSurvivesOddBufferBoundaries() {
        val e = PcmEnvelope(); e.reset(22050)
        val data = ByteArray(882 * 3)
        for (i in 882 until 1764 step 2) { data[i] = 0; data[i+1] = 32 }
        data.toList().chunked(113).forEach { e.append(it.toByteArray()) }
        assertEquals(0f, e.levelAt(0)!!, 0f); assertTrue(e.levelAt(20)!! > 0.9f)
        assertEquals(0f, e.levelAt(40)!!, 0f); assertNull(e.levelAt(60)); assertNull(e.levelAt(-1))
        e.reset(22050, pcm16 = false); e.append(data); assertNull(e.levelAt(0))
    }
    @Test fun stereoSilenceAndResetDoNotRetainPreviousUtterances() {
        val e = PcmEnvelope(); e.reset(48000, 2); e.append(ByteArray(3840))
        assertEquals(0f, e.levelAt(0)!!, 0f); e.reset(); assertNull(e.levelAt(0))
    }
}
