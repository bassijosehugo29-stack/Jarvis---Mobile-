package br.com.jarvis.mobile.domain

import org.junit.Assert.*
import org.junit.Test

class PersonalCommandsTest {
    private val parser=CommandParser()
    @Test fun routesAllRequestedPersonalCommandsLocally() {
        val cases=mapOf("minha agenda" to Command.Agenda(),"o que tenho hoje" to Command.Agenda(),
            "próximo compromisso" to Command.Agenda("next"),"agenda da semana" to Command.Agenda("week"),
            "meus e-mails" to Command.Mail(),"tem e-mail importante" to Command.Mail(true),
            "notícias" to Command.News(),"notícias de tecnologia" to Command.News("tecnologia"),
            "bom dia" to Command.Digest(),"abrir Second Brain" to Command.OpenBrain,
            "adicionar uma nota" to Command.AddNote,"quais são minhas metas" to Command.Goals)
        cases.forEach{(text,expected)->val parsed=parser.parse("Jarvis, $text.");assertTrue(text,parsed.hasWakeWord);assertEquals(text,expected,parsed.command)}
    }
    @Test fun ordinaryConversationDoesNotCreateOrReadMemories() {
        for(text in listOf("minha saúde está melhor","quero discutir minhas metas","adicionar uma nota com minha senha 123","as notícias que vi ontem")) {
            assertEquals(text,Command.Unknown,parser.parse("Jarvis, $text").command)
        }
    }
    @Test fun preservesExistingAndroidActions() {
        assertEquals(Command.OpenChatGpt,parser.parse("Jarvis, abrir ChatGPT").command)
        assertEquals(Command.OpenApp("spotify"),parser.parse("Jarvis, abrir Spotify").command)
        assertEquals(Command.SetAlarm(7,30),parser.parse("Jarvis, alarme para 7:30").command)
        assertTrue(parser.parse("Jarvis, agende dentista amanhã às 9 horas").command is Command.CreateEvent)
    }
}
