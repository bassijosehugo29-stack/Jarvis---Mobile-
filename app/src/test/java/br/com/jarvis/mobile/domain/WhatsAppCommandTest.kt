package br.com.jarvis.mobile.domain

import org.junit.Assert.*
import org.junit.Test

class WhatsAppCommandTest {
    @Test fun conversationalCommandOpensOnlyAnInstalledLauncherAndAbsenceIsEmpty() {
        val parsed = CommandParser().parse("Jarvis, abre o WhatsApp.")
        assertEquals(Command.OpenApp("whatsapp"), parsed.command)
        val resolver = AppResolver()
        val normal = LaunchableApp("WhatsApp", "com.whatsapp", "com.whatsapp/.Main")
        val business = LaunchableApp("WhatsApp Business", "com.whatsapp.w4b", "com.whatsapp.w4b/.Main")
        assertEquals(listOf(normal), resolver.candidates("whats app", listOf(business, normal)))
        assertTrue(resolver.candidates("whatsapp", emptyList()).isEmpty())
        assertEquals(listOf(business), resolver.candidates("whatsapp business", listOf(normal, business)))
    }
}
