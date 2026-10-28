package br.com.jarvis.mobile.hub

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class HttpReply(val code:Int,val body:String,val etag:String?=null,val modified:String?=null)
interface HttpTransport {
    suspend fun get(url:String,bearer:String?=null,etag:String?=null,modified:String?=null):HttpReply
}

/** No logging interceptor. Authorization is never attached to public RSS/weather requests. */
class NetworkClient:HttpTransport {
    override suspend fun get(url:String,bearer:String?,etag:String?,modified:String?):HttpReply=withContext(Dispatchers.IO){
        var target=URL(url);var redirects=0
        while(true){
            currentCoroutineContext().ensureActive()
            require(target.protocol=="https")
            if(bearer!=null) require(target.host in setOf("gmail.googleapis.com","www.googleapis.com")&&target.path.startsWith("/gmail/v1/"))
            val connection=target.openConnection() as HttpsURLConnection
            try{
                connection.instanceFollowRedirects=false;connection.connectTimeout=8000;connection.readTimeout=8000
                connection.setRequestProperty("Accept","application/json, application/rss+xml, application/atom+xml, application/xml, text/xml")
                connection.setRequestProperty("User-Agent","JARVIS-Mobile/3.0 personal-reader")
                bearer?.let{connection.setRequestProperty("Authorization","Bearer $it")}
                etag?.let{connection.setRequestProperty("If-None-Match",it)}
                modified?.let{connection.setRequestProperty("If-Modified-Since",it)}
                val code=connection.responseCode
                if(code in setOf(301,302,303,307,308)){
                    require(bearer==null&&redirects++<3)
                    val next=URL(target,connection.getHeaderField("Location"))
                    fun ebc(host:String)=host=="ebc.com.br"||host.endsWith(".ebc.com.br")
                    require(next.protocol=="https" && (next.host==target.host || (ebc(target.host)&&ebc(next.host))))
                    target=next;continue
                }
                if(code==304)return@withContext HttpReply(code,"",connection.getHeaderField("ETag"),connection.getHeaderField("Last-Modified"))
                if(code !in 200..299)return@withContext HttpReply(code,"")
                val bytes=connection.inputStream.use{input->
                    val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(16384)
                    while(true){currentCoroutineContext().ensureActive();val n=input.read(buffer);if(n<0)break
                        require(out.size()+n<=2*1024*1024){"Resposta excedida."};out.write(buffer,0,n)}
                    out.toByteArray()
                }
                return@withContext HttpReply(code,String(bytes,Charsets.UTF_8),connection.getHeaderField("ETag"),connection.getHeaderField("Last-Modified"))
            }finally{connection.disconnect()}
        }
        @Suppress("UNREACHABLE_CODE") error("Resposta indisponível")
    }
}
