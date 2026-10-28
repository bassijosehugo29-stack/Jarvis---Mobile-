package br.com.jarvis.mobile.hub

import android.content.Context
import br.com.jarvis.mobile.brain.PersonalDatabase
import br.com.jarvis.mobile.brain.PersonalRecord
import br.com.jarvis.mobile.security.PrivateCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Cache/token metadata key is separate from the foreground-only notes access gate. */
class HubStorage(context: Context) {
    private val db=PersonalDatabase.get(context)
    private val meta=context.applicationContext.getSharedPreferences("jarvis-data-epoch",Context.MODE_PRIVATE)
    private val cipher=PrivateCipher("jarvis-data-v1")
    fun epoch():Long=meta.getLong("epoch",0L)
    suspend fun get(kind:String):JSONObject? = withContext(Dispatchers.IO) { lock.withLock {
        db.records().records(kind).firstOrNull()?.let { JSONObject(String(cipher.decrypt(it.payload,kind),Charsets.UTF_8)) }
    } }
    suspend fun put(kind:String,value:JSONObject,expectedEpoch:Long)=withContext(Dispatchers.IO){lock.withLock{
        check(epoch()==expectedEpoch){"Dados invalidados."}
        db.records().put(PersonalRecord(kind,"current",System.currentTimeMillis(),cipher.encrypt(value.toString().toByteArray(Charsets.UTF_8),kind)))
    }}
    suspend fun remove(kind:String)=withContext(Dispatchers.IO){lock.withLock{db.records().clearKind(kind)}}
    suspend fun disconnectMail()=withContext(Dispatchers.IO){lock.withLock{
        check(meta.edit().putLong("epoch",epoch()+1).commit())
        for(kind in listOf("gmail-account","mail","mail-overrides"))db.records().clearKind(kind)
    }}
    suspend fun eraseAll()=withContext(Dispatchers.IO){lock.withLock{
        check(meta.edit().putLong("epoch",epoch()+1).commit())
        db.records().clear();cipher.destroy()
    }}
    companion object { private val lock=Mutex() }
}
