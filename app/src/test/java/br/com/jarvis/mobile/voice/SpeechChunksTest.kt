package br.com.jarvis.mobile.voice

import org.junit.Assert.*
import org.junit.Test

class SpeechChunksTest {
    @Test fun longDigestIsSplitWithoutChangingWordsOrDroppingSentences() {
        val text=(1..100).joinToString(" "){"Compromisso $it confirmado às 09:30."}
        val chunks=speechChunks(text)
        assertTrue(chunks.size>1);assertTrue(chunks.all{it.length<=420})
        assertEquals(text,chunks.joinToString(" "))
    }
    @Test fun singleLongTokenStillHasBoundedUtterances() {
        val chunks=speechChunks("a".repeat(1000))
        assertTrue(chunks.all{it.length<=420});assertEquals(1000,chunks.sumOf{it.length})
    }
    @Test fun emptyMessageHasAudibleFallbackAndInvalidLimitFails() {
        assertEquals(listOf("Às suas ordens, senhor."),speechChunks("  "))
        assertThrows(IllegalArgumentException::class.java){speechChunks("texto",0)}
    }
}
