package br.com.jarvis.mobile.voice

import org.junit.Assert.*
import org.junit.Test

class InterruptionPolicyTest {
    @Test fun onlyExplicitInterruptionIsAcceptedAndAssistantEchoIsRejected() {
        assertTrue(InterruptionPolicy.accepts("Jarvis, pare!", "Às suas ordens, senhor."))
        assertTrue(InterruptionPolicy.accepts("Jarvis, me escute", "Resposta de exemplo."))
        for (text in listOf("parar", "Jarvis", "Jarvis abrir WhatsApp", "eu disse Jarvis pare depois"))
            assertFalse(InterruptionPolicy.accepts(text, "Resposta normal"))
        assertFalse(InterruptionPolicy.accepts("Jarvis, pare", "Para interromper, diga Jarvis, pare."))
    }
}
