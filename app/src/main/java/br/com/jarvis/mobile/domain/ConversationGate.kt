package br.com.jarvis.mobile.domain

/** Wake gate with monotonic time, injectable for deterministic unit tests. */
class ConversationGate(private val nowMillis: () -> Long) {
    private var activeUntil = 0L
    fun isOpen(): Boolean = nowMillis() < activeUntil
    fun accept(parsed: ParsedUtterance): Boolean {
        if (!parsed.hasWakeWord && nowMillis() >= activeUntil) return false
        activeUntil = nowMillis() + 20_000L
        return true
    }
    fun extend() { activeUntil = nowMillis() + 20_000L }
    fun reset() { activeUntil = 0L }
}
