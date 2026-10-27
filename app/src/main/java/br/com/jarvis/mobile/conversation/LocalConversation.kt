package br.com.jarvis.mobile.conversation

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

internal object LlamaBridge {
    init { System.loadLibrary("jarvis_conversation") }
    val executor = Executors.newSingleThreadExecutor { task -> Thread(task, "jarvis-conversation") }
    val dispatcher = executor.asCoroutineDispatcher()
    val sequence = AtomicLong()
    external fun activate(id: Long)
    external fun generate(path: String, prompt: ByteArray, id: Long, maxTokens: Int): ByteArray
    external fun cancel()
    external fun release()
}

class LocalConversation(private val models: ModelStore) {
    private val history = ChatHistory()
    @Volatile private var initialized = false
    suspend fun answer(message: String): String {
        val file = models.readyFile() ?: throw IllegalStateException("MODEL_NOT_READY")
        val id = LlamaBridge.sequence.incrementAndGet()
        LlamaBridge.activate(id)
        initialized = true
        return withContext(LlamaBridge.dispatcher) {
            currentCoroutineContext().ensureActive()
            val bytes = try {
                LlamaBridge.generate(file.absolutePath, history.prompt(message).toByteArray(Charsets.UTF_8), id, 192)
            } catch (error: IllegalStateException) {
                if (error.message != "CONTEXT_TOO_LONG") throw error
                LlamaBridge.generate(file.absolutePath, history.prompt(message, false).toByteArray(Charsets.UTF_8), id, 192)
            }
            currentCoroutineContext().ensureActive()
            val reply = bytes.toString(Charsets.UTF_8).substringBefore("<|im_end|>")
                .substringBefore("<|im_start|>").replace('\uFFFD', ' ').trim().take(900)
            check(reply.isNotBlank()) { "EMPTY_REPLY" }
            history.remember(message, reply)
            reply
        }
    }
    fun cancel() { if (initialized) LlamaBridge.cancel() }
    fun clear() { history.clear() }
    fun release() { if (initialized) { LlamaBridge.cancel(); LlamaBridge.executor.execute { LlamaBridge.release() } } }
}
