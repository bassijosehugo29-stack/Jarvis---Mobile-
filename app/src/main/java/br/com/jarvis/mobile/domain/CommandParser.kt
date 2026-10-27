package br.com.jarvis.mobile.domain

/** Pure Kotlin: no microphone, Android dependency, network or paid API. */
class CommandParser {
    fun parse(text: String): ParsedUtterance {
        val normalized = voiceText(text)
        val wake = Regex("\\bjarvis\\b").containsMatchIn(normalized)
        val content = normalized.replace(Regex("\\bjarvis\\b"), "").trim()
        val opening = Regex("^(?:(?:eu )?quero\\s+)?(?:abrir|abra|abre|iniciar|inicie|entrar)\\b\\s*(.*)$").find(content)
        val command = when {
            content.isBlank() && wake -> Command.Wake
            Regex("^(pare|parar|desativar|silencio|cancelar)\\b").containsMatchIn(content) -> Command.Stop
            Regex("^(ajuda|comandos)\\b").containsMatchIn(content) -> Command.Help
            opening != null -> {
                val name = opening.groupValues[1].replace(Regex("^(?:\\s*(?:no|na|o|a|aplicativo|app)\\b)+\\s*"), "").trim()
                    .replace(Regex("\\s+por favor$"), "")
                when (name) {
                    "" -> Command.Unknown
                    "chatgpt", "chat gpt", "chat g p t" -> Command.OpenChatGpt
                    else -> Command.OpenApp(name)
                }
            }
            Regex("\\b(agende|agendar|marque|marcar)\\b").containsMatchIn(content) ||
                (Regex("\\b(anote|anotar|criar|crie|coloque|colocar)\\b").containsMatchIn(content) &&
                    Regex("\\b(agenda|compromisso|evento)\\b").containsMatchIn(content)) -> Command.CreateEvent(text)
            Regex("\\b(alarme|despertador)\\b").containsMatchIn(content) -> parseAlarm(content)
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
