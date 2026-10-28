package br.com.jarvis.mobile.hub

import android.util.Xml
import br.com.jarvis.mobile.brain.normalizedSearch
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.net.URI
import java.time.*
import java.time.format.DateTimeFormatter

data class NewsFeed(val url:String,val topic:String)
object RssParser {
    fun parse(xml:String,source:String,topic:String,now:Long=System.currentTimeMillis()):List<NewsItem>{
        require(xml.length<=2*1024*1024)
        val parser=Xml.newPullParser();parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,true)
        parser.setInput(java.io.StringReader(xml))
        var type=parser.eventType;var recognized=false;var inside=false
        var title="";var link="";var date="";var id="";val items=mutableListOf<NewsItem>()
        while(type!=XmlPullParser.END_DOCUMENT){
            require(type!=XmlPullParser.DOCDECL){"Declarações externas não são aceitas."}
            if(type==XmlPullParser.START_TAG){
                val name=parser.name.lowercase()
                if(name in setOf("rss","feed"))recognized=true
                if(name in setOf("item","entry")){inside=true;title="";link="";date="";id=""}
                else if(inside){
                    when(name){
                        "title"->title=parser.nextText().take(500)
                        "link"->{val href=parser.getAttributeValue(null,"href");if(href!=null){if(parser.getAttributeValue(null,"rel") in listOf(null,"alternate"))link=href}else link=parser.nextText()}
                        "pubdate","published","date"->date=parser.nextText()
                        "updated"->if(date.isBlank())date=parser.nextText()
                        "guid","id"->id=parser.nextText().take(500)
                    }
                }
            }else if(type==XmlPullParser.END_TAG&&parser.name.lowercase() in setOf("item","entry")&&inside){
                inside=false
                val uri=runCatching{URI(link.trim())}.getOrNull()
                if(title.isNotBlank()&&link.length<=2048&&uri?.scheme in setOf("http","https")&&!uri?.host.isNullOrBlank()){
                    val safe=if(uri!!.scheme=="http")"https"+link.trim().removePrefix("http") else link.trim()
                    val published=parseDate(date)?.takeIf{it<=now+300000L}
                    items+=NewsItem(id.ifBlank{safe},android.text.Html.fromHtml(title,android.text.Html.FROM_HTML_MODE_LEGACY).toString(),safe,source,published,setOf(topic))
                }
                if(items.size>=200)break
            }
            type=parser.nextToken()
        }
        require(recognized){"A fonte não retornou RSS/Atom."};return items
    }
    private fun parseDate(value:String):Long?=runCatching{Instant.parse(value).toEpochMilli()}.getOrNull()
        ?:runCatching{OffsetDateTime.parse(value).toInstant().toEpochMilli()}.getOrNull()
        ?:runCatching{ZonedDateTime.parse(value,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()}.getOrNull()
}

class NewsRepository(private val storage:HubStorage,private val http:HttpTransport){
    val feeds=listOf(
        NewsFeed("https://agenciabrasil.ebc.com.br/rss/ultimasnoticias/feed.xml","Brasil"),
        NewsFeed("https://agenciabrasil.ebc.com.br/rss/economia/feed.xml","Economia"),
        NewsFeed("https://agenciabrasil.ebc.com.br/radioagencia-nacional/rss/pesquisa-e-inovacao/feed.xml","Tecnologia"))
    suspend fun cached():NewsSnapshot {
        val raw=storage.get("news")?:return NewsSnapshot()
        return NewsSnapshot(decodeItems(raw.optJSONArray("items")),SourceInfo(SourceStatus.STALE,"Notícias armazenadas; confira a última atualização.",raw.optLong("time")))
    }
    suspend fun refresh(force:Boolean=false):NewsSnapshot=coroutineScope{
        val epoch=storage.epoch();val saved=storage.get("news");val now=System.currentTimeMillis()
        if(!force&&saved!=null&&now-saved.optLong("time")<900000L)return@coroutineScope NewsSnapshot(decodeItems(saved.optJSONArray("items")),SourceInfo(SourceStatus.READY,"Cache recente da EBC",saved.optLong("time")))
        val results=feeds.map{feed->async{
            try{
                val previous=saved?.optJSONObject("feeds")?.optJSONObject(feed.url)
                val reply=http.get(feed.url,etag=previous?.optString("etag")?.takeIf{it.isNotBlank()},modified=previous?.optString("modified")?.takeIf{it.isNotBlank()})
                val body=if(reply.code==304)previous?.getString("body")?:error("Sem cache")else{check(reply.code in 200..299);reply.body}
                val parsed=withContext(Dispatchers.Default){RssParser.parse(body,"EBC / Agência Brasil",feed.topic,now)}
                // Keep each Room row below CursorWindow limits, even for unusually large feeds.
                val cache=if(body.toByteArray(Charsets.UTF_8).size<=128*1024)
                    JSONObject().put("body",body).put("etag",reply.etag?:previous?.optString("etag")).put("modified",reply.modified?:previous?.optString("modified")) else JSONObject()
                Triple(feed,parsed,cache)
            }catch(e:CancellationException){throw e}catch(_:Exception){null}
        }}.awaitAll()
        val succeeded=results.filterNotNull()
        if(succeeded.isEmpty())return@coroutineScope cached().let{it.copy(info=it.info.copy(status=if(it.items.isEmpty())SourceStatus.ERROR else SourceStatus.STALE,message="Fontes indisponíveis; nenhum dado foi inventado."))}
        val old=decodeItems(saved?.optJSONArray("items"));val all=(succeeded.flatMap{it.second}+old).filter{it.publishedAt==null||it.publishedAt>=now-7*86400000L}
            .groupBy{it.url}.values.map{group->group.first().copy(topics=group.flatMap{it.topics}.toSet())}.sortedByDescending{it.publishedAt?:0L}.take(500)
        val feedCache=JSONObject();succeeded.forEach{feedCache.put(it.first.url,it.third)}
        var kept=all
        fun cache()=JSONObject().put("time",now).put("items",encodeItems(kept)).put("feeds",feedCache)
        while(cache().toString().toByteArray(Charsets.UTF_8).size>1024*1024&&kept.isNotEmpty())kept=kept.dropLast(1)
        storage.put("news",cache(),epoch)
        NewsSnapshot(kept,SourceInfo(SourceStatus.READY,"${succeeded.size}/${feeds.size} fontes atualizadas · EBC",now))
    }
    fun select(items:List<NewsItem>,topics:Set<String>):List<NewsItem>{
        if(topics.isEmpty())return emptyList()
        return items.filter{item->topics.any{topic->
            val key=normalizedSearch(topic)
            key=="brasil"||item.topics.any{normalizedSearch(it)==key}||normalizedSearch(item.title).contains(key)||
                (key=="tecnologia"&&Regex("tecnolog|inovacao|ciencia|inteligencia artificial|digital|software").containsMatchIn(normalizedSearch(item.title)))
        }}
    }
    private fun encodeItems(items:List<NewsItem>)=JSONArray(items.map{JSONObject().put("id",it.id).put("title",it.title).put("url",it.url).put("source",it.source).put("date",it.publishedAt?:JSONObject.NULL).put("topics",JSONArray(it.topics.toList()))})
    private fun decodeItems(array:JSONArray?):List<NewsItem> = array?.let{a->(0 until a.length()).map{n->val o=a.getJSONObject(n);val t=o.getJSONArray("topics");NewsItem(o.getString("id"),o.getString("title"),o.getString("url"),o.getString("source"),if(o.isNull("date"))null else o.getLong("date"),(0 until t.length()).map{t.getString(it)}.toSet())}}.orEmpty()
}
