package br.com.jarvis.mobile

import android.provider.AlarmClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.jarvis.mobile.actions.AndroidActions
import br.com.jarvis.mobile.actions.AppAction
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmIntentTest {
    @Test fun explicitAlarmRequestsClockAutomationWithCorrectTime() {
        val intent = AndroidActions().alarmIntent(AppAction.SetAlarm(7, 30))
        assertEquals(AlarmClock.ACTION_SET_ALARM, intent.action)
        assertEquals(7, intent.getIntExtra(AlarmClock.EXTRA_HOUR, -1))
        assertEquals(30, intent.getIntExtra(AlarmClock.EXTRA_MINUTES, -1))
        assertTrue(intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false))
    }
    @Test fun unspecifiedTimeIsLeftToClock() {
        val intent = AndroidActions().alarmIntent(AppAction.SetAlarm())
        assertFalse(intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, true))
        assertFalse(intent.hasExtra(AlarmClock.EXTRA_HOUR))
        assertFalse(intent.hasExtra(AlarmClock.EXTRA_MINUTES))
    }
}
