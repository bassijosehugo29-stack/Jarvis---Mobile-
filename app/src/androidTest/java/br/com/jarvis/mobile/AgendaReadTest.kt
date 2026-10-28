package br.com.jarvis.mobile

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars as Cal
import android.provider.CalendarContract.Events as Ev
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.jarvis.mobile.hub.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*

@RunWith(AndroidJUnit4::class)
class AgendaReadTest {
    @Test fun expandsRecurringEventsAndReadsReadOnlyCalendarsWithoutMutatingProvider()=runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        for(permission in listOf(Manifest.permission.READ_CALENDAR,Manifest.permission.WRITE_CALENDAR))
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName,permission)
        val resolver=context.contentResolver
        val account="jarvis-read-ci"
        fun sync(uri:android.net.Uri)=uri.buildUpon().appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER,"true")
            .appendQueryParameter(Cal.ACCOUNT_NAME,account).appendQueryParameter(Cal.ACCOUNT_TYPE,CalendarContract.ACCOUNT_TYPE_LOCAL).build()
        val calendars=sync(Cal.CONTENT_URI);val events=sync(Ev.CONTENT_URI)
        val calendar=resolver.insert(calendars,ContentValues().apply{
            put(Cal.ACCOUNT_NAME,account);put(Cal.ACCOUNT_TYPE,CalendarContract.ACCOUNT_TYPE_LOCAL);put(Cal.NAME,"jarvis-read-fixture")
            put(Cal.CALENDAR_DISPLAY_NAME,"Leitura CI");put(Cal.CALENDAR_COLOR,0xFF53E8FF.toInt());put(Cal.CALENDAR_ACCESS_LEVEL,Cal.CAL_ACCESS_READ)
            put(Cal.OWNER_ACCOUNT,account);put(Cal.CALENDAR_TIME_ZONE,"America/Sao_Paulo");put(Cal.VISIBLE,1);put(Cal.SYNC_EVENTS,1)
        })!!
        val id=ContentUris.parseId(calendar)
        val zone=ZoneId.of("America/Sao_Paulo");val day=LocalDate.now(zone)
        val start=day.atTime(14,0).atZone(zone).toInstant().toEpochMilli()
        fun insert(title:String,allDay:Boolean=false,recurrence:Boolean=false,canceled:Boolean=false){
            resolver.insert(events,ContentValues().apply{
                put(Ev.CALENDAR_ID,id);put(Ev.TITLE,title);put(Ev.EVENT_TIMEZONE,if(allDay)"UTC" else zone.id);put(Ev.ALL_DAY,if(allDay)1 else 0)
                put(Ev.DTSTART,if(allDay)day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() else start)
                if(recurrence){put(Ev.RRULE,"FREQ=DAILY;COUNT=3");put(Ev.DURATION,"PT30M")}
                else put(Ev.DTEND,if(allDay)day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() else start+1800000)
                if(canceled)put(Ev.STATUS,Ev.STATUS_CANCELED)
            })!!
        }
        try{
            insert("Recorrente CI",recurrence=true);insert("Dia inteiro CI",allDay=true);insert("Cancelado CI",canceled=true)
            fun providerCount()=resolver.query(events,arrayOf(Ev._ID),"${Ev.CALENDAR_ID} = ?",arrayOf(id.toString()),null)!!.use{it.count}
            val before=providerCount()
            val result=AgendaRepository(context).load(day.atTime(9,0).atZone(zone).toInstant(),zone)
            assertEquals(SourceStatus.READY,result.info.status)
            val own=result.entries.filter{it.calendarId==id}
            assertEquals(3,own.count{it.title=="Recorrente CI"})
            assertEquals(1,own.count{it.title=="Dia inteiro CI"&&it.allDay})
            assertFalse(own.any{it.title=="Cancelado CI"})
            assertTrue(own.zipWithNext().all{it.first.start<=it.second.start})
            assertEquals(before,providerCount())
            assertEquals(2,todayEntries(own,day,zone).size)
        }finally{resolver.delete(events,"${Ev.CALENDAR_ID} = ?",arrayOf(id.toString()));resolver.delete(calendars,"${Cal._ID} = ?",arrayOf(id.toString()))}
    }
}
