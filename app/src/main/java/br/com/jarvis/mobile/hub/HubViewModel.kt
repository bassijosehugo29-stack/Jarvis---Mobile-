package br.com.jarvis.mobile.hub

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.jarvis.mobile.JarvisApplication
import br.com.jarvis.mobile.brain.NoteCategory
import br.com.jarvis.mobile.brain.searchNotes
import br.com.jarvis.mobile.domain.Command
import com.google.android.gms.auth.api.identity.AuthorizationResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

data class HubState(val panel:HubPanel=HubPanel.HOME,val busy:Boolean=false,val notice:String="",
    val agenda:AgendaSnapshot=AgendaSnapshot(),val news:NewsSnapshot=NewsSnapshot(),val mail:MailSnapshot=MailSnapshot(),
    val weather:WeatherSnapshot=WeatherSnapshot(),val digest:DigestSnapshot?=null,val preferences:HubPreferences=HubPreferences(),
    val preferencesReady:Boolean=false,val newsTopic:String="",val cityResults:List<WeatherCity> = emptyList(),
    val mailDetail:String="",val now:Long=System.currentTimeMillis())

sealed interface HubEvent{
    data class Speak(val text:String,val privateData:Boolean=true):HubEvent
    data class Unlock(val command:Command):HubEvent
    data class CalendarPermission(val command:Command):HubEvent
    data object BrainChanged:HubEvent
    data object DataErased:HubEvent
    data object Finished:HubEvent
}

