package br.com.jarvis.mobile.hub

import br.com.jarvis.mobile.brain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class HubModelsTest {
    private val zone=ZoneId.of("America/Sao_Paulo")
    private val now=Instant.parse("2026-10-28T12:00:00Z")
    private fun event(title:String,start:String,end:String,allDay:Boolean=false)=AgendaEntry(1,2,title,Instant.parse(start).toEpochMilli(),Instant.parse(end).toEpochMilli(),allDay,"Pessoal",0)
    @Test fun allDayUsesUtcDateAndExclusiveEndInNegativeOffset() {
        val item=event("Feriado","2026-10-28T00:00:00Z","2026-10-29T00:00:00Z",true)
        assertTrue(todayEntries(listOf(item),LocalDate.of(2026,10,27),zone).isEmpty())
        assertEquals(listOf(item),todayEntries(listOf(item),LocalDate.of(2026,10,28),zone))
        assertTrue(todayEntries(listOf(item),LocalDate.of(2026,10,29),zone).isEmpty())
    }
    @Test fun timedEventsCrossMidnightAndNextExcludesAllDayOrPast() {
        val night=event("Turno","2026-10-28T02:00:00Z","2026-10-28T04:00:00Z")
        assertEquals(1,todayEntries(listOf(night),LocalDate.of(2026,10,27),zone).size)
        assertEquals(1,todayEntries(listOf(night),LocalDate.of(2026,10,28),zone).size)
        val upcoming=event("Reunião","2026-10-28T15:00:00Z","2026-10-28T16:00:00Z")
        assertEquals(upcoming,nextEntry(listOf(night,upcoming,upcoming.copy(allDay=true,start=now.toEpochMilli())),now.toEpochMilli()))
    }
    @Test fun countdownHasBoundariesWithoutNegativeTimes() {
        assertEquals("Agora",countdown(0,1))
        assertEquals("Em menos de um minuto",countdown(59999,0))
        assertEquals("Em 1 min",countdown(60000,0))
        assertEquals("Em 1 h 0 min",countdown(3600000,0))
        assertEquals("Em 1 dia(s)",countdown(86400000,0))
    }
    @Test fun localMailRulesPrioritizeActionAndRespectNoReplyWording() {
        assertEquals(EmailCategory.ACTION,classifyEmail("Aprovação pendente","Favor responder",setOf("CATEGORY_PROMOTIONS")))
        assertEquals(EmailCategory.INFO,classifyEmail("Aprovação","Apenas para conhecimento. Não precisa responder"))
        assertEquals(EmailCategory.NOISE,classifyEmail("Newsletter","Novidades"))
        assertEquals(EmailCategory.NOISE,classifyEmail("Oferta","",setOf("CATEGORY_SOCIAL")))
        assertEquals(EmailCategory.INFO,classifyEmail("Recibo","Pagamento recebido"))
    }
    private fun digest(notes:List<Note>?,medium:Boolean=false,agenda:AgendaSnapshot=AgendaSnapshot(),mail:MailSnapshot=MailSnapshot(),news:NewsSnapshot=NewsSnapshot())=
        DigestBuilder.build(now,zone,agenda,mail,news,WeatherSnapshot(),notes,medium)
    @Test fun unavailableSourcesAreNeverReportedAsEmptyOrInvented() {
        val text=digest(null).text
        assertTrue(text.contains("Agenda indisponível"))
        assertTrue(text.contains("E-mails indisponíveis"))
        assertTrue(text.contains("Clima não configurado"))
        assertTrue(text.contains("Notícias indisponíveis"))
        assertTrue(text.contains("Metas protegidas"))
        assertFalse(text.contains("Não há compromissos"))
    }
    @Test fun realEmptyAgendaDiffersFromPermissionDenied() {
        val ready=AgendaSnapshot(info=SourceInfo(SourceStatus.READY))
        assertTrue(digest(emptyList(),agenda=ready).text.contains("Não há compromissos"))
    }
    @Test fun onlyExplicitDigestNotesAreReadAndNoContentIsLeaked() {
        val secret=Note(title="SEGREDO NÃO AUTORIZADO",category=NoteCategory.HEALTH,content="Diagnóstico confidencial")
        val allowed=Note(title="Estudar inglês",category=NoteCategory.GOALS,content="Detalhe privado",includeInDigest=true)
        val text=digest(listOf(secret,allowed),medium=true).text
        assertTrue(text.contains("Estudar inglês"))
        assertFalse(text.contains(secret.title));assertFalse(text.contains(secret.content));assertFalse(text.contains(allowed.content))
    }
    @Test fun staleMailAndNewsAreLabeledAsCacheWithRealSource() {
        val mail=MailSnapshot(items=listOf(EmailItem("1","test@example.invalid","Teste","Assunto","Prévia",0,EmailCategory.ACTION)),info=SourceInfo(SourceStatus.STALE))
        val news=NewsSnapshot(listOf(NewsItem("1","Manchete de teste","https://example.invalid/1","Fonte de teste",0,setOf("Brasil"))),SourceInfo(SourceStatus.STALE))
        val text=digest(emptyList(),true,mail=mail,news=news).text
        assertTrue(text.contains("No último cache"));assertTrue(text.contains("Notícias do último cache"))
        assertTrue(text.contains("1 e-mail(s)"));assertTrue(text.contains("Fonte de teste"))
    }
    @Test fun reportsUseBoundedWholeSentencesAndCorrectLocalDate() {
        for(medium in listOf(false,true)) {
            val value=digest(emptyList(),medium)
            assertTrue(value.text.split(Regex("\\s+")).size<=if(medium)220 else 100)
            assertTrue(value.text.contains("quarta-feira, 28 de outubro"))
            assertTrue(value.privateData)
        }
    }
}
