package br.com.jarvis.mobile.hub

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.*

class AgendaRepository(private val context:Context) {
    fun hasPermission()=ContextCompat.checkSelfPermission(context,Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED
    suspend fun load(now:Instant=Instant.now(),zone:ZoneId=ZoneId.systemDefault()):AgendaSnapshot=withContext(Dispatchers.IO){
        if(!hasPermission())return@withContext AgendaSnapshot()
        try{
            val day=now.atZone(zone).toLocalDate();val begin=day.atStartOfDay(zone).toInstant().toEpochMilli();val end=day.plusDays(8).atStartOfDay(zone).toInstant().toEpochMilli()
            val calendars=mutableMapOf<Long,Pair<String,Int>>()
            context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI,
                arrayOf("_id","calendar_displayName","calendar_color"),"visible = ? AND calendar_access_level >= ?",arrayOf("1","200"),null)?.use{
                while(it.moveToNext())calendars[it.getLong(0)]=it.getString(1).orEmpty().ifBlank{"Agenda"} to it.getInt(2)
            }
            val result=mutableListOf<AgendaEntry>()
            // One-day padding also covers UTC dates of all-day events in local negative offsets.
            CalendarContract.Instances.query(context.contentResolver,
                arrayOf("event_id","calendar_id","title","begin","end","allDay","eventStatus"),begin-86400000L,end+86400000L).use{cursor->
                if(cursor!=null)while(cursor.moveToNext()){
                    val calendar=calendars[cursor.getLong(1)]?:continue
                    if(cursor.getInt(6)==CalendarContract.Events.STATUS_CANCELED)continue
                    val item=AgendaEntry(cursor.getLong(0),cursor.getLong(1),cursor.getString(2).orEmpty().ifBlank{"Sem título"},cursor.getLong(3),cursor.getLong(4),cursor.getInt(5)!=0,calendar.first,calendar.second)
                    val overlaps=if(item.allDay){val date=Instant.ofEpochMilli(item.start).atZone(ZoneOffset.UTC).toLocalDate();val finish=Instant.ofEpochMilli(item.end).atZone(ZoneOffset.UTC).toLocalDate();date<day.plusDays(8)&&finish>day}
                        else item.start<end&&item.end>begin
                    if(overlaps)result+=item
                }
            }
            AgendaSnapshot(result.sortedWith(compareBy({it.start},{it.title})),SourceInfo(SourceStatus.READY,"Calendários visíveis deste aparelho",now.toEpochMilli()))
        }catch(_:SecurityException){AgendaSnapshot()}catch(_:RuntimeException){AgendaSnapshot(info=SourceInfo(SourceStatus.ERROR,"Não foi possível consultar o calendário do Android."))}
    }
}
