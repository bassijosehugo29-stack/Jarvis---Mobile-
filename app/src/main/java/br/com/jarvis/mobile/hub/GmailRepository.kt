package br.com.jarvis.mobile.hub

import android.accounts.Account
import android.content.Context
import android.text.Html
import com.google.android.gms.auth.api.identity.*
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GmailConsentRequired:Exception()
class GmailRepository(private val context:Context,private val storage:HubStorage,private val http:NetworkClient){
    private val scope="https://www.googleapis.com/auth/gmail.readonly"
    fun request(email:String?=null):AuthorizationRequest = AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(scope))).apply{
        if(!email.isNullOrBlank())setAccount(Account(email,"com.google"))
    }.build()
    suspend fun connectedAccount():String?=storage.get("gmail-account")?.optString("email")?.takeIf{it.isNotBlank()}
    suspend fun acceptAuthorization(result:AuthorizationResult,expectedEpoch:Long=storage.epoch()):MailSnapshot{
        check(expectedEpoch==storage.epoch())
        check(!result.hasResolution())
        val token=result.accessToken?:throw GmailConsentRequired()
        val epoch=expectedEpoch;val profile=http.get("https://gmail.googleapis.com/gmail/v1/users/me/profile",bearer=token)
        check(profile.code==200);val email=JSONObject(profile.body).getString("emailAddress")
        val previous=connectedAccount()
        check(epoch==storage.epoch())
        if(previous!=null&&previous!=email)storage.disconnectMail()
        storage.put("gmail-account",JSONObject().put("email",email),storage.epoch())
        check(epoch==storage.epoch()||previous!=email)
        return refresh(token)
    }
    suspend fun cached():MailSnapshot{
        val email=connectedAccount()?:return MailSnapshot()
        val data=storage.get("mail")?:return MailSnapshot(account=email,info=SourceInfo(SourceStatus.NEEDS_AUTH,"Conectado; atualize para consultar mensagens."))
        val overrides=storage.get("mail-overrides")?:JSONObject()
        val array=data.optJSONArray("items")?:JSONArray()
        val items=(0 until array.length()).map{decode(array.getJSONObject(it),overrides)}
        return MailSnapshot(email,items,SourceInfo(SourceStatus.STALE,"Cache local · até 100 mensagens da entrada dos últimos sete dias",data.optLong("time")))
    }
    private suspend fun token():String{
        val email=connectedAccount()?:throw GmailConsentRequired()
        val result=Identity.getAuthorizationClient(context).authorize(request(email)).awaitTask()
        if(result.hasResolution())throw GmailConsentRequired()
        return result.accessToken?:throw GmailConsentRequired()
    }
    suspend fun refresh(accessToken:String?=null):MailSnapshot=coroutineScope{
        val epoch=storage.epoch()
        try{
            val email=connectedAccount()?:return@coroutineScope MailSnapshot()
            val auth=accessToken?:token()
            val listing=http.get("https://gmail.googleapis.com/gmail/v1/users/me/messages?labelIds=INBOX&maxResults=100&q=newer_than%3A7d",bearer=auth)
            if(listing.code==401){Identity.getAuthorizationClient(context).clearToken(ClearTokenRequest.builder().setToken(auth).build()).awaitTask();throw GmailConsentRequired()}
            if(listing.code==403)throw GmailConsentRequired()
            check(listing.code==200)
            val array=JSONObject(listing.body).optJSONArray("messages")?:JSONArray()
            val result=mutableListOf<EmailItem>()
            for(start in 0 until array.length() step 4){
                val batch=(start until minOf(start+4,array.length())).map{n->async{
                    val id=array.getJSONObject(n).getString("id");require(id.matches(Regex("[A-Za-z0-9_-]+")))
                    val reply=http.get("https://gmail.googleapis.com/gmail/v1/users/me/messages/$id?format=metadata&metadataHeaders=From&metadataHeaders=Subject",bearer=auth)
                    if(reply.code==401||reply.code==403)throw GmailConsentRequired()
                    check(reply.code==200);parseMessage(JSONObject(reply.body),email)
                }}.awaitAll();result+=batch
            }
            val overrides=storage.get("mail-overrides")?:JSONObject()
            val data=JSONObject().put("time",System.currentTimeMillis()).put("items",JSONArray(result.map(::encode)))
            storage.put("mail",data,epoch)
            MailSnapshot(email,result.map{decode(encode(it),overrides)},SourceInfo(SourceStatus.READY,"Até 100 mensagens da entrada · últimos sete dias · classificação local sugerida",data.getLong("time")))
        }catch(e:CancellationException){throw e}catch(_:GmailConsentRequired){cached().let{it.copy(info=it.info.copy(status=SourceStatus.NEEDS_AUTH,message="Autorize ou reconecte o Gmail no aplicativo."))}}
        catch(_:Exception){cached().let{it.copy(info=it.info.copy(status=if(it.items.isEmpty())SourceStatus.ERROR else SourceStatus.STALE,message="Gmail indisponível. O cache não confirma mensagens novas."))}}
    }
    suspend fun detail(id:String):String{
        require(id.matches(Regex("[A-Za-z0-9_-]+")))
        val auth=token();val reply=http.get("https://gmail.googleapis.com/gmail/v1/users/me/messages/$id?format=full",bearer=auth)
        check(reply.code==200)
        fun body(payload:JSONObject):String{
            val mime=payload.optString("mimeType");val value=payload.optJSONObject("body")?.optString("data").orEmpty()
            if(value.isNotBlank()&&mime in setOf("text/plain","text/html")){
                val raw=String(android.util.Base64.decode(value,android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP),Charsets.UTF_8)
                return if(mime=="text/html")Html.fromHtml(raw,Html.FROM_HTML_MODE_LEGACY).toString() else raw
            }
            val parts=payload.optJSONArray("parts")?:return ""
            val sorted=(0 until parts.length()).map{parts.getJSONObject(it)}.sortedBy{if(it.optString("mimeType")=="text/plain")0 else 1}
            return sorted.firstNotNullOfOrNull{body(it).takeIf{v->v.isNotBlank()}}.orEmpty()
        }
        return body(JSONObject(reply.body).getJSONObject("payload")).take(60000).ifBlank{"Sem conteúdo textual disponível. Anexos e imagens externas não são carregados."}
    }
    suspend fun classify(id:String,category:EmailCategory){
        val data=storage.get("mail-overrides")?:JSONObject();data.put(id,category.name);storage.put("mail-overrides",data,storage.epoch())
    }
    suspend fun disconnect():Boolean{
        val email=connectedAccount();var revoked=true
        if(email!=null)try{Identity.getAuthorizationClient(context).revokeAccess(RevokeAccessRequest.builder().setAccount(Account(email,"com.google")).setScopes(listOf(Scope(scope))).build()).awaitTask()}
        catch(e:CancellationException){throw e}catch(_:Exception){revoked=false}
        storage.disconnectMail();return revoked
    }
    internal fun parseMessage(json:JSONObject,email:String):EmailItem{
        val headers=json.optJSONObject("payload")?.optJSONArray("headers")?:JSONArray()
        fun header(name:String)=(0 until headers.length()).map{headers.getJSONObject(it)}.firstOrNull{it.optString("name").equals(name,true)}?.optString("value").orEmpty()
        val labels=json.optJSONArray("labelIds")?.let{a->(0 until a.length()).map{a.getString(it)}.toSet()}.orEmpty()
        val subject=header("Subject").take(500).ifBlank{"Sem assunto"}
        val preview=Html.fromHtml(json.optString("snippet"),Html.FROM_HTML_MODE_LEGACY).toString().take(500)
        return EmailItem(json.getString("id"),email,header("From").take(500),subject,preview,json.optString("internalDate").toLongOrNull()?:0,
            classifyEmail(subject,preview,labels),"UNREAD" in labels)
    }
    private fun encode(item:EmailItem)=JSONObject().put("id",item.id).put("account",item.account).put("sender",item.sender).put("subject",item.subject)
        .put("preview",item.preview).put("time",item.receivedAt).put("category",item.category.name).put("unread",item.unread)
    private fun decode(json:JSONObject,overrides:JSONObject):EmailItem{
        val id=json.getString("id");val manual=overrides.optString(id).takeIf{it.isNotBlank()}
        return EmailItem(id,json.getString("account"),json.getString("sender"),json.getString("subject"),json.getString("preview"),json.getLong("time"),
            EmailCategory.valueOf(manual?:json.getString("category")),json.optBoolean("unread"),manual!=null)
    }
}

private suspend fun <T> Task<T>.awaitTask():T=suspendCancellableCoroutine{continuation->
    addOnSuccessListener{if(continuation.isActive)continuation.resume(it)}
    addOnFailureListener{if(continuation.isActive)continuation.resumeWithException(it)}
    addOnCanceledListener{continuation.cancel()}
}
