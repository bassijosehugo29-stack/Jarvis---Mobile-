package br.com.jarvis.mobile.domain

sealed interface Command {
    data object Wake : Command
    data object OpenChatGpt : Command
    data class SetAlarm(val hour: Int, val minute: Int) : Command
    data object AskAlarmTime : Command
    data object InvalidAlarmTime : Command
    data object Stop : Command
    data object Help : Command
    data object Unknown : Command
}

data class ParsedUtterance(val hasWakeWord: Boolean, val command: Command)
