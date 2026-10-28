package br.com.jarvis.mobile

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.jarvis.mobile.experience.*
import br.com.jarvis.mobile.voice.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V4IntegrationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private class Input(val callbacks: SpeechCallbacks) : SpeechInput {
        override val available=true; override val onDevice=true
        var starts=0; var closed=false
        override fun start(){ if(!closed){starts++;callbacks.onReady()} }
        override fun stop()=Unit
        override fun close(){closed=true}
        fun emit(text:String){if(!closed)callbacks.onResult(text)}
    }
    @Test fun continuousSessionResumesAfterTwoRealTtsRepliesAndStopsOnBackground() {
        val inst=InstrumentationRegistry.getInstrumentation();val target=inst.targetContext
        android.os.ParcelFileDescriptor.AutoCloseInputStream(inst.uiAutomation.executeShellCommand(
            "pm grant ${target.packageName} android.permission.RECORD_AUDIO")).use { it.readBytes() }
        target.contentResolver.call(Uri.parse("content://br.com.jarvis.mobile.test.tts-fixture"),"local",null,null)
        lateinit var model:JarvisViewModel; lateinit var input:Input;val store=ViewModelStore()
        inst.runOnMainSync {
            model=JarvisViewModel(target.applicationContext as Application,SpeechInputFactory{_,callbacks,_->Input(callbacks).also{input=it}})
            store.put("v4-session",model);model.onForeground()
        }
        try {
            compose.waitUntil(8000){model.state.value.preferencesReady}
            inst.runOnMainSync {
                model.updateExperience{it.copy(mode=OperationMode.ASSISTANT,voiceProfile=VoiceProfile.ANDROID,
                    enginePackage=inst.context.packageName,voiceName="",voiceEnabled=true,voiceInterruption=false)}
                model.start()
            }
            compose.waitUntil(12000){model.state.value.listening}
            inst.runOnMainSync{input.emit("Jarvis")}
            compose.waitUntil(12000){model.state.value.listening && input.starts>=2}
            inst.runOnMainSync{input.emit("Jarvis")}
            compose.waitUntil(12000){model.state.value.listening && input.starts>=3}
            inst.runOnMainSync{model.onBackground();input.callbacks.onResult("Jarvis, abre o WhatsApp")}
            assertTrue(input.closed);assertFalse(model.state.value.enabled);assertFalse(model.state.value.listening)
            assertFalse(model.state.value.speaking);assertFalse(model.state.value.interruptListening)
        } finally {
            inst.runOnMainSync{model.onBackground();store.clear()}
            runBlocking{ExperienceStore(target).update{ExperienceSettings()}}
        }
    }
    @Test fun assistIntentIsOfficiallyResolvableAndDoesNotStartHiddenCapture() {
        val intent=Intent(Intent.ACTION_ASSIST).setPackage(compose.activity.packageName).addCategory(Intent.CATEGORY_DEFAULT)
        @Suppress("DEPRECATION") val resolved=compose.activity.packageManager.queryIntentActivities(intent,PackageManager.MATCH_DEFAULT_ONLY)
        assertTrue(resolved.any{it.activityInfo.name==MainActivity::class.java.name})
        compose.runOnUiThread{compose.activity.startActivity(intent)}
        compose.waitForIdle()
        val model=androidx.lifecycle.ViewModelProvider(compose.activity)[JarvisViewModel::class.java]
        assertFalse(model.state.value.enabled);assertFalse(model.state.value.listening)
        compose.onNodeWithContentDescription("Configurações").performClick()
        compose.onNodeWithTag("system-assistant").performScrollTo().assertExists()
    }
    @Test fun speakerRouteRefusesSimultaneousRecognition() {
        var active=false
        val detector=VoiceInterruption(compose.activity,{a,_->active=a},{fail("No headset on CI emulator")})
        compose.runOnUiThread{assertFalse(detector.privateRoute());detector.start("Texto público de teste")}
        assertFalse(active)
        compose.runOnUiThread{detector.stop()}
    }
}
