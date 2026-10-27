package br.com.jarvis.mobile

import android.content.ComponentName
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.jarvis.mobile.actions.AndroidActions
import br.com.jarvis.mobile.actions.InstalledApps
import br.com.jarvis.mobile.domain.LaunchableApp
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstalledAppsTest {
    @Test fun enumeratesOnlyExportedLauncherActivitiesInCurrentProfile() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val apps = InstalledApps(context).list()
        assertTrue("Google API emulator has launchable apps", apps.isNotEmpty())
        assertFalse(apps.any { it.packageName == context.packageName })
        for (app in apps) {
            val intent = AndroidActions().launchIntent(app)
            val info = requireNotNull(context.packageManager.resolveActivity(intent, 0))
            assertTrue(info.activityInfo.exported)
            assertEquals(app.packageName, info.activityInfo.packageName)
        }
    }
    @Test fun launchIntentTargetsExactlyTheSelectedComponent() {
        val app = LaunchableApp("Exemplo", "br.exemplo", "br.exemplo/.Launcher")
        val intent = AndroidActions().launchIntent(app)
        assertEquals(Intent.ACTION_MAIN, intent.action)
        assertTrue(intent.hasCategory(Intent.CATEGORY_LAUNCHER))
        assertEquals(ComponentName("br.exemplo", "br.exemplo.Launcher"), intent.component)
        assertNull(intent.data)
    }
}