class HubViewModel(application:Application):AndroidViewModel(application){
    val data=(application as JarvisApplication).data
    private val _state=MutableStateFlow(HubState());val state=_state.asStateFlow()
    private val _events=MutableSharedFlow<HubEvent>(extraBufferCapacity=8);val events=_events.asSharedFlow()
    private var foreground=false;private var epoch=0L;private var job:Job?=null;private var ticker:Job?=null;private var agendaWatch:Job?=null
    private var pendingPreferences:HubPreferences?=null
    private var observing=false
    private val observer=object:ContentObserver(Handler(Looper.getMainLooper())){override fun onChange(selfChange:Boolean){
        agendaWatch?.cancel();agendaWatch=viewModelScope.launch{delay(500);if(foreground&&data.agenda.hasPermission()){
            val current=epoch;val loaded=data.agenda.load();if(current==epoch&&foreground)_state.update{it.copy(agenda=loaded)}
        }}
    }}
    init{
        viewModelScope.launch{data.preferences.state.collect{value->
            if(pendingPreferences==null||pendingPreferences==value){pendingPreferences=null;_state.update{it.copy(preferences=value,preferencesReady=true)}}
        }}
    }
    fun onForeground(){
        foreground=true
        ticker?.cancel();ticker=viewModelScope.launch{
            var date=LocalDate.now()
            while(isActive&&foreground){_state.update{it.copy(now=System.currentTimeMillis())};if(date!=LocalDate.now()){date=LocalDate.now();refreshAgenda()};delay(1000)}
        }
        if(!observing)try{
            val resolver=getApplication<Application>().contentResolver
            resolver.registerContentObserver(CalendarContract.Events.CONTENT_URI,true,observer)
            resolver.registerContentObserver(CalendarContract.Calendars.CONTENT_URI,true,observer);observing=true
        }catch(_:RuntimeException){getApplication<Application>().contentResolver.unregisterContentObserver(observer);observing=false}
        val current=epoch
        viewModelScope.launch{try{
            val mail=data.gmail.cached();val news=data.news.cached()
            if(foreground&&current==epoch)_state.update{it.copy(mail=mail,news=news)}
        }catch(e:CancellationException){throw e}catch(_:Exception){notice("Não foi possível abrir o cache local.")}}
        if(data.agenda.hasPermission())refreshAgenda()
    }
    fun onBackground(){foreground=false;epoch++;job?.cancel();ticker?.cancel();agendaWatch?.cancel();data.brain.lock()
        if(observing){getApplication<Application>().contentResolver.unregisterContentObserver(observer);observing=false}
        _state.update{it.copy(busy=false,agenda=AgendaSnapshot(),mail=MailSnapshot(),digest=null,mailDetail="",weather=WeatherSnapshot(),cityResults=emptyList())}
    }
    fun navigate(panel:HubPanel){epoch++;job?.cancel();_state.update{it.copy(panel=panel,busy=false,notice="",mailDetail="")}
        if(panel==HubPanel.AGENDA&&data.agenda.hasPermission())refreshAgenda()
    }
    fun notice(value:String){_state.update{it.copy(notice=value)}}
    fun cancelWork(){epoch++;job?.cancel();_state.update{it.copy(busy=false)}}
    fun updatePreferences(value:HubPreferences){
        val bounded=value.copy(topics=value.topics.map{it.trim().take(80)}.filter{it.isNotBlank()}.take(20).toSet())
        pendingPreferences=bounded;_state.update{it.copy(preferences=bounded)}
        viewModelScope.launch{try{data.preferences.update(bounded)}catch(e:CancellationException){throw e}catch(_:Exception){
            if(pendingPreferences==bounded)pendingPreferences=null
            notice("Não foi possível salvar esta preferência. Confira o armazenamento do aparelho.")
        }}
    }
    fun shouldAutoDigest():Boolean{
        val current=_state.value
        val date=LocalDate.now().toString()
        if(!current.preferencesReady||!current.preferences.autoDigest||current.preferences.lastAutoDate==date)return false
        updatePreferences(current.preferences.copy(lastAutoDate=date));return true
    }
    fun handle(command:Command){
        when(command){
            Command.OpenBrain,Command.AddNote->{navigate(HubPanel.BRAIN);_events.tryEmit(HubEvent.Unlock(command))}
            Command.Goals->{if(!data.brain.isUnlocked()){_events.tryEmit(HubEvent.Unlock(command));return};run{
                val notes=searchNotes(data.brain.notes(),"",NoteCategory.GOALS)
                _events.emit(HubEvent.Speak(if(notes.isEmpty())"Não há metas registradas na sua memória, senhor." else "Suas metas: "+notes.take(5).joinToString(". "){it.title+": "+it.content.take(220)}))
            }}
            is Command.Agenda->{navigate(HubPanel.AGENDA);if(!data.agenda.hasPermission()){_events.tryEmit(HubEvent.CalendarPermission(command));return};run{
                val snapshot=data.agenda.load();_state.update{it.copy(agenda=snapshot)}
                val selected=when(command.range){"week"->snapshot.entries;"next"->listOfNotNull(nextEntry(snapshot.entries,System.currentTimeMillis()));else->todayEntries(snapshot.entries,LocalDate.now(),ZoneId.systemDefault())}
                val text=if(snapshot.info.status!=SourceStatus.READY)snapshot.info.message else if(selected.isEmpty())"Não encontrei compromissos nesse período, senhor." else
                    selected.take(if(command.range=="week")8 else 4).joinToString(". "){agendaText(it)}
                _events.emit(HubEvent.Speak(text))
            }}
            is Command.News->{navigate(HubPanel.NEWS);_state.update{it.copy(newsTopic=command.topic.orEmpty())};refreshNews(speak=true)}
            is Command.Mail->{navigate(HubPanel.EMAIL);refreshMail(speak=true,actionOnly=command.actionOnly)}
            is Command.Digest->{
                navigate(HubPanel.DIGEST)
                if(!data.brain.isUnlocked()){
                    run{if(data.brain.count()>0)_events.emit(HubEvent.Unlock(command))else generateDigest()}
                }else run{generateDigest()}
            }
            else->Unit
        }
    }
    fun digestWithoutNotes()=run{generateDigest()}
    fun refreshAgenda()=run{val agenda=data.agenda.load();_state.update{it.copy(agenda=agenda)}}
    fun requestAgendaPermission(){_events.tryEmit(HubEvent.CalendarPermission(Command.Agenda()))}
    fun refreshNews(speak:Boolean=false)=run{
        val value=data.news.refresh(force=!speak);_state.update{it.copy(news=value)}
        if(speak){val topic=_state.value.newsTopic;val items=data.news.select(value.items,if(topic.isBlank())_state.value.preferences.topics else setOf(topic))
            _events.emit(HubEvent.Speak(if(items.isEmpty())"Não há manchetes disponíveis para esse assunto. ${value.info.message}" else
                (if(value.info.status==SourceStatus.STALE)"Do último cache: " else "Manchetes: ")+items.take(4).joinToString(". "){it.title+", fonte "+it.source},false))}
    }
    fun readHeadlines(){val items=visibleNews();_events.tryEmit(HubEvent.Speak(if(items.isEmpty())"Atualize as notícias para obter manchetes reais." else items.take(4).joinToString(". "){it.title+", fonte "+it.source},false))}
    fun visibleNews()=data.news.select(_state.value.news.items,if(_state.value.newsTopic.isBlank())_state.value.preferences.topics else setOf(_state.value.newsTopic))
    fun newsTopic(value:String){_state.update{it.copy(newsTopic=value.take(80))}}
    fun refreshMail(speak:Boolean=false,actionOnly:Boolean=false)=run{
        val result=data.gmail.refresh();_state.update{it.copy(mail=result)}
        if(speak){val selected=if(actionOnly)result.items.filter{it.category==EmailCategory.ACTION}else result.items
            _events.emit(HubEvent.Speak(if(result.info.status !in setOf(SourceStatus.READY,SourceStatus.STALE))result.info.message else
                "${selected.size} mensagens ${if(actionOnly)"classificadas como ação " else ""}entre as últimas mensagens sincronizadas da entrada. "+
                    selected.take(3).joinToString(". "){it.subject}))}
    }
    fun acceptAuthorization(result:AuthorizationResult,expectedEpoch:Long){
        viewModelScope.launch{
            try{val snapshot=data.gmail.acceptAuthorization(result,expectedEpoch);MailRefreshWorker.schedule(getApplication());if(foreground)_state.update{it.copy(mail=snapshot,notice="Gmail autorizado somente para leitura.")}}
            catch(e:CancellationException){throw e}catch(_:Exception){if(foreground)notice("Autorização não concluída. Confira OAuth, assinatura do APK e conta de teste no guia do Gmail.")}
        }
    }
    fun mailDetail(id:String)=run{val body=data.gmail.detail(id);_state.update{it.copy(mailDetail=body)}}
    fun closeMailDetail(){_state.update{it.copy(mailDetail="")}}
    fun classifyMail(id:String,category:EmailCategory)=run{data.gmail.classify(id,category);val mail=data.gmail.cached();_state.update{it.copy(mail=mail)}}
    fun disconnectMail()=run{
        MailRefreshWorker.cancel(getApplication());_state.update{it.copy(mail=MailSnapshot(),mailDetail="")}
        val revoked=withTimeoutOrNull(5000){data.gmail.disconnect()}?:false
        _state.update{it.copy(mail=MailSnapshot(),mailDetail="",notice=if(revoked)"Conta desconectada e cache local removido." else "Cache e conexão locais removidos. Sem rede, a revogação no Google não foi confirmada; confira os acessos da sua Conta Google.")}
    }
    fun findCity(name:String)=run{val result=data.weather.search(name);_state.update{it.copy(cityResults=result,notice=if(result.isEmpty())"Nenhuma cidade encontrada." else "Escolha a cidade para autorizar a consulta de clima.")}}
    fun selectCity(city:WeatherCity)=run{data.weather.configure(city);val weather=data.weather.refresh();_state.update{it.copy(weather=weather,cityResults=emptyList())}}
    fun disableWeather()=run{data.weather.disable();_state.update{it.copy(weather=WeatherSnapshot(),cityResults=emptyList())}}
    fun readDigest(){_state.value.digest?.let{_events.tryEmit(HubEvent.Speak(it.text))}}
    private suspend fun generateDigest(){
        val current=epoch
        val values=supervisorScope{
            val a=async{source(AgendaSnapshot(info=SourceInfo(SourceStatus.ERROR,"Agenda indisponível"))){data.agenda.load()}}
            val n=async{source(NewsSnapshot(info=SourceInfo(SourceStatus.ERROR,"Notícias indisponíveis"))){data.news.refresh()}}
            val m=async{source(MailSnapshot(info=SourceInfo(SourceStatus.ERROR,"E-mails indisponíveis"))){data.gmail.cached()}}
            val w=async{source(WeatherSnapshot(info=SourceInfo(SourceStatus.ERROR,"Clima indisponível"))){data.weather.refresh()}}
            listOf(a.await(),n.await(),m.await(),w.await())
        }
        if(!foreground||current!=epoch)return
        val agenda=values[0] as AgendaSnapshot;val news=values[1] as NewsSnapshot;val mail=values[2] as MailSnapshot;val weather=values[3] as WeatherSnapshot
        val notes=if(data.brain.isUnlocked())source<List<br.com.jarvis.mobile.brain.Note>?>(null){data.brain.notes()}else null
        val visible=news.copy(items=data.news.select(news.items,_state.value.preferences.topics))
        val digest=DigestBuilder.build(Instant.now(),ZoneId.systemDefault(),agenda,mail,visible,weather,notes,_state.value.preferences.mediumDigest)
        _state.update{it.copy(agenda=agenda,news=news,mail=mail,weather=weather,digest=digest)}
        _events.emit(HubEvent.Speak(digest.text))
    }
    private suspend fun <T> source(fallback:T,block:suspend()->T):T=try{withTimeout(12000){block()}}
        catch(_:TimeoutCancellationException){fallback}catch(e:CancellationException){throw e}catch(_:Exception){fallback}
    fun erasePersonalData()=run{
        MailRefreshWorker.cancel(getApplication())
        val revoked=withTimeoutOrNull(5000){data.gmail.disconnect()}?:false
        data.brain.erase();data.storage.eraseAll();pendingPreferences=null;data.preferences.clear()
        getApplication<Application>().getSharedPreferences("jarvis-calendar",Application.MODE_PRIVATE).edit().clear().apply()
        _state.value=HubState(panel=HubPanel.PRIVACY,notice="Dados pessoais locais e chaves removidos. Agendas, e-mails originais e backups externos não foram apagados."+
            if(revoked)"" else " Revogação Google pendente: confira a Conta Google quando houver rede.")
        _events.emit(HubEvent.DataErased)
    }
    private fun agendaText(item:AgendaEntry):String{
        val formatter=DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm",Locale.forLanguageTag("pt-BR")).withZone(ZoneId.systemDefault())
        return item.title+if(item.allDay)", dia inteiro, agenda ${item.calendar}" else ", ${formatter.format(Instant.ofEpochMilli(item.start))}, agenda ${item.calendar}"
    }
    private fun run(block:suspend()->Unit){
        if(!foreground)return
        job?.cancel();val current=++epoch
        job=viewModelScope.launch{
            _state.update{it.copy(busy=true,notice="")}
            try{block()}catch(e:CancellationException){throw e}catch(_:Exception){if(foreground&&current==epoch)notice("Não foi possível concluir. Confira a conexão, a permissão ou o desbloqueio; dados ausentes não foram estimados.")}
            finally{if(current==epoch){_state.update{it.copy(busy=false)};_events.tryEmit(HubEvent.Finished)}}
        }
    }
    override fun onCleared(){onBackground();super.onCleared()}
}
