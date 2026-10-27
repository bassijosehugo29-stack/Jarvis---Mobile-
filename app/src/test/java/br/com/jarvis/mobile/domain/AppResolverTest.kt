package br.com.jarvis.mobile.domain

import org.junit.Assert.*
import org.junit.Test

class AppResolverTest {
    private val resolver = AppResolver()
    private fun app(label: String, pkg: String = label) = LaunchableApp(label, pkg, "$pkg/.Main")

    @Test fun matchesInstalledLabelsIgnoringAccentsAndCase() {
        val camera = app("Câmera")
        assertEquals(listOf(camera), resolver.candidates("camera", listOf(camera)))
    }
    @Test fun explicitLabelWinsOverBroaderMatches() {
        val normal = app("WhatsApp", "com.whatsapp")
        val business = app("WhatsApp Business", "com.whatsapp.w4b")
        assertEquals(listOf(normal), resolver.candidates("WhatsApp", listOf(normal, business)))
        assertEquals(listOf(business), resolver.candidates("WhatsApp Business", listOf(normal, business)))
    }
    @Test fun calendarAliasCanReturnSeveralOptionsWithoutGuessing() {
        val samsung = app("Calendar", "com.samsung.android.calendar")
        val google = app("Google Calendar", "com.google.android.calendar")
        assertEquals(2, resolver.candidates("agenda", listOf(google, samsung)).size)
    }
    @Test fun unknownAppsAreNeverTurnedIntoWebSearchesOrPackages() {
        assertTrue(resolver.candidates("banco", listOf(app("Calculadora"))).isEmpty())
        assertTrue(resolver.candidates("", listOf(app("Calculadora"))).isEmpty())
    }
    @Test fun partialMatchesUseWholeWordsAndRemainAmbiguous() {
        assertEquals(2, resolver.candidates("Samsung", listOf(app("Samsung Notes"), app("Samsung Health"))).size)
        assertTrue(resolver.candidates("gram", listOf(app("Instagram"))).isEmpty())
    }
    @Test fun duplicateLabelsRequireANumber() {
        assertNull(choiceIndex("Agenda", listOf("Agenda", "Agenda")))
        assertEquals(1, choiceIndex("opção duas", listOf("Agenda", "Agenda")))
        assertNull(choiceIndex("3", listOf("Agenda", "Agenda")))
        assertEquals(0, choiceIndex("um", listOf("Pessoal", "Trabalho")))
        assertEquals(1, choiceIndex("trabalho", listOf("Pessoal", "Trabalho")))
    }
}
