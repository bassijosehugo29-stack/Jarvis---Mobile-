package br.com.jarvis.mobile.domain

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class EventParserTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-27T12:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val parser = EventParser(clock)

    @Test fun fullRequestPreservesTitleAndUsesPhoneTimeZone() {
        val result = parser.parse("Jarvis, agende reunião com João amanhã às 15:30")
        assertNull(result.error)
        assertEquals("reunião com João", result.draft.title)
        assertEquals(LocalDate.of(2026, 10, 28), result.draft.date)
        assertEquals(LocalTime.of(15, 30), result.draft.time)
        val event = requireNotNull(parser.toEvent(result.draft))
        assertEquals("America/Sao_Paulo", event.timeZone)
        assertEquals(Instant.parse("2026-10-28T18:30:00Z").toEpochMilli(), event.startMillis)
        assertEquals(60 * 60_000L, event.endMillis - event.startMillis)
    }
    @Test fun conversationCollectsMissingDateAndTime() {
        var result = parser.parse("Agende dentista")
        assertEquals("dentista", result.draft.title)
        assertNull(parser.toEvent(result.draft))
        result = parser.parse("amanhã", result.draft)
        assertEquals("dentista", result.draft.title)
        assertNull(result.draft.time)
        result = parser.parse("quinze e trinta", result.draft)
        assertNull(result.error)
        assertEquals(LocalTime.of(15, 30), result.draft.time)
        assertNotNull(parser.toEvent(result.draft))
    }
    @Test fun conversationCanAskForTitleLast() {
        val emptyTitle = parser.parse("Agende amanhã às três da tarde").draft
        assertTrue(emptyTitle.title.isBlank())
        assertNull(parser.toEvent(emptyTitle))
        val complete = parser.parse("Consulta com a médica", emptyTitle).draft
        assertEquals("Consulta com a médica", complete.title)
        assertNotNull(parser.toEvent(complete))
    }
    @Test fun numericAndNamedDatesAreEquivalent() {
        for (date in listOf("dia 30/10", "30/10/2026", "dia 30 de outubro", "30 de outubro de 2026")) {
            val result = parser.parse("Agende dentista $date às 14 horas")
            assertNull(date, result.error)
            assertEquals(date, LocalDate.of(2026, 10, 30), result.draft.date)
            assertEquals(date, LocalTime.of(14, 0), result.draft.time)
        }
    }
    @Test fun missingYearUsesNextOccurrence() {
        val draft = parser.parse("Agende consulta dia 20 de outubro às 14 horas").draft
        assertEquals(LocalDate.of(2027, 10, 20), draft.date)
    }
    @Test fun invalidDateOrTimeIsNeverSaved() {
        for (text in listOf("Agende consulta 31/02 às 14 horas", "Agende consulta amanhã às 25:00",
            "Agende consulta amanhã às 7:90", "Agende consulta amanhã às 7 30")) {
            val result = parser.parse(text)
            assertNotNull(text, result.error)
            assertNull(text, parser.toEvent(result.draft))
        }
    }
    @Test fun unsupportedRecurrenceAndRelativeOffsetsAreNotGuessed() {
        for (text in listOf("Agende treino todo dia às 7", "Agende consulta daqui a duas horas")) {
            assertNotNull(parser.parse(text).error)
        }
    }
    @Test fun pastTimeDoesNotRollOverSilently() {
        val draft = parser.parse("Agende consulta hoje às 8 horas").draft
        assertEquals(LocalDate.of(2026, 10, 27), draft.date)
        assertNull(parser.toEvent(draft))
    }
    @Test fun weekdayMeansNextOccurrenceAndExplicitDurationIsRespected() {
        val result = parser.parse("Agende treino sexta-feira às 18h30 por 30 minutos")
        assertNull(result.error)
        assertEquals("treino", result.draft.title)
        assertEquals(LocalDate.of(2026, 10, 30), result.draft.date)
        assertEquals(LocalTime.of(18, 30), result.draft.time)
        assertEquals(30, result.draft.durationMinutes)
    }
    @Test fun durationIsNotMistakenForStartTime() {
        val result = parser.parse("Agende treino amanhã por duas horas")
        assertNull(result.error)
        assertNull(result.draft.time)
        assertEquals(120, result.draft.durationMinutes)
        assertEquals("treino", result.draft.title)
    }
    @Test fun invalidDurationsAreRejected() {
        for (duration in listOf("por 0 minutos", "por 999 minutos", "por duas horas e meia")) {
            assertNotNull(parser.parse("Agende treino amanhã às 15 horas $duration").error)
        }
    }
    @Test fun nonexistentOrAmbiguousDstTimeIsRejected() {
        val dstParser = EventParser(Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("Europe/Paris")))
        for (draft in listOf(EventDraft("Teste", LocalDate.of(2026, 3, 29), LocalTime.of(2, 30)),
            EventDraft("Teste", LocalDate.of(2026, 10, 25), LocalTime.of(2, 30)))) {
            assertNull(dstParser.toEvent(draft))
        }
    }
}
