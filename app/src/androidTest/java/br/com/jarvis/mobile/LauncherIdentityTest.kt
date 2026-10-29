package br.com.jarvis.mobile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherIdentityTest {
    @Test fun launcherHasTheExactNameAndAnAdaptiveCoreIcon() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val info = context.packageManager.getApplicationInfo(context.packageName, 0)
        assertEquals("JARVIS", context.packageManager.getApplicationLabel(info).toString())
        val icon = context.packageManager.getApplicationIcon(info)
        assertTrue(icon is AdaptiveIconDrawable)
        val adaptive = icon as AdaptiveIconDrawable
        assertNotNull(adaptive.background); assertNotNull(adaptive.foreground)
        assertNotNull(adaptive.monochrome)
        val bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
        adaptive.setBounds(0, 0, 192, 192); adaptive.draw(Canvas(bitmap))
        val center = bitmap.getPixel(96, 96)
        assertTrue("Luminous blue-white center", Color.blue(center) > 180)
        saveUiScreenshot("jarvis-launcher-icon.png", bitmap)
    }
}
