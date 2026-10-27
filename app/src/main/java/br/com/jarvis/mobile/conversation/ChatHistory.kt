package br.com.jarvis.mobile.conversation

/** Memory-only dialogue. Model text is never routed to the device command dispatcher. */
class ChatHistory {
    private val turns = ArrayDeque<Pair<String, String>>()
    fun prompt(message: String, includeHistory: Boolean = true): String = buildString {
        append("<|im_start|>system\nVocê é JARVIS, um assistente pessoal. Converse somente em português brasileiro, de forma natural e clara. Responda em uma ou duas frases curtas. Use o contexto da conversa. Se não souber, diga que não sabe. Você não tem acesso à internet nem pode executar ações; nunca afirme que abriu apps, salvou compromissos ou fez tarefas.\n<|im_end|>\n")
        if (includeHistory) for ((question, answer) in turns) {
            append("<|im_start|>user\n").append(clean(question)).append("<|im_end|>\n")
            append("<|im_start|>assistant\n").append(clean(answer)).append("<|im_end|>\n")
        }
        append("<|im_start|>user\n").append(clean(message)).append("<|im_end|>\n<|im_start|>assistant\n")
    }
    fun remember(question: String, answer: String) {
        turns.addLast(clean(question) to clean(answer))
        while (turns.size > 3 || turns.sumOf { it.first.length + it.second.length } > 1600) turns.removeFirst()
    }
    fun clear() = turns.clear()
    private fun clean(text: String): String = text.replace(Regex("<\\|[^>]*\\|>"), "")
        .replace('\u0000', ' ').trim().take(480)
}
