package br.com.jarvis.mobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.jarvis.mobile.hub.RssParser
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class RssParserTest {
    @Test fun rssReadsRealFieldsRejectsUnsafeLinksAndDoesNotInventTime() {
        val xml="""<rss version="2.0"><channel>
          <item><title>Ciência &amp; tecnologia</title><link>https://example.invalid/noticia</link><pubDate>Wed, 28 Oct 2026 09:00:00 GMT</pubDate></item>
          <item><title>Sem data</title><link>https://example.invalid/sem-data</link></item>
          <item><title>Link inseguro</title><link>javascript:alert(1)</link></item>
        </channel></rss>"""
        val items=RssParser.parse(xml,"Fonte CI","Tecnologia",Instant.parse("2026-10-28T12:00:00Z").toEpochMilli())
        assertEquals(2,items.size);assertEquals("Ciência & tecnologia",items[0].title)
        assertEquals(Instant.parse("2026-10-28T09:00:00Z").toEpochMilli(),items[0].publishedAt)
        assertNull(items[1].publishedAt);assertEquals(setOf("Tecnologia"),items[0].topics)
    }
    @Test fun atomSupportsAlternateLinkAndDateWhileFutureTimeIsUnknown() {
        val xml="""<feed xmlns="http://www.w3.org/2005/Atom"><entry><id>CI</id><title>Notícia Atom</title><link rel="alternate" href="https://example.invalid/atom"/><updated>2099-01-01T00:00:00Z</updated></entry></feed>"""
        val item=RssParser.parse(xml,"Fonte CI","Brasil",0).single()
        assertEquals("https://example.invalid/atom",item.url);assertNull(item.publishedAt)
    }
    @Test fun rejectsHtmlAndExternalEntityDeclarations() {
        for(xml in listOf("<html><body>Indisponível</body></html>","""<!DOCTYPE rss [<!ENTITY xxe SYSTEM "file:///data/data/secret">]><rss><channel/></rss>""")) {
            try{RssParser.parse(xml,"CI","Brasil");fail("Invalid feed accepted")}catch(_:Exception){}
        }
    }
}
