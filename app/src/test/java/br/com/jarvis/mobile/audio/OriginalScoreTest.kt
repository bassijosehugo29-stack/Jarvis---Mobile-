package br.com.jarvis.mobile.audio

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class OriginalScoreTest {
    @Test fun originalLoopIsBoundedNonSilentAndHasZeroEdges() {
        val pcm = OriginalScore.ambient()
        assertEquals(OriginalScore.SAMPLE_RATE * OriginalScore.LOOP_SECONDS, pcm.size)
        assertEquals(0, pcm.first().toInt()); assertEquals(0, pcm.last().toInt())
        assertTrue(pcm.any { abs(it.toInt()) > 1000 })
        assertTrue(pcm.all { abs(it.toInt()) < 28000 })
    }
    @Test fun allCuesAreShortDistinctAndNonClipping() {
        val hashes = mutableSetOf<Int>()
        SoundCue.entries.forEach { cue ->
            val pcm = OriginalScore.cue(cue)
            assertTrue(pcm.size in 2000..40000)
            assertEquals(0, pcm.first().toInt()); assertEquals(0, pcm.last().toInt())
            assertTrue(pcm.any { abs(it.toInt()) > 1000 })
            assertTrue(pcm.all { abs(it.toInt()) < 28000 })
            assertTrue(hashes.add(pcm.contentHashCode()))
        }
    }
}
