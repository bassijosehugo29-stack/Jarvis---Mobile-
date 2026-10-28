package br.com.jarvis.mobile

import android.app.Application
import android.content.Context
import br.com.jarvis.mobile.brain.BrainStore
import br.com.jarvis.mobile.hub.*

class JarvisApplication:Application(){val data:AppData by lazy{AppData(this)}}
class AppData(context:Context){
    val brain=BrainStore(context)
    val storage=HubStorage(context)
    private val http=NetworkClient()
    val preferences=HubPreferencesStore(context)
    val agenda=AgendaRepository(context)
    val news=NewsRepository(storage,http)
    val weather=WeatherRepository(storage,http)
    val gmail=GmailRepository(context.applicationContext,storage,http)
}
