package br.com.jarvis.mobile

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.jarvis.mobile.experience.*
import br.com.jarvis.mobile.hub.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HubCommandIntegrationTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @After fun reset()=runBlocking{ExperienceStore(InstrumentationRegistry.getInstrumentation().targetContext).update{ExperienceSettings()}}
    @Test fun personalCommandUsesSharedRouterAndSilentPolicyWithoutPretendingGmailIsConnected()=runBlocking {
        val application=compose.activity.application as JarvisApplication
        application.data.storage.eraseAll()
        lateinit var core:JarvisViewModel;lateinit var hub:HubViewModel
        compose.runOnIdle{core=ViewModelProvider(compose.activity)[JarvisViewModel::class.java];hub=ViewModelProvider(compose.activity)[HubViewModel::class.java]}
        compose.waitUntil(8000){core.state.value.preferencesReady}
        compose.runOnUiThread{core.updateExperience{it.copy(mode=OperationMode.SILENT)};core.sendMessage("Jarvis, meus e-mails")}
        compose.waitUntil(8000){hub.state.value.panel==HubPanel.EMAIL&&!hub.state.value.busy&&!core.state.value.working}
        compose.runOnIdle{
            assertTrue(hub.state.value.mail.account.isEmpty());assertTrue(hub.state.value.mail.items.isEmpty())
            assertEquals(SourceStatus.NOT_CONFIGURED,hub.state.value.mail.info.status)
            assertTrue(core.state.value.reply.contains("não conectado"))
            assertFalse(core.state.value.speaking);assertFalse(core.state.value.voicePlaying)
            assertFalse(core.state.value.enabled);assertFalse(core.state.value.listening)
            assertEquals(OperationMode.SILENT,core.state.value.experience.mode)
        }
    }
}
