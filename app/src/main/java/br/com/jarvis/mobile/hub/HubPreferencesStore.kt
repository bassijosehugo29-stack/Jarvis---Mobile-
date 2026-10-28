package br.com.jarvis.mobile.hub

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.hubPreferences by preferencesDataStore("jarvis_hub")
class HubPreferencesStore(context:Context) {
    private val store=context.applicationContext.hubPreferences
    val state=store.data.catch{if(it is IOException)emit(emptyPreferences())else throw it}.map{
        HubPreferences(it[topics]?:setOf("Brasil","Tecnologia"),it[medium]?:false,it[automatic]?:true,it[date].orEmpty()) }
    suspend fun update(value:HubPreferences){store.edit{it[topics]=value.topics;it[medium]=value.mediumDigest;it[automatic]=value.autoDigest;it[date]=value.lastAutoDate}}
    suspend fun clear(){store.edit{it.clear()}}
    private companion object{val topics=stringSetPreferencesKey("topics");val medium=booleanPreferencesKey("medium");val automatic=booleanPreferencesKey("auto");val date=stringPreferencesKey("date")}
}
