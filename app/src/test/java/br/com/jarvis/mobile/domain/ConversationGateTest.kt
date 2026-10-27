package br.com.jarvis.mobile.domain

import org.junit.Assert.*
import org.junit.Test

class ConversationGateTest {
    private var time = 100L
    private val gate = ConversationGate { time }
    private val command = ParsedUtterance(false, Command.OpenChatGpt)
    @Test fun ignoresCommandsBeforeWakeWord() { assertFalse(gate.accept(command)) }
    @Test fun acceptsWakeAndFollowupWithinTwentySeconds() {
        assertTrue(gate.accept(ParsedUtterance(true, Command.Wake)))
        time += 19_999
        assertTrue(gate.accept(command))
    }
    @Test fun sessionExpiresAtDeadline() {
        gate.accept(ParsedUtterance(true, Command.Wake))
        time += 20_000
        assertFalse(gate.accept(command))
    }
    @Test fun resetRequiresWakeAgain() {
        gate.accept(ParsedUtterance(true, Command.Wake))
        gate.reset()
        assertFalse(gate.accept(command))
    }
    @Test fun speechCompletionCanExtendSession() {
        gate.accept(ParsedUtterance(true, Command.Wake))
        time += 18_000
        gate.extend()
        time += 10_000
        assertTrue(gate.accept(command))
    }
}
