package br.com.jarvis.mobile.actions

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract.Calendars as Cal
import android.provider.CalendarContract.Events as Ev
import androidx.core.content.ContextCompat
import br.com.jarvis.mobile.domain.CalendarEvent

data class WritableCalendar(val id: Long, val label: String)
sealed interface CalendarSaveResult {
    data class Saved(val eventId: Long) : CalendarSaveResult
    data object Unconfirmed : CalendarSaveResult
}

/** The provider owns account sync. Only the calendar list and the newly inserted event are read. */
class CalendarStore(private val context: Context) {
    private val preferences = context.getSharedPreferences("jarvis-calendar", Context.MODE_PRIVATE)

    fun hasPermission(): Boolean = listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR).all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun calendars(): List<WritableCalendar> {
        check(hasPermission())
        val result = mutableListOf<WritableCalendar>()
        context.contentResolver.query(Cal.CONTENT_URI,
            arrayOf(Cal._ID, Cal.CALENDAR_DISPLAY_NAME, Cal.ACCOUNT_NAME),
            "${Cal.CALENDAR_ACCESS_LEVEL} >= ? AND ${Cal.VISIBLE} = ? AND ${Cal.SYNC_EVENTS} = ?",
            arrayOf(Cal.CAL_ACCESS_CONTRIBUTOR.toString(), "1", "1"), null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(1).orEmpty().ifBlank { "Agenda" }
                val account = cursor.getString(2).orEmpty()
                result += WritableCalendar(cursor.getLong(0), if (account.isBlank() || account == name) name else "$name ($account)")
            }
        }
        return result.sortedWith(compareBy({ it.label }, { it.id }))
    }

    fun preferredId(): Long? = preferences.getLong("selected-calendar", -1).takeIf { it >= 0 }
    fun remember(calendarId: Long) { preferences.edit().putLong("selected-calendar", calendarId).apply() }

    fun save(event: CalendarEvent, calendarId: Long): CalendarSaveResult {
        check(hasPermission())
        if (calendars().none { it.id == calendarId }) return CalendarSaveResult.Unconfirmed
        val values = ContentValues().apply {
            put(Ev.CALENDAR_ID, calendarId)
            put(Ev.TITLE, event.title)
            put(Ev.DTSTART, event.startMillis)
            put(Ev.DTEND, event.endMillis)
            put(Ev.EVENT_TIMEZONE, event.timeZone)
            put(Ev.HAS_ALARM, 0)
        }
        val uri = context.contentResolver.insert(Ev.CONTENT_URI, values) ?: return CalendarSaveResult.Unconfirmed
        // Only claim success after the provider returns the same title and time from that event.
        return context.contentResolver.query(uri, arrayOf(Ev._ID, Ev.TITLE, Ev.DTSTART, Ev.DTEND), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && cursor.getString(1) == event.title && cursor.getLong(2) == event.startMillis && cursor.getLong(3) == event.endMillis)
                CalendarSaveResult.Saved(cursor.getLong(0)) else CalendarSaveResult.Unconfirmed
        } ?: CalendarSaveResult.Unconfirmed
    }
}
