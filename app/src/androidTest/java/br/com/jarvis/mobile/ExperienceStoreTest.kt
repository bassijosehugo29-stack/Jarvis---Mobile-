package br.com.jarvis.mobile

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.jarvis.mobile.experience.*
import br.com.jarvis.mobile.voice.VoiceProfile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ExperienceStoreTest {
    @Test fun upgradesToApprovedVoiceWithoutChangingModeAndPersistsExplicitAlternative()=runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val file=File(context.cacheDir,"voice-migration-${System.nanoTime()}.preferences_pb")
        val job=SupervisorJob();val scope=CoroutineScope(job+Dispatchers.IO)
        try{
            val disk=PreferenceDataStoreFactory.create(scope=scope,produceFile={file})
            disk.edit{it[stringPreferencesKey("mode")]=OperationMode.SILENT.name;it[floatPreferencesKey("pitch")]=0.78f;it[floatPreferencesKey("speech_rate")]=0.81f;it[stringPreferencesKey("engine") ]="legacy.voice"}
            val store=ExperienceStore(context,disk);val migrated=store.settings.first()
            assertEquals(OperationMode.SILENT,migrated.mode);assertEquals(VoiceProfile.OFFICIAL_A,migrated.voiceProfile)
            assertEquals(1f,migrated.pitch,0f);assertEquals(1f,migrated.speechRate,0f);assertEquals("",migrated.enginePackage)
            store.update{it.copy(voiceProfile=VoiceProfile.ANDROID,pitch=0.83f)}
            val selected=store.settings.first{it.voiceProfile==VoiceProfile.ANDROID}
            assertEquals(0.83f,selected.pitch,0f);assertFalse(selected.voiceAllowed)
        }finally{job.cancelAndJoin();file.delete()}
    }
    @Test fun modeVolumesAndVoiceSurviveClosingAndReopeningDataStore() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "experience-test-${System.nanoTime()}.preferences_pb")
        var job = SupervisorJob()
        var scope = CoroutineScope(job + Dispatchers.IO)
        try {
            val first = ExperienceStore(context, PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }))
            assertEquals(OperationMode.ASSISTANT, first.settings.first().mode)
            first.update { it.copy(voiceProfile=VoiceProfile.ANDROID,mode = OperationMode.CINEMATIC, voiceVolume = 0.72f, musicVolume = 0.08f,
                effectsEnabled = false, pitch = 0.81f, speechRate = 0.92f, enginePackage = "teste.motor", voiceName = "teste.voz") }
            first.update { it.copy(mode = OperationMode.SILENT) }
            job.cancelAndJoin()
            assertTrue(file.length() > 0)
            job = SupervisorJob(); scope = CoroutineScope(job + Dispatchers.IO)
            val restored = ExperienceStore(context, PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })).settings.first()
            assertEquals(OperationMode.SILENT, restored.mode)
            assertFalse(restored.voiceAllowed)
            assertEquals(0.72f, restored.voiceVolume, 0f); assertEquals(0.08f, restored.musicVolume, 0f)
            assertFalse(restored.effectsEnabled); assertEquals(0.81f, restored.pitch, 0f)
            assertEquals("teste.motor", restored.enginePackage); assertEquals("teste.voz", restored.voiceName)
        } finally { job.cancelAndJoin(); file.delete() }
    }
}
