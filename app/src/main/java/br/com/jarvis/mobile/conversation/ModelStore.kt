package br.com.jarvis.mobile.conversation

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale

internal data class ModelSpec(val name: String, val fileName: String, val url: String, val bytes: Long, val sha256: String) {
    companion object {
        fun read(context: Context): ModelSpec {
            val json = JSONObject(context.assets.open("conversation-model.json").bufferedReader().use { it.readText() })
            return ModelSpec(json.getString("name"), json.getString("fileName"), json.getString("url"), json.getLong("bytes"), json.getString("sha256"))
        }
    }
}

data class ModelState(val ready: Boolean = false, val downloading: Boolean = false, val progress: Int = 0,
    val message: String = "Conversa offline não preparada • download gratuito de 491 MB")

/** Only the pinned, hash-verified model is executable. Download is an explicit user action. */
class ModelStore(private val context: Context) {
    private val spec = ModelSpec.read(context)
    private val manager = context.getSystemService(DownloadManager::class.java)
    private val prefs = context.getSharedPreferences("jarvis-conversation", Context.MODE_PRIVATE)
    private val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: File(context.filesDir, "models")
    private val file = File(directory, spec.fileName)
    private val mutex = Mutex()
    private val _state = MutableStateFlow(ModelState())
    val state = _state.asStateFlow()

    fun readyFile(): File? = file.takeIf {
        it.isFile && it.length() == spec.bytes && prefs.getString("verified-sha", "") == spec.sha256 &&
            prefs.getLong("verified-time", -1) == it.lastModified()
    }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (readyFile() != null) { ready(); return@withLock }
            val id = prefs.getLong("download-id", -1)
            if (id >= 0) {
                manager.query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        if (status in listOf(DownloadManager.STATUS_PENDING, DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_RUNNING)) {
                            val bytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                            val percent = (bytes * 100L / spec.bytes).toInt().coerceIn(0, 100)
                            _state.value = ModelState(downloading = true, progress = percent,
                                message = if (status == DownloadManager.STATUS_PAUSED) "Download aguardando Wi-Fi ou espaço • $percent%" else "Preparando conversa offline • $percent% de 491 MB")
                            return@withLock
                        }
                        if (status == DownloadManager.STATUS_FAILED) {
                            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                            _state.value = ModelState(message = "O download não terminou (código $reason). Conecte ao Wi-Fi e tente novamente.")
                            prefs.edit().remove("download-id").apply()
                            return@withLock
                        }
                    } else prefs.edit().remove("download-id").apply()
                }
            }
            if (!file.isFile) {
                if (_state.value.downloading) _state.value = ModelState(message = "O download foi interrompido. Conecte ao Wi-Fi e tente novamente.")
                return@withLock
            }
            _state.value = ModelState(downloading = true, progress = 100, message = "Conferindo o modelo baixado…")
            if (verify(file)) { markVerified(); ready() }
            else {
                prefs.edit().remove("download-id").remove("verified-sha").apply()
                file.delete()
                _state.value = ModelState(message = "O arquivo está incompleto ou diferente do modelo esperado. Baixe novamente ou importe o modelo oficial.")
            }
        }
    }

    suspend fun download() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (readyFile() != null) { ready(); return@withLock }
            if (_state.value.downloading) return@withLock
            check(directory.exists() || directory.mkdirs()) { "Não foi possível preparar o armazenamento." }
            check(StatFs(directory.path).availableBytes >= spec.bytes + 64L * 1024 * 1024) { "Libere pelo menos 560 MB para preparar a conversa." }
            check(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) != null) { "Armazenamento indisponível. Importe o modelo pelos ajustes." }
            val old = prefs.getLong("download-id", -1)
            if (old >= 0) manager.remove(old)
            file.delete()
            val request = DownloadManager.Request(Uri.parse(spec.url))
                .setTitle("JARVIS • conversa offline")
                .setDescription("Modelo gratuito • download de 491 MB por Wi-Fi")
                .setMimeType("application/octet-stream")
                .setAllowedOverMetered(false).setAllowedOverRoaming(false)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, spec.fileName)
            val id = manager.enqueue(request)
            prefs.edit().putLong("download-id", id).remove("verified-sha").apply()
            _state.value = ModelState(downloading = true, message = "Download solicitado • conecte ao Wi-Fi • 491 MB")
        }
    }

    suspend fun cancelDownload() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val id = prefs.getLong("download-id", -1)
            if (id >= 0) manager.remove(id)
            prefs.edit().remove("download-id").apply()
            if (readyFile() == null) file.delete()
            _state.value = ModelState(message = "Preparação cancelada. Você pode baixar ou importar o modelo quando quiser.")
        }
    }

    suspend fun importModel(uri: Uri) {
        val input = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri) }
            ?: throw IllegalStateException("Não foi possível abrir o arquivo escolhido.")
        input.use { importStream(it) }
    }

    internal suspend fun importStream(input: InputStream) = withContext(Dispatchers.IO) {
        mutex.withLock {
            check(directory.exists() || directory.mkdirs())
            val old = prefs.getLong("download-id", -1)
            if (old >= 0 && _state.value.downloading) manager.remove(old)
            prefs.edit().remove("download-id").apply()
            _state.value = ModelState(downloading = true, message = "Importando e conferindo o modelo…")
            val temporary = File(directory, "${spec.fileName}.import")
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                var count = 0L
                temporary.outputStream().buffered().use { output ->
                    val buffer = ByteArray(256 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        count += read
                        check(count <= spec.bytes) { "Escolha o modelo Qwen2.5 0.5B Q4_K_M oficial de 491 MB." }
                        digest.update(buffer, 0, read); output.write(buffer, 0, read)
                    }
                }
                check(count == spec.bytes && hex(digest.digest()) == spec.sha256) { "Esse não é o modelo oficial esperado, ou o arquivo está incompleto." }
                check(!file.exists() || file.delete())
                check(temporary.renameTo(file)) { "Não foi possível guardar o modelo." }
                markVerified(); ready()
            } finally { temporary.delete() }
        }
    }

    fun reportFailure(message: String) { _state.value = ModelState(message = message) }
    private fun markVerified() { prefs.edit().putString("verified-sha", spec.sha256).putLong("verified-time", file.lastModified()).remove("download-id").apply() }
    private fun ready() { _state.value = ModelState(ready = true, message = "Conversa offline pronta • modelo local gratuito") }
    private suspend fun verify(candidate: File): Boolean {
        if (candidate.length() != spec.bytes) return false
        val digest = MessageDigest.getInstance("SHA-256")
        candidate.inputStream().buffered().use { input ->
            val buffer = ByteArray(256 * 1024)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return hex(digest.digest()) == spec.sha256
    }
    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(Locale.ROOT, it) }
}
