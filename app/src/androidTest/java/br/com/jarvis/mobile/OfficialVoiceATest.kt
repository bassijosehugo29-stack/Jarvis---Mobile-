package br.com.jarvis.mobile

import android.Manifest
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.jarvis.mobile.experience.ExperienceSettings
import br.com.jarvis.mobile.voice.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class OfficialVoiceATest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun installedEngineMatchesApprovedIdentityAndCannotAccessNetworkOrMicrophone(){
        assertEquals(VoiceAInstallation.READY,OfficialVoiceA.installation(compose.activity))
        @Suppress("DEPRECATION") val info=compose.activity.packageManager.getPackageInfo(OfficialVoiceA.PACKAGE,android.content.pm.PackageManager.GET_PERMISSIONS)
        assertFalse(info.requestedPermissions.orEmpty().contains(Manifest.permission.INTERNET))
        assertFalse(info.requestedPermissions.orEmpty().contains(Manifest.permission.RECORD_AUDIO))
    }
    @Test fun realApprovedModelSynthesizesAllFourPhrasesAndNewTextWithoutRecordedFallback(){
        val inst=InstrumentationRegistry.getInstrumentation()
        val initialized=CountDownLatch(1);val initCode=AtomicReference<Int>()
        lateinit var tts:TextToSpeech
        inst.runOnMainSync{tts=TextToSpeech(compose.activity,{status->initCode.set(status);initialized.countDown()},OfficialVoiceA.PACKAGE)}
        try{
            assertTrue(initialized.await(20,TimeUnit.SECONDS));assertEquals(TextToSpeech.SUCCESS,initCode.get().toInt())
            inst.runOnMainSync{
                val voice=tts.voices.first{it.name==OfficialVoiceA.VOICE}
                assertFalse(voice.isNetworkConnectionRequired);assertEquals("pt-BR",voice.locale.toLanguageTag())
                assertEquals(TextToSpeech.SUCCESS,tts.setVoice(voice));tts.setSpeechRate(1f);tts.setPitch(1f)
            }
            val texts=listOf("Bom dia, senhor. Todos os sistemas estão operacionais.","Às suas ordens, senhor.",
                "Entendido. Iniciando os procedimentos.","Senhor, identifiquei uma atualização importante. Deseja que eu apresente os detalhes?",
                "Reunião de revisão às nove e quarenta, com duração de vinte minutos.")
            val sizes=mutableSetOf<Int>()
            texts.forEachIndexed{i,text->
                val done=CountDownLatch(1);val success=AtomicReference(false)
                val file=File(compose.activity.cacheDir,"voice-a-$i.wav")
                inst.runOnMainSync{
                    tts.setOnUtteranceProgressListener(object:UtteranceProgressListener(){
                        override fun onStart(id:String?){}
                        override fun onDone(id:String?){success.set(true);done.countDown()}
                        @Deprecated("callback") override fun onError(id:String?){done.countDown()}
                    })
                    assertEquals(TextToSpeech.SUCCESS,tts.synthesizeToFile(text,null,file,"voice-a-test-$i"))
                }
                assertTrue("Local synthesis timed out",done.await(45,TimeUnit.SECONDS));assertTrue("Local synthesis failed",success.get())
                val data=file.readBytes();assertEquals("RIFF",String(data,0,4));assertTrue(data.size>22050)
                var offset=12;var samples:ByteArray?=null
                while(offset+8<=data.size){val name=String(data,offset,4);val length=ByteBuffer.wrap(data,offset+4,4).order(ByteOrder.LITTLE_ENDIAN).int
                    if(name=="fmt "){assertEquals(22050,ByteBuffer.wrap(data,offset+12,4).order(ByteOrder.LITTLE_ENDIAN).int)}
                    if(name=="data"){samples=data.copyOfRange(offset+8,minOf(data.size,offset+8+length));break}
                    offset+=8+length+(length%2)
                }
                assertNotNull(samples);assertTrue(samples!!.any{it.toInt()!=0});sizes+=samples.size
                if(i<4)export(file,"voz-a-android-frase-${i+1}.wav")
                file.delete()
            }
            assertTrue("Different new text must produce different nonempty PCM",sizes.size>=4)
        }finally{inst.runOnMainSync{tts.stop();tts.shutdown()}}
    }
    @Test fun mainSpeechOutputSelectsApprovedEngineAndReceivesRealPlaybackPcm(){
        val inst=InstrumentationRegistry.getInstrumentation();val done=CountDownLatch(1)
        val result=AtomicReference<SpeechResult>();val status=AtomicReference<VoiceStatus>();lateinit var output:SpeechOutput
        inst.runOnMainSync{
            output=SpeechOutput(compose.activity,{if(it.ready)status.set(it)})
            output.configure(ExperienceSettings())
            output.speak("Às suas ordens, senhor.",true){result.set(it);done.countDown()}
        }
        try{assertTrue(done.await(40,TimeUnit.SECONDS));assertTrue(result.get().toString(),result.get() is SpeechResult.Completed)
            assertTrue((result.get() as SpeechResult.Completed).audioBytes>22050)
            assertEquals(OfficialVoiceA.PACKAGE,status.get().engine);assertEquals("pt-BR",status.get().locale)
        }finally{inst.runOnMainSync{output.close()}}
    }
    private fun export(file:File,name:String){
        if(Build.VERSION.SDK_INT<29)return
        val resolver=compose.activity.contentResolver
        val uri=resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,ContentValues().apply{
            put(MediaStore.MediaColumns.DISPLAY_NAME,name);put(MediaStore.MediaColumns.MIME_TYPE,"audio/wav")
            put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/JARVIS-testes");put(MediaStore.MediaColumns.IS_PENDING,1)
        })!!
        resolver.openOutputStream(uri)!!.use{out->file.inputStream().use{it.copyTo(out)}}
        resolver.update(uri,ContentValues().apply{put(MediaStore.MediaColumns.IS_PENDING,0)},null,null)
    }
}
