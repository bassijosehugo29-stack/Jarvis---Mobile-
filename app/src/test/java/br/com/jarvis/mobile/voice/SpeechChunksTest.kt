package br.com.jarvis.mobile.voice

import org.junit.Assert.*
import org.junit.Test

class SpeechChunksTest {
    @Test fun aTitleStaysWithItsNameAtTheNativeTtsBoundary() {
        val text = "Informação para a reunião com o Sr. João amanhã pela manhã."
        val chunks = speechChunks(text, 40)
        assertEquals(text, chunks.joinToString(" "))
        assertTrue(chunks.all { it.length <= 40 })
        assertFalse(chunks.any { it.endsWith("Sr.") })
    }
    @Test fun decimalsTimesAndAccentsKeepTheirOriginalText() {
        val text = "Às 07:30, o Dr. José apresenta R$ 7,50 e a versão 2.0 do projeto. ".repeat(8).trim()
        val chunks = speechChunks(text, 80)
        assertEquals(text, chunks.joinToString(" "))
        assertFalse(chunks.any { it.endsWith("Dr.") })
    }
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
