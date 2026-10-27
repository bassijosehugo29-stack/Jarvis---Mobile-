package br.com.jarvis.mobile.domain

import java.text.Normalizer
import java.util.Locale

/** Pure Kotlin: no microphone, Android dependency, network or paid API. */
class CommandParser {
    fun parse(text: String): ParsedUtterance {
        val normalized = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9: ]"), " ")
            .replace(Regex("\\s+"), " ").trim()
        val wake = Regex("\\bjarvis\\b").containsMatchIn(normalized)
        val content = normalized.replace(Regex("\\bjarvis\\b"), "").trim()
        val command = when {
            content.isBlank() && wake -> Command.Wake
            Regex("\\b(pare|parar|desativar|silencio|cancelar)\\b").containsMatchIn(content) -> Command.Stop
            Regex("\\b(ajuda|comandos)\\b").containsMatchIn(content) -> Command.Help
            Regex("\\b(abrir|abra|iniciar)\\b").containsMatchIn(content) &&
                Regex("\\b(chatgpt|chat gpt|chat g p t)\\b").containsMatchIn(content) -> Command.OpenChatGpt
            Regex("\\b(alarme|despertador)\\b").containsMatchIn(content) -> parseAlarm(content)
            else -> Command.Unknown
        }
        return ParsedUtterance(wake, command)
    }

    private fun parseAlarm(text: String): Command {
        // Accept explicit numerical times; ambiguous/word-only times open the clock UI.
        if (Regex("\\b(daqui|amanha|meia|quarto)\\b").containsMatchIn(text)) return Command.AskAlarmTime
        val time = Regex("(?<![0-9])([0-9]{1,3})(?:\\s*:\\s*([0-9]{1,3})|\\s*(?:horas?|h)(?:\\s*(?:e\\s*)?([0-9]{1,3})(?:\\s*minutos?)?)?|\\s+e\\s+([0-9]{1,3}))?(?![0-9])")
            .find(text) ?: return Command.AskAlarmTime
        if (text.contains("minutos") && time.groupValues.drop(2).all { it.isEmpty() }) return Command.AskAlarmTime
        if (Regex("[0-9]").containsMatchIn(text.removeRange(time.range))) return Command.AskAlarmTime
        var hour = time.groupValues[1].toInt()
        val minute = time.groupValues.drop(2).firstOrNull { it.isNotEmpty() }?.toInt() ?: 0
        if (hour !in 0..23 || minute !in 0..59) return Command.InvalidAlarmTime
        if (text.contains("da tarde") || text.contains("da noite")) {
            if (hour in 1..11) hour += 12
        } else if (text.contains("da manha") && hour == 12) hour = 0
        return Command.SetAlarm(hour, minute)
    }
}
