package br.com.jarvis.mobile.domain

import org.junit.Assert.*
import org.junit.Test

class CommandParserTest {
    private val parser = CommandParser()
    @Test fun wakeWordIsCaseInsensitive() {
        assertEquals(ParsedUtterance(true, Command.Wake), parser.parse("JÁRVIS!"))
    }
    @Test fun wakeWordMustBeAWholeWord() {
        assertFalse(parser.parse("superjarvis abrir ChatGPT").hasWakeWord)
    }
    @Test fun opensOnlyExplicitChatGptCommands() {
        for (text in listOf("Jarvis, abrir ChatGPT", "Jarvis abra o chat gpt", "iniciar chat g p t")) {
            assertEquals(Command.OpenChatGpt, parser.parse(text).command)
        }
        assertEquals(Command.Unknown, parser.parse("Jarvis, fechar ChatGPT").command)
    }
    @Test fun acceptsSupportedAlarmFormats() {
        for (time in listOf("7:30", "7h30", "7 horas e 30 minutos", "7 e 30")) {
            assertEquals(time, Command.SetAlarm(7, 30), parser.parse("Jarvis, alarme para $time").command)
        }
        assertEquals(Command.SetAlarm(0, 0), parser.parse("alarme para 0:00").command)
        assertEquals(Command.SetAlarm(23, 59), parser.parse("despertador para 23:59").command)
        assertEquals(Command.SetAlarm(7, 0), parser.parse("alarme para 7").command)
    }
    @Test fun handlesDayPeriods() {
        assertEquals(Command.SetAlarm(19, 0), parser.parse("alarme para 7 da noite").command)
        assertEquals(Command.SetAlarm(15, 0), parser.parse("alarme para 3 da tarde").command)
        assertEquals(Command.SetAlarm(0, 0), parser.parse("alarme para 12 da manhã").command)
    }
    @Test fun refusesInvalidTimes() {
        for (time in listOf("25:00", "7:60", "99:99", "123:30", "7:300")) {
            assertEquals(time, Command.InvalidAlarmTime, parser.parse("alarme para $time").command)
        }
    }
    @Test fun ambiguousTimesRequireClockUi() {
        for (text in listOf("alarme", "alarme para sete e trinta", "alarme para 7 e meia",
            "alarme para amanhã às 7", "alarme daqui a 10 minutos", "alarme para 7 30")) {
            assertEquals(text, Command.AskAlarmTime, parser.parse(text).command)
        }
    }
    @Test fun stopTakesPriority() {
        assertEquals(Command.Stop, parser.parse("Jarvis cancelar alarme").command)
    }
    @Test fun helpAndUnknownAreExplicit() {
        assertEquals(Command.Help, parser.parse("Jarvis ajuda").command)
        assertEquals(Command.Unknown, parser.parse("").command)
    }
}
