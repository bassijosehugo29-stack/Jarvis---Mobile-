package br.com.jarvis.mobile.domain

sealed interface Command {
    data object Wake : Command
    data object OpenChatGpt : Command
    data class OpenApp(val name: String) : Command
    data class CreateEvent(val request: String) : Command
    data class SetAlarm(val hour: Int, val minute: Int) : Command
    data object AskAlarmTime : Command
    data object InvalidAlarmTime : Command
    data object Stop : Command
    data object Help : Command
    data object PrepareConversation : Command
    data class Agenda(val range: String = "today") : Command
    data class Mail(val actionOnly: Boolean = false) : Command
    data class News(val topic: String? = null) : Command
    data class Digest(val automatic: Boolean = false) : Command
    data object OpenBrain : Command
    data object AddNote : Command
    data object Goals : Command
    data object Unknown : Command
}

data class ParsedUtterance(val hasWakeWord: Boolean, val command: Command)
