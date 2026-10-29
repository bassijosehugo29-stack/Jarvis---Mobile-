package br.com.jarvis.mobile.conversation

/** Provider boundary: local generation today; a future authenticated Gemini adapter can be
 * supplied without embedding provider keys or granting generated text Android action authority.
 * Implementations must cooperate with cancellation; the ViewModel additionally fences late replies.
 */
interface ConversationClient {
    val ready: Boolean
    suspend fun answer(message: String): String
    fun cancel()
    fun clear()
    fun release()
}
fun interface ConversationFactory { fun create(models: ModelStore): ConversationClient }
object LocalConversationFactory : ConversationFactory {
    override fun create(models: ModelStore): ConversationClient = LocalConversation(models)
}
