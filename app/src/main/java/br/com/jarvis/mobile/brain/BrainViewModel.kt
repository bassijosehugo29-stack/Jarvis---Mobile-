package br.com.jarvis.mobile.brain

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.jarvis.mobile.JarvisApplication
import br.com.jarvis.mobile.security.BackupCipher
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class BrainState(val unlocked:Boolean=false,val busy:Boolean=false,val notes:List<Note> = emptyList(),
    val error:String="",val draft:Note?=null,val importUri:Uri?=null,val importPreview:List<Note>?=null)

class BrainViewModel(application:Application):AndroidViewModel(application){
    private val store=(application as JarvisApplication).data.brain
    private val _state=MutableStateFlow(BrainState());val state=_state.asStateFlow()
    private val _exports=MutableSharedFlow<ByteArray>(extraBufferCapacity=1);val exports=_exports.asSharedFlow()
    private var foreground=false;private var job:Job?=null;private var epoch=0L
    fun foreground(){foreground=true}
    fun background(){foreground=false;epoch++;job?.cancel();store.lock();_state.value=BrainState()}
    fun unlocked(){store.unlock();_state.update{it.copy(unlocked=true,error="")};reload()}
    fun error(message:String){_state.update{it.copy(error=message)}}
    fun reload()=run{val notes=store.notes();_state.update{it.copy(notes=notes)}}
    fun edit(note:Note?=null){if(!store.isUnlocked())return;_state.update{it.copy(draft=note?:Note(title="",category=NoteCategory.YOU,content=""),error="")}}
    fun suggest(content:String){if(!store.isUnlocked())return;_state.update{it.copy(draft=Note(title="",category=NoteCategory.YOU,content=content.take(20000)),error="")}}
    fun closeEditor(){_state.update{it.copy(draft=null)}}
    fun save(note:Note)=run{store.save(note.copy(updatedAt=System.currentTimeMillis()));val notes=store.notes();_state.update{it.copy(draft=null,notes=notes)}}
    fun delete(id:String)=run{store.delete(id);val notes=store.notes();_state.update{it.copy(draft=null,notes=notes)}}
    fun export(password:CharArray)=run{
        try{val bytes=store.export(password);if(foreground)_exports.emit(bytes)}finally{password.fill('\u0000')}
    }
    fun selectImport(uri:Uri){_state.update{it.copy(importUri=uri,importPreview=null,error="")}}
    fun cancelImport(){epoch++;job?.cancel();job=null;_state.update{it.copy(importUri=null,importPreview=null,busy=false,error="")}}
    fun preview(password:CharArray)=run{
        try{
            val uri=_state.value.importUri?:return@run
            val bytes=withContext(Dispatchers.IO){
                requireNotNull(getApplication<Application>().contentResolver.openInputStream(uri)).use{input->
                    val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(32768)
                    while(true){currentCoroutineContext().ensureActive();val n=input.read(buffer);if(n<0)break
                        require(output.size()+n<=BackupCipher.MAX_BYTES);output.write(buffer,0,n)};output.toByteArray()
                }
            }
            val notes=store.previewBackup(bytes,password)
            _state.update{it.copy(importPreview=notes)}
        }finally{password.fill('\u0000')}
    }
    fun confirmImport()=run{store.importConfirmed(_state.value.importPreview?:return@run);val notes=store.notes();_state.update{it.copy(notes=notes,importUri=null,importPreview=null)}}
    private fun run(block:suspend()->Unit){
        if(!foreground||!store.isUnlocked()||job?.isActive==true)return
        val current=epoch
        job=viewModelScope.launch{
            _state.update{it.copy(busy=true,error="")}
            try{block()}catch(e:CancellationException){throw e}
            catch(_:Exception){if(current==epoch&&foreground)_state.update{it.copy(error="Não foi possível concluir. Confira os campos, a senha do backup e o armazenamento.")}}
            finally{if(current==epoch)_state.update{it.copy(busy=false)}}
        }
    }
}
