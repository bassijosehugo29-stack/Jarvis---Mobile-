package br.com.jarvis.mobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.jarvis.mobile.hub.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.*
import java.util.concurrent.atomic.AtomicInteger

/** Deterministic transport fixtures exercise real Room cache/parser; live availability has a separate CI report. */
@RunWith(AndroidJUnit4::class)
class PublicRepositoriesTest {
    private val storage=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as JarvisApplication).data.storage
    @Before fun reset()=runBlocking{storage.eraseAll()}
    @After fun clean()=runBlocking{storage.eraseAll()}
    @Test fun rssCacheHonorsTtlConditionalRefreshDeduplicationAndOfflineStatus()=runBlocking {
        val requests=AtomicInteger();val conditional=AtomicInteger();var offline=false
        val xml="""<rss><channel><item><title>Pesquisa tecnológica CI</title><link>https://example.invalid/mesma-noticia</link><pubDate>${Instant.now().minusSeconds(60)}</pubDate></item></channel></rss>"""
        val transport=object:HttpTransport{override suspend fun get(url:String,bearer:String?,etag:String?,modified:String?):HttpReply{
            requests.incrementAndGet();assertNull(bearer)
            if(offline)throw java.io.IOException("Fixture offline")
            return if(etag=="CI-ETAG"){conditional.incrementAndGet();HttpReply(304,"")}else HttpReply(200,xml,"CI-ETAG")
        }}
        val repository=NewsRepository(storage,transport)
        val first=repository.refresh(force=true)
        assertEquals(SourceStatus.READY,first.info.status);assertEquals(3,requests.get());assertEquals(1,first.items.size)
        assertEquals(setOf("Brasil","Economia","Tecnologia"),first.items.single().topics)
        repository.refresh();assertEquals(3,requests.get())
        assertEquals(first.items,repository.refresh(force=true).items);assertEquals(3,conditional.get())
        assertEquals(1,repository.select(first.items,setOf("tecnologia")).size)
        assertTrue(repository.select(first.items,setOf("assunto ausente")).isEmpty())
        offline=true
        val stale=repository.refresh(force=true)
        assertEquals(SourceStatus.STALE,stale.info.status);assertEquals(first.items,stale.items)
    }
    @Test fun weatherNeverFetchesWithoutOptInAndNullRainStaysUnknownInCache()=runBlocking {
        val requests=AtomicInteger();var offline=false
        val today=LocalDate.now(ZoneId.of("America/Sao_Paulo"))
        val response="""{"timezone":"America/Sao_Paulo","daily":{"time":["$today"],"temperature_2m_min":[15.0],"temperature_2m_max":[24.0],"precipitation_probability_max":[null]}}"""
        val transport=object:HttpTransport{override suspend fun get(url:String,bearer:String?,etag:String?,modified:String?):HttpReply{
            requests.incrementAndGet();assertNull(bearer);if(offline)throw java.io.IOException("Fixture offline")
            return HttpReply(200,response)
        }}
        val repository=WeatherRepository(storage,transport)
        assertEquals(SourceStatus.NOT_CONFIGURED,repository.refresh().info.status);assertEquals(0,requests.get())
        repository.configure(WeatherCity("Cidade CI",-23.55,-46.63));assertEquals(0,requests.get())
        val ready=repository.refresh();assertEquals(SourceStatus.READY,ready.info.status);assertNull(ready.rain)
        assertEquals(15.0,ready.low!!,0.0);assertEquals(24.0,ready.high!!,0.0)
        assertNull(repository.refresh().rain);assertEquals(1,requests.get())
        offline=true;val stale=repository.refresh(force=true);assertEquals(SourceStatus.STALE,stale.info.status);assertNull(stale.rain)
        repository.disable();assertEquals(SourceStatus.NOT_CONFIGURED,repository.refresh().info.status);assertEquals(2,requests.get())
    }
}
