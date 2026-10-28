package br.com.jarvis.mobile.domain

/** Pure Kotlin: no microphone, Android dependency, network or paid API. */
class CommandParser {
    fun parse(text: String): ParsedUtterance {
        val normalized = voiceText(text)
        val wakeWords = Regex("\\b(jarvis|jarves|javis|javes)\\b")
        val wake = wakeWords.containsMatchIn(normalized)
        val content = normalized.replace(wakeWords, "").trim()
        val opening = Regex("^(?:(?:eu )?quero\\s+)?(?:abrir|abra|abre|iniciar|inicie|entrar)\\b\\s*(.*)$").find(content)
        val command = when {
            content.isBlank() && wake -> Command.Wake
            Regex("^(pare|parar|desativar|silencio|cancelar)\\b").containsMatchIn(content) -> Command.Stop
            Regex("^(ajuda|comandos)$").matches(content) -> Command.Help
            Regex("^(preparar|prepare|baixar|baixe)\\s+(?:o\\s+|a\\s+)?(?:modelo|conversa)(?:\\s+offline)?$").matches(content) -> Command.PrepareConversation
            content in setOf("minha agenda", "o que tenho hoje", "agenda de hoje") -> Command.Agenda()
            content in setOf("proximo compromisso", "qual e o proximo compromisso") -> Command.Agenda("next")
            content in setOf("agenda da semana", "minha agenda da semana") -> Command.Agenda("week")
            content in setOf("meus e mails", "meus emails", "meus e-mails") -> Command.Mail()
            content in setOf("tem e mail importante", "tem email importante", "tem e-mail importante") -> Command.Mail(true)
            content == "noticias" -> Command.News()
            content.startsWith("noticias de ") -> Command.News(content.removePrefix("noticias de ").trim().take(80))
            content in setOf("bom dia", "digest", "resumo do dia") -> Command.Digest()
            content in setOf("abrir second brain", "abrir secondbrain", "abrir memoria", "abra second brain") -> Command.OpenBrain
            content in setOf("adicionar uma nota", "adicione uma nota", "nova nota", "adicionar nota") -> Command.AddNote
            content in setOf("quais sao minhas metas", "minhas metas", "quais minhas metas") -> Command.Goals
            opening != null -> {
                val name = opening.groupValues[1].replace(Regex("^(?:\\s*(?:no|na|o|a|aplicativo|app)\\b)+\\s*"), "").trim()
                    .replace(Regex("\\s+por favor$"), "")
                when (name) {
                    "" -> Command.Unknown
                    "chatgpt", "chat gpt", "chat g p t" -> Command.OpenChatGpt
                    else -> Command.OpenApp(name)
                }
            }
            Regex("^(?:(?:eu\\s+)?quero\\s+)?(agende|agendar|marque|marcar)\\b").containsMatchIn(content) ||
                (Regex("^(anote|anotar|criar|crie|coloque|colocar)\\b").containsMatchIn(content) &&
                    Regex("\\b(agenda|compromisso|evento)\\b").containsMatchIn(content)) -> Command.CreateEvent(text)
            Regex("^(?:(?:criar|crie|definir|defina|configurar|configure|colocar|coloque)\\s+(?:(?:um|o)\\s+)?)?(alarme|despertador)\\b").containsMatchIn(content) -> parseAlarm(content)
            else -> Command.Unknown
        }
        return ParsedUtterance(wake, command)
    }

    private fun parseAlarm(text: String): Command {
        if (Regex("\\b(daqui|amanha|hoje|dia)\\b").containsMatchIn(text)) return Command.AskAlarmTime
        val content = text.replace(Regex("^.*?\\b(?:alarme|despertador)\\b\\s*(?:para\\s+)?"), "").trim()
        if (content.isBlank()) return Command.AskAlarmTime
        val reading = readClock(content, standalone = true)
        if (reading.error) return Command.InvalidAlarmTime
        val time = reading.time ?: return Command.AskAlarmTime
        return Command.SetAlarm(time.hour, time.minute)
    }
}
