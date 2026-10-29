package br.com.jarvis.mobile.voice

import java.text.Normalizer
import java.util.Locale

enum class ListeningPhase { IDLE, LISTENING, WAITING_FINAL, CONTINUATION, COMMITTED, UNCONFIRMED, CANCELLED }

data class ListeningDraft(val phase: ListeningPhase, val text: String, val revision: Long)

/** A monotonic-clock utterance boundary, independent of Android/provider endpoint decisions.
 * Partial hypotheses replace each other. Final native segments are joined only once. No device
 * command or assistant response may use a partial/unconfirmed snapshot.
 */
class SpeechTurn(
    private val now: () -> Long,
    val quietMillis: Long = 3_300L,
    private val finalGraceMillis: Long = 1_500L,
    private val maximumMillis: Long = 120_000L
) {
    init { require(quietMillis >= 3_000L); require(finalGraceMillis > 0); require(maximumMillis > quietMillis) }
    var phase = ListeningPhase.IDLE
        private set
    var revision = 0L
        private set
    private var started = 0L
    private var activity: Long? = null
    private var ended: Long? = null
    private var finishRequested: Long? = null
    private var confirmed = ""
    private var partial = ""
    private var speech = false
    private var awaitingFinal = false
    private var lastWindow = 0L
    private var window = 0L
    private var readyAt: Long? = null
    private val rms = RmsSpeechActivity()
    val active get() = phase == ListeningPhase.LISTENING || phase == ListeningPhase.WAITING_FINAL || phase == ListeningPhase.CONTINUATION
    val draft get() = ListeningDraft(phase, TranscriptJoiner.join(confirmed, partial), revision)
    val hasUnconfirmed get() = partial.isNotBlank() || awaitingFinal
    val needsFinal get() = finishRequested != null && hasUnconfirmed
    val textToCommit get() = confirmed.takeIf { phase == ListeningPhase.COMMITTED }

    fun start(): Long {
        revision++; confirmed = ""; partial = ""; speech = false; awaitingFinal = false
        started = now(); activity = null; ended = null; finishRequested = null; readyAt = null
        phase = ListeningPhase.LISTENING; rms.reset(); window = ++lastWindow
        return window
    }
    fun nextWindow(): Long {
        check(active && !hasUnconfirmed)
        window = ++lastWindow; readyAt = null; speech = false; ended = null; rms.reset()
        return window
    }
    fun accepts(id: Long) = active && id == window
    fun ready(id: Long) { if (accepts(id)) readyAt = now() }
    fun beginning(id: Long) {
        if (!accepts(id)) return
        speech = true; awaitingFinal = true; ended = null; markActivity(); phase = ListeningPhase.LISTENING
    }
    fun partial(id: Long, value: String) {
        if (!accepts(id)) return
        val next = value.trim().replace(Regex("\\s+"), " ").take(20_000)
        if (next.isEmpty() || next == partial) return
        partial = next; speech = true; awaitingFinal = true; markActivity(); phase = ListeningPhase.LISTENING
    }
    fun level(id: Long, db: Float) {
        if (accepts(id) && rms.voiced(db, now(), speech)) markActivity()
    }
    fun end(id: Long) {
        if (!accepts(id)) return
        speech = false; ended = now()
        if (activity == null && awaitingFinal) activity = ended
        phase = if (hasUnconfirmed) ListeningPhase.WAITING_FINAL else ListeningPhase.CONTINUATION
    }
    fun final(id: Long, value: String): Boolean {
        if (!accepts(id)) return false
        val clean = value.trim().replace(Regex("\\s+"), " ").take(20_000)
        if (clean.isBlank()) return false
        if (TranscriptJoiner.truncates(partial, clean)) { fail(); return false }
        // A delayed final/correction is not new acoustic activity and must not add another 3.3 s.
        if (activity == null) activity = ended ?: now()
        confirmed = TranscriptJoiner.join(confirmed, clean).take(20_000)
        partial = ""; speech = false; awaitingFinal = false; finishRequested = null
        phase = ListeningPhase.CONTINUATION; revision++
        return true
    }
    fun noMatch(id: Long): Boolean {
        if (!accepts(id)) return false
        speech = false
        if (hasUnconfirmed || confirmed.isBlank()) { fail(); return false }
        phase = ListeningPhase.CONTINUATION
        return true
    }
    /** The adapter schedules exactly this boundary, not a perpetual poll or timer per word. */
    fun deadline(): Long? {
        if (!active) return null
        val max = started + maximumMillis
        finishRequested?.let { return minOf(max, it + finalGraceMillis) }
        val last = activity ?: return minOf(max, started + 10_000L)
        return minOf(max, maxOf(last + quietMillis, (readyAt ?: started) + 120L))
    }
    fun advance(): ListeningPhase {
        if (!active) return phase
        if (now() >= started + maximumMillis) { fail(); return phase }
        if (now() < (deadline() ?: return phase)) return phase
        if (activity == null) { fail(); return phase }
        if (hasUnconfirmed || speech) {
            if (finishRequested == null) { finishRequested = now(); phase = ListeningPhase.WAITING_FINAL }
            else if (now() >= finishRequested!! + finalGraceMillis) fail()
        } else if (confirmed.isNotBlank()) { phase = ListeningPhase.COMMITTED; revision++ }
        else fail()
        return phase
    }
    fun fail() { if (active) { phase = ListeningPhase.UNCONFIRMED; revision++ } }
    fun cancel() {
        phase = ListeningPhase.CANCELLED; revision++; window = ++lastWindow; speech = false
        confirmed = ""; partial = ""; activity = null; finishRequested = null
    }
    private fun markActivity() { activity = now(); finishRequested = null; revision++ }
}

