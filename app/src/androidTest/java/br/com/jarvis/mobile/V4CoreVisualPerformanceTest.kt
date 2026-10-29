package br.com.jarvis.mobile

import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.jarvis.mobile.diagnostics.CoreShowcaseActivity
import br.com.jarvis.mobile.experience.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Screenshots use the controlled Compose clock; performance is collected by measure_v4.py
 * outside instrumentation, on actual compositor frames. Virtual-clock timings are not benchmarks.
 */
@RunWith(AndroidJUnit4::class)
class V4CoreVisualPerformanceTest {
    @get:Rule val compose=createAndroidComposeRule<CoreShowcaseActivity>()
    @Test fun captureAllStatesWithControlledClockAndKeepCoreAccessible() {
        val inst=InstrumentationRegistry.getInstrumentation()
        compose.mainClock.autoAdvance=false
        for(phase in CorePhase.entries){
            compose.runOnUiThread{compose.activity.phase=phase;compose.activity.mode=OperationMode.CINEMATIC}
            compose.mainClock.advanceTimeBy(1500)
            compose.onNodeWithTag("jarvis-core", useUnmergedTree=true).assertIsDisplayed()
            compose.onNodeWithText(phase.label).assertExists()
            SystemClock.sleep(180)
            saveUiScreenshot("jarvis-v4-${phase.name.lowercase()}.png",requireNotNull(inst.uiAutomation.takeScreenshot()))
        }
        compose.mainClock.autoAdvance=true
    }
}
