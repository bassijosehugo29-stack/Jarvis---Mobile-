package br.com.jarvis.mobile.voice

import org.junit.Assert.*
import org.junit.Test

class SpeechTurnTest {
    private class Clock { var time = 0L; fun now() = time }
    @Test fun aNativeFinalIsNotPermissionToAnswerDuringANaturalPause() {
        val c = Clock(); val t = SpeechTurn(c::now); val first = t.start(); t.ready(first)
        t.beginning(first); t.partial(first, "Jarvis alarme para sete"); t.end(first)
        c.time = 700; assertTrue(t.final(first, "Jarvis alarme para sete"))
        val next = t.nextWindow(); c.time = 820; t.ready(next)
        c.time = 2_900; assertEquals(ListeningPhase.CONTINUATION, t.advance()); assertNull(t.textToCommit)
        t.beginning(next); t.partial(next, "e trinta"); t.end(next)
        c.time = 3_500; t.final(next, "e trinta")
        c.time = 5_900; assertNull(t.textToCommit); t.advance(); assertNull(t.textToCommit)
        c.time = 6_200; assertEquals(ListeningPhase.COMMITTED, t.advance())
        assertEquals("Jarvis alarme para sete e trinta", t.textToCommit)
    }
    @Test fun pauseOfThreeSecondsStillAllowsContinuationWithoutSplittingACommand() {
        val c = Clock(); val t = SpeechTurn(c::now); var id = t.start(); t.ready(id)
        t.partial(id, "Jarvis abre"); t.end(id); t.final(id, "Jarvis abre")
        id = t.nextWindow(); c.time = 100; t.ready(id)
        c.time = 3_000; t.beginning(id); t.partial(id, "o WhatsApp")
        c.time = 3_301; t.advance(); assertNull(t.textToCommit)
        t.end(id); t.final(id, "o WhatsApp"); c.time = 6_300; t.advance()
        assertEquals("Jarvis abre o WhatsApp", t.textToCommit)
    }
    @Test fun revisedPartialsReplaceRatherThanRepeatWords() {
        val c = Clock(); val t = SpeechTurn(c::now); val id = t.start(); t.ready(id)
        t.partial(id, "Jarvis abre o zap"); c.time = 150; t.partial(id, "Jarvis abre o WhatsApp")
        assertEquals("Jarvis abre o WhatsApp", t.draft.text)
        t.end(id); t.final(id, "Jarvis, abre o WhatsApp."); c.time = 3_450; t.advance()
        assertEquals("Jarvis, abre o WhatsApp.", t.textToCommit)
    }
    @Test fun repeatedPartialAndDelayedFinalDoNotAddAnotherSilenceWindow() {
        val c = Clock(); val t = SpeechTurn(c::now); val id = t.start(); t.ready(id)
        t.partial(id, "Jarvis"); val revision = t.revision
        c.time = 2_500; t.partial(id, "Jarvis"); assertEquals(revision, t.revision)
        t.end(id); c.time = 3_500; t.final(id, "Jarvis")
        assertEquals(ListeningPhase.COMMITTED, t.advance())
    }
    @Test fun endOfSpeechCannotCommitAnUnconfirmedPartial() {
        val c = Clock(); val t = SpeechTurn(c::now); val id = t.start(); t.ready(id)
        t.partial(id, "Jarvis alarme para"); t.end(id); c.time = 3_300
        assertEquals(ListeningPhase.WAITING_FINAL, t.advance()); assertTrue(t.needsFinal)
        c.time = 4_799; t.advance(); assertNull(t.textToCommit)
        c.time = 4_800; assertEquals(ListeningPhase.UNCONFIRMED, t.advance())
        assertEquals("Jarvis alarme para", t.draft.text); assertNull(t.textToCommit)
    }
    @Test fun aShorterFinalMustNotSilentlyDropTheTailOfThePartial() {
        val c = Clock(); val t = SpeechTurn(c::now); val id = t.start(); t.ready(id)
        t.partial(id, "Jarvis alarme para sete e trinta")
        assertFalse(t.final(id, "Jarvis alarme para sete"))
        assertEquals(ListeningPhase.UNCONFIRMED, t.phase)
        assertEquals("Jarvis alarme para sete e trinta", t.draft.text)
        assertNull(t.textToCommit)
    }
    @Test fun oldWindowCallbacksCannotReplaceANewerSegment() {
        val c = Clock(); val t = SpeechTurn(c::now); val old = t.start(); t.ready(old)
        t.partial(old, "Jarvis abre"); t.end(old); t.final(old, "Jarvis abre")
        val next = t.nextWindow(); t.ready(next); t.beginning(next); t.partial(next, "o WhatsApp")
        t.partial(old, "uma mensagem errada"); assertFalse(t.final(old, "outro aplicativo"))
        assertEquals("Jarvis abre o WhatsApp", t.draft.text)
    }
    @Test fun cancellationInvalidatesAQueuedConfirmationAndClearsEphemeralText() {
        val c = Clock(); val t = SpeechTurn(c::now); val id = t.start(); t.ready(id)
        t.partial(id, "Jarvis minha agenda"); t.final(id, "Jarvis minha agenda"); t.cancel()
        c.time = 9_000; t.advance(); assertNull(t.textToCommit); assertEquals("", t.draft.text)
        assertFalse(t.final(id, "Jarvis abre WhatsApp")); assertNull(t.deadline())
    }
    @Test fun aNewTurnCannotReceiveThePreviousTurnsFinal() {
        val c = Clock(); val t = SpeechTurn(c::now); val old = t.start(); t.cancel()
        val id = t.start(); t.ready(id); t.partial(id, "Jarvis notícias")
        assertFalse(t.final(old, "Jarvis alarme")); assertEquals("Jarvis notícias", t.draft.text)
    }
    @Test fun benignEmptyWindowsDoNotLoseAConfirmedPrefix() {
        val c = Clock(); val t = SpeechTurn(c::now); var id = t.start(); t.ready(id)
        t.partial(id, "Jarvis"); t.end(id); t.final(id, "Jarvis"); id = t.nextWindow(); t.ready(id)
        c.time = 1_000; assertTrue(t.noMatch(id)); id = t.nextWindow(); t.ready(id)
        c.time = 3_300; t.advance(); assertEquals("Jarvis", t.textToCommit)
    }
    @Test fun recognitionErrorPreservesAnUnconfirmedDraftButNeverDispatchesIt() {
        val c = Clock(); val t = SpeechTurn(c::now); val id = t.start(); t.ready(id)
        t.partial(id, "Jarvis, alarme para sete"); assertFalse(t.noMatch(id))
        assertEquals("Jarvis, alarme para sete", t.draft.text); assertNull(t.textToCommit)
    }
    @Test fun backgroundNoiseAndIsolatedClicksDoNotExtendTheTurn() {
        val c = Clock(); val t = SpeechTurn(c::now); var id = t.start(); t.ready(id)
        t.partial(id, "Jarvis"); t.final(id, "Jarvis"); id = t.nextWindow(); t.ready(id)
        for (i in 1..30) { c.time = i * 100L; t.level(id, 12f) }
        t.level(id, Float.NaN); t.level(id, Float.POSITIVE_INFINITY)
        c.time = 3_300; t.advance(); assertEquals("Jarvis", t.textToCommit)
    }
    @Test fun sustainedVoicedEnergyDuringProviderSpeechInvalidatesAnOldDeadline() {
        val c = Clock(); val t = SpeechTurn(c::now); val id = t.start(); t.ready(id)
        t.beginning(id); t.partial(id, "Jarvis quero")
        c.time = 2_700; t.level(id, 7f); c.time = 2_800; t.level(id, 8f)
        c.time = 3_300; t.advance(); assertEquals(ListeningPhase.LISTENING, t.phase)
        assertTrue(t.deadline()!! >= 6_100L)
    }
    @Test fun hardDeadlinePreventsPerpetualNoiseOrEndlessSpeech() {
        val c = Clock(); val t = SpeechTurn(c::now); val id = t.start(); t.ready(id)
        for (i in 0..120) { c.time = i * 1_000L; t.partial(id, "texto $i") }
        t.advance(); assertEquals(ListeningPhase.UNCONFIRMED, t.phase); assertNull(t.textToCommit)
    }
    @Test fun emptyInputHasABoundedInitialWait() {
        val c = Clock(); val t = SpeechTurn(c::now); val id = t.start(); t.ready(id)
        c.time = 10_000; assertEquals(ListeningPhase.UNCONFIRMED, t.advance()); assertNull(t.textToCommit)
    }
    @Test fun overlappingProviderWindowsDoNotDuplicateWakeWordsOrCommands() {
        assertEquals("Jarvis, abrir o WhatsApp", TranscriptJoiner.join("Jarvis, abrir", "abrir o WhatsApp"))
        assertEquals("notícias de tecnologia", TranscriptJoiner.join("notícias de", "de tecnologia"))
        assertEquals("Jarvis abrir WhatsApp", TranscriptJoiner.join("Jarvis abrir WhatsApp", "Jarvis abrir WhatsApp"))
    }
    @Test fun longCumulativeDictationsHaveABoundedLinearMerge() {
        val first = (1..2_000).joinToString(" ") { "palavra$it" }
        assertEquals("$first final", TranscriptJoiner.join(first, "$first final"))
    }
}