/** Merge only a segment boundary; revisions of the current hypothesis never append words. */
object TranscriptJoiner {
    private val spaces = Regex("\\s+")
    private val marks = Regex("\\p{M}")
    private val punctuation = Regex("[^\\p{L}\\p{N}]")
    private fun token(s: String) = Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(marks, "").replace(punctuation, "")
    fun truncates(partial: String, final: String): Boolean {
        if (partial.isBlank()) return false
        val a = partial.trim().split(spaces).map(::token); val b = final.trim().split(spaces).map(::token)
        return b.size < a.size && a.take(b.size) == b
    }
    fun join(left: String, right: String): String {
        if (left.isBlank()) return right.trim()
        if (right.isBlank()) return left.trim()
        val a = left.trim().split(spaces); val b = right.trim().split(spaces)
        val pattern = b.map(::token)
        // Linear suffix/prefix overlap, including long dictations and cumulative provider results.
        val prefix = IntArray(pattern.size)
        for (i in 1 until pattern.size) {
            var j = prefix[i - 1]
            while (j > 0 && pattern[i] != pattern[j]) j = prefix[j - 1]
            if (pattern[i] == pattern[j]) j++
            prefix[i] = j
        }
        var overlap = 0
        for (word in a) {
            val next = token(word)
            if (overlap == pattern.size) overlap = prefix[overlap - 1]
            while (overlap > 0 && next != pattern[overlap]) overlap = prefix[overlap - 1]
            if (next == pattern[overlap]) overlap++
        }
        return (a + b.drop(overlap)).joinToString(" ")
    }
}

/** Energy is a secondary hint, gated by the provider's speech event and debounced. A fan, isolated
 * click, duplicate partial or non-finite RMS must not keep extending the user's turn indefinitely.
 */
internal class RmsSpeechActivity {
    private var floor = -2f
    private var highSince: Long? = null
    fun reset() { floor = -2f; highSince = null }
    fun voiced(db: Float, now: Long, providerSpeech: Boolean): Boolean {
        if (!db.isFinite()) return false
        if (!providerSpeech) {
            floor += (db.coerceIn(-20f, 30f) - floor).coerceIn(-0.3f, 0.2f)
            highSince = null; return false
        }
        if (db < maxOf(1f, floor + 3.5f)) { highSince = null; return false }
        if (highSince == null) highSince = now
        return now - highSince!! >= 70L
    }
}
