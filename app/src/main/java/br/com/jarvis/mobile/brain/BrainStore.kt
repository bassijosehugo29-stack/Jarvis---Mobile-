package br.com.jarvis.mobile.brain

import android.content.Context
import androidx.room.withTransaction
import br.com.jarvis.mobile.security.BackupCipher
import br.com.jarvis.mobile.security.PrivateCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Shared gate is closed on background. Draft suggestions exist in RAM only until confirmed. */
class BrainStore(context: Context) {
    private val db=PersonalDatabase.get(context)
    private val cipher=PrivateCipher("jarvis-brain-v1")
    private val mutex=Mutex()
    @Volatile private var unlocked=false
    @Volatile private var generation=0L
    fun unlock() { unlocked=true }
    fun lock() { unlocked=false; generation++ }
    fun isUnlocked() = unlocked
    private fun requireOpen() = check(unlocked) { "Desbloqueie o Second Brain para acessar as notas." }
    suspend fun count(): Int = withContext(Dispatchers.IO) { db.records().count("note") }
    suspend fun notes(): List<Note> = withContext(Dispatchers.IO) {
        requireOpen(); val current=generation
        db.records().records("note").map { record ->
            decode(JSONObject(String(cipher.decrypt(record.payload,"note:${record.id}"),Charsets.UTF_8))).also {
                check(current==generation && unlocked) { "A memória foi bloqueada." }
            }
        }
    }
    suspend fun save(note: Note) = withContext(Dispatchers.IO) { mutex.withLock {
        requireOpen(); validate(note)
        val record=record(note); requireOpen(); db.records().put(record)
    } }
    suspend fun delete(id: String) = withContext(Dispatchers.IO) { mutex.withLock { requireOpen(); db.records().delete("note",id) } }
    suspend fun export(password: CharArray): ByteArray = withContext(Dispatchers.IO) {
        val json=JSONObject().put("version",1).put("notes",JSONArray(notes().map(::encode)))
        BackupCipher.encrypt(json.toString().toByteArray(Charsets.UTF_8),password)
    }
    suspend fun previewBackup(bytes: ByteArray,password: CharArray): List<Note> = withContext(Dispatchers.IO) {
        requireOpen()
        val json=JSONObject(String(BackupCipher.decrypt(bytes,password),Charsets.UTF_8))
        require(json.getInt("version")==1) { "Versão de backup incompatível." }
        val array=json.getJSONArray("notes")
        require(array.length() <= 10000) { "Quantidade de notas excedida." }
        val imported=(0 until array.length()).map { decode(array.getJSONObject(it)).also(::validate) }
        require(imported.map { it.id }.distinct().size==imported.size) { "Notas duplicadas no backup." }
        requireOpen(); imported
    }
    suspend fun importConfirmed(imported: List<Note>) = withContext(Dispatchers.IO) { mutex.withLock {
        requireOpen(); val merged=mergeNotes(notes(),imported)
        val records=merged.map(::record); requireOpen()
        db.withTransaction { db.records().putAll(records) }
    } }
    suspend fun erase() = withContext(Dispatchers.IO) { mutex.withLock {
        lock(); db.records().clearKind("note"); cipher.destroy()
    } }
    private fun record(note: Note)=PersonalRecord("note",note.id,note.updatedAt,cipher.encrypt(encode(note).toString().toByteArray(Charsets.UTF_8),"note:${note.id}"))
    private fun validate(note: Note) {
        java.util.UUID.fromString(note.id)
        require(note.title.isNotBlank() && note.title.length<=160) { "Informe um título de até 160 caracteres." }
        require(note.content.isNotBlank() && note.content.length<=20000) { "Informe um conteúdo de até 20 mil caracteres." }
        require(note.tags.size<=30 && note.links.size<=100 && note.tags.all { it.length<=80 }) { "Tags ou vínculos excedidos." }
        require(note.updatedAt>=0); note.links.forEach { java.util.UUID.fromString(it) }
    }
    private fun encode(note: Note)=JSONObject().put("id",note.id).put("title",note.title).put("category",note.category.name)
        .put("content",note.content).put("updatedAt",note.updatedAt).put("tags",JSONArray(note.tags)).put("links",JSONArray(note.links))
        .put("digest",note.includeInDigest).put("pinned",note.pinned)
    private fun decode(json: JSONObject): Note {
        fun strings(key:String): List<String> = json.optJSONArray(key)?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty()
        return Note(json.getString("id"),json.getString("title"),NoteCategory.valueOf(json.getString("category")),
            json.getString("content"),json.getLong("updatedAt"),strings("tags"),strings("links"),json.optBoolean("digest"),json.optBoolean("pinned"))
    }
}
