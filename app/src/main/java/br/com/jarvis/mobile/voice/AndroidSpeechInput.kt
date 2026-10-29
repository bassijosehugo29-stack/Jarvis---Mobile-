package br.com.jarvis.mobile.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.SpeechRecognizer

/** Foreground-only turns spanning short native sessions. Partial text never dispatches a command.
 * Fast stop commands bypass conversational settling. No raw audio, hidden service or polling loop.
 */
class AndroidSpeechInput(
    context: Context,
    private val onReady: () -> Unit,
    private val onResult: (String) -> Unit,
    private val onFailure: (Int) -> Unit,
    private val onLevel: (Float) -> Unit = {},
    requireOnDevice: Boolean = false,
    private val naturalPauses: Boolean = true,
    private val onDraft: (ListeningDraft) -> Unit = {},
    factory: RecognitionWindowFactory? = null
) : SpeechInput {
    private val windows = factory ?: AndroidRecognitionWindows(context, requireOnDevice)
    private val localRequired = requireOnDevice
    override val onDevice get() = windows.onDevice
    override val available get() = windows.available && (!localRequired || windows.onDevice)
    private val handler = Handler(Looper.getMainLooper())
    private val turn = SpeechTurn(SystemClock::elapsedRealtime)
    private var native: RecognitionWindow? = null
    private var running = false
    private var closed = false
    private var liveWindow = false
    private var epoch = 0L
    private var windowId = 0L
    private var busyRetries = 0
    private var finalRequested = false
    private var scheduled: Runnable? = null
    private var reopening: Runnable? = null
    private var startup: Runnable? = null
    private var previousDraft: ListeningDraft? = null

    override fun start() {
        if (closed || running) return
        if (!available) { onFailure(SpeechRecognizer.ERROR_CLIENT); return }
        running = true; epoch++; busyRetries = 0; previousDraft = null
        windowId = turn.start()
        open(epoch, windowId)
    }
    private fun valid(generation: Long, id: Long) = running && !closed && generation == epoch && id == windowId
    private fun accepts(generation: Long, id: Long) = liveWindow && valid(generation, id)
    private fun open(generation: Long, id: Long) {
        if (!valid(generation, id)) return
        finalRequested = false; liveWindow = true
        try {
            native = windows.create(RecognitionEvents(
                ready = { if (accepts(generation, id)) {
                    startup?.let(handler::removeCallbacks); startup = null
                    turn.ready(id); onReady(); update(generation)
                } },
                beginning = { if (accepts(generation, id)) { turn.beginning(id); update(generation) } },
                end = { if (accepts(generation, id)) { turn.end(id); update(generation) } },
                partial = { text -> if (accepts(generation, id) && naturalPauses) { turn.partial(id, text); update(generation) } },
                final = { text, ended -> if (accepts(generation, id)) {
                    if (!naturalPauses) {
                        if (text.isNotBlank()) complete(text) else fail(SpeechRecognizer.ERROR_NO_MATCH)
                    } else {
                        if (text.isNotBlank() && !turn.final(id, text)) fail(SpeechRecognizer.ERROR_NO_MATCH)
                        else if (ended) {
                            closeNative(); turn.advance()
                            if (turn.phase == ListeningPhase.COMMITTED) complete(turn.textToCommit.orEmpty())
                            else if (turn.hasUnconfirmed) fail(SpeechRecognizer.ERROR_NO_MATCH)
                            else if (turn.active) reopen(generation, 100L)
                            else fail(SpeechRecognizer.ERROR_NO_MATCH)
                        } else update(generation)
                    }
                } },
                error = { code -> if (accepts(generation, id)) nativeError(generation, id, code) },
                level = { db -> if (accepts(generation, id)) {
                    if (naturalPauses) { turn.level(id, db); update(generation) }
                    onLevel(db)
                } }
            ))
            startup = Runnable { if (accepts(generation, id)) fail(SpeechRecognizer.ERROR_CLIENT) }
                .also { handler.postDelayed(it, 4_000L) }
            if (!naturalPauses) scheduled = Runnable {
                if (accepts(generation, id)) fail(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
            }.also { handler.postDelayed(it, 10_000L) }
            native?.start(); update(generation)
        } catch (_: SecurityException) { fail(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) }
        catch (_: RuntimeException) { fail(SpeechRecognizer.ERROR_CLIENT) }
    }
    private fun nativeError(generation: Long, id: Long, code: Int) {
        closeNative()
        if (naturalPauses && code == SpeechRecognizer.ERROR_RECOGNIZER_BUSY && busyRetries++ < 2 && !turn.hasUnconfirmed) {
            reopen(generation, 250L * busyRetries); return
        }
        if (naturalPauses && code in setOf(SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT) && turn.noMatch(id)) {
            turn.advance()
            if (turn.phase == ListeningPhase.COMMITTED) complete(turn.textToCommit.orEmpty())
            else reopen(generation, 120L)
        } else fail(code)
    }
    private fun reopen(generation: Long, delay: Long) {
        if (!running || !turn.active) return
        windowId = turn.nextWindow()
        val id = windowId
        reopening?.let(handler::removeCallbacks)
        reopening = Runnable { if (valid(generation, id)) open(generation, id) }.also { handler.postDelayed(it, delay) }
        update(generation)
    }
    private fun update(generation: Long) {
        if (!running || generation != epoch || !naturalPauses) return
        publish()
        scheduled?.let(handler::removeCallbacks); scheduled = null
        val deadline = turn.deadline() ?: return
        val revision = turn.revision
        scheduled = Runnable {
            if (!running || generation != epoch || turn.revision != revision) return@Runnable
            when (turn.advance()) {
                ListeningPhase.COMMITTED -> complete(turn.textToCommit.orEmpty())
                ListeningPhase.UNCONFIRMED -> fail(SpeechRecognizer.ERROR_NO_MATCH)
                else -> {
                    if (turn.needsFinal && !finalRequested) {
                        finalRequested = true
                        try { native?.requestFinal() } catch (_: RuntimeException) { fail(SpeechRecognizer.ERROR_CLIENT) }
                    }
                    update(generation)
                }
            }
        }.also { handler.postDelayed(it, (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(1L)) }
    }
    private fun publish() {
        val next = turn.draft
        if (next != previousDraft) { previousDraft = next; onDraft(next) }
    }
    private fun complete(text: String) {
        if (!running) return
        running = false; epoch++; clearScheduled(); closeNative(); publish(); onLevel(-2f)
        if (text.isBlank()) onFailure(SpeechRecognizer.ERROR_NO_MATCH) else onResult(text)
    }
    private fun fail(code: Int) {
        if (!running) return
        turn.fail(); publish()
        running = false; epoch++; clearScheduled(); closeNative(); onLevel(-2f); onFailure(code)
    }
    private fun closeNative() {
        liveWindow = false
        startup?.let(handler::removeCallbacks); startup = null
        val old = native; native = null
        try { old?.close() } catch (_: RuntimeException) { /* Cancelled service; never log speech. */ }
    }
    private fun clearScheduled() {
        scheduled?.let(handler::removeCallbacks); scheduled = null
        reopening?.let(handler::removeCallbacks); reopening = null
        startup?.let(handler::removeCallbacks); startup = null
    }
    override fun stop() { running = false; epoch++; turn.cancel(); clearScheduled(); closeNative(); onLevel(-2f) }
    override fun close() { if (!closed) { closed = true; stop() } }
}
