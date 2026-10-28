package br.com.jarvis.mobile

import android.view.WindowManager
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.jarvis.mobile.brain.BrainViewModel
import br.com.jarvis.mobile.hub.HubPanel
import br.com.jarvis.mobile.hub.HubViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrivacyLifecycleTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun backgroundLocksMemoryDiscardsDraftAndProtectedScreenDisablesCapture() {
        lateinit var brain:BrainViewModel
        compose.runOnIdle{
            brain=ViewModelProvider(compose.activity)[BrainViewModel::class.java]
            brain.unlocked();brain.suggest("RASCUNHO PRIVADO CI")
            ViewModelProvider(compose.activity)[HubViewModel::class.java].navigate(HubPanel.BRAIN)
        }
        compose.waitUntil(5000){brain.state.value.unlocked&&brain.state.value.draft!=null}
        compose.runOnIdle{assertTrue(compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE!=0)}
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        assertFalse(brain.state.value.unlocked);assertNull(brain.state.value.draft);assertTrue(brain.state.value.notes.isEmpty())
        val data=(compose.activity.application as JarvisApplication).data
        assertFalse(data.brain.isUnlocked())
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.runOnIdle{assertFalse(brain.state.value.unlocked)}
    }
}
