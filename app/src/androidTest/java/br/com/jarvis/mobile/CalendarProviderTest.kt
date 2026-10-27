package br.com.jarvis.mobile

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars as Cal
import android.provider.CalendarContract.Events as Ev
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import br.com.jarvis.mobile.actions.CalendarSaveResult
import br.com.jarvis.mobile.actions.CalendarStore
import br.com.jarvis.mobile.domain.CalendarEvent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 28)
class CalendarProviderTest {
    @Test fun savesAndVerifiesEventInAnIsolatedLocalCalendar() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        for (permission in listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, permission)
        }
        val resolver = context.contentResolver
        val account = "jarvis-instrumented-test"
        val syncUri = Cal.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(Cal.ACCOUNT_NAME, account)
            .appendQueryParameter(Cal.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL).build()
        val calendarUri = requireNotNull(resolver.insert(syncUri, ContentValues().apply {
            put(Cal.ACCOUNT_NAME, account)
            put(Cal.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(Cal.NAME, "jarvis-ci-only")
            put(Cal.CALENDAR_DISPLAY_NAME, "JARVIS teste local")
            put(Cal.CALENDAR_COLOR, 0xFF53E8FF.toInt())
            put(Cal.CALENDAR_ACCESS_LEVEL, Cal.CAL_ACCESS_OWNER)
            put(Cal.OWNER_ACCOUNT, account)
            put(Cal.CALENDAR_TIME_ZONE, "America/Sao_Paulo")
            put(Cal.VISIBLE, 1)
            put(Cal.SYNC_EVENTS, 1)
        }))
        val id = ContentUris.parseId(calendarUri)
        val store = CalendarStore(context)
        try {
            assertTrue(store.hasPermission())
            assertTrue(store.calendars().any { it.id == id })
            val start = System.currentTimeMillis() + 86_400_000L
            val event = CalendarEvent("Dentista com João — teste", start, start + 1_800_000L, "America/Sao_Paulo")
            val saved = store.save(event, id)
            assertTrue("Provider must confirm the inserted event", saved is CalendarSaveResult.Saved)
            val eventUri = ContentUris.withAppendedId(Ev.CONTENT_URI, (saved as CalendarSaveResult.Saved).eventId)
            resolver.query(eventUri, arrayOf(Ev.TITLE, Ev.DTSTART, Ev.DTEND, Ev.EVENT_TIMEZONE, Ev.CALENDAR_ID, Ev.HAS_ALARM), null, null, null)!!.use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(event.title, cursor.getString(0))
                assertEquals(start, cursor.getLong(1))
                assertEquals(start + 1_800_000L, cursor.getLong(2))
                assertEquals("America/Sao_Paulo", cursor.getString(3))
                assertEquals(id, cursor.getLong(4))
                assertEquals(0, cursor.getInt(5))
            }
            assertEquals(CalendarSaveResult.Unconfirmed, store.save(event, -9999))
        } finally {
            resolver.delete(Ev.CONTENT_URI, "${Ev.CALENDAR_ID} = ?", arrayOf(id.toString()))
            resolver.delete(syncUri, "${Cal._ID} = ?", arrayOf(id.toString()))
        }
    }
}
