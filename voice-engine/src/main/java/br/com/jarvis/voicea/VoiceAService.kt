// SPDX-License-Identifier: GPL-3.0-or-later
package br.com.jarvis.voicea

import android.media.AudioFormat
import android.speech.tts.*
import com.k2fsa.sherpa.onnx.*
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/** Standard Android TTS service, in its own sandbox with no network/microphone permissions.
 * Synthesis input exists in RAM only. No raw-text logs, recording, database, worker or boot receiver.
 */
class VoiceAService:TextToSpeechService(){
    private val serial=AtomicLong()
    private val lock=Any()
    private var model:OfflineTts?=null
    override fun onIsLanguageAvailable(language:String,country:String,variant:String):Int=
        if(language in setOf("pt","por"))if(country in setOf("BR","BRA",""))TextToSpeech.LANG_COUNTRY_AVAILABLE else TextToSpeech.LANG_AVAILABLE else TextToSpeech.LANG_NOT_SUPPORTED
    override fun onGetLanguage()=arrayOf("por","BRA","")
    override fun onLoadLanguage(language:String,country:String,variant:String)=onIsLanguageAvailable(language,country,variant)
    override fun onGetVoices()=listOf(Voice(VOICE,Locale.forLanguageTag("pt-BR"),Voice.QUALITY_HIGH,Voice.LATENCY_NORMAL,false,emptySet()))
    override fun onIsValidVoiceName(voiceName:String)=if(voiceName==VOICE)TextToSpeech.SUCCESS else TextToSpeech.ERROR
    override fun onLoadVoice(voiceName:String)=onIsValidVoiceName(voiceName)
    override fun onGetDefaultVoiceNameFor(language:String,country:String,variant:String)=VOICE
    override fun onStop(){serial.incrementAndGet()}
    override fun onSynthesizeText(request:SynthesisRequest?,callback:SynthesisCallback?){
        if(request==null||callback==null)return
        val current=serial.incrementAndGet()
        fun active()=serial.get()==current
        val text=request.charSequenceText?.toString().orEmpty()
        if(text.isBlank()){callback.start(22050,AudioFormat.ENCODING_PCM_16BIT,1);callback.done();return}
        if(text.length>MAX_TEXT||onIsLanguageAvailable(request.language,request.country,request.variant)<0){callback.error(TextToSpeech.ERROR_INVALID_REQUEST);return}
        try{
            val parts=mutableListOf<FloatArray>()
            synchronized(lock){
                if(!active())return
                val engine=model?:loadModel(::active).also{model=it}
                val rate=(request.speechRate/100f).coerceIn(0.70f,1.15f)
                engine.generateWithCallback(text=text,sid=0,speed=rate){samples->
                    if(!active())0 else {if(samples.isNotEmpty()){parts+=samples.copyOf();parts+=FloatArray((22050*0.24).toInt())};1}
                }
            }
            if(!active())return
            val count=parts.sumOf{it.size};if(count==0){callback.error(TextToSpeech.ERROR_SYNTHESIS);return}
            require(count<=22050*180)
            val combined=FloatArray(count);var at=0
            parts.forEach{it.copyInto(combined,at);at+=it.size}
            val processed=ExecutiveDsp.process(combined)
            if(!active())return
            if(callback.start(22050,AudioFormat.ENCODING_PCM_16BIT,1)!=TextToSpeech.SUCCESS)return
            val buffer=ByteArray(minOf(callback.maxBufferSize,16384).coerceAtLeast(2).let{it-it%2})
            var index=0
            while(index<processed.size&&active()){
                val n=minOf(buffer.size/2,processed.size-index)
                for(i in 0 until n){val sample=(processed[index+i].coerceIn(-1f,1f)*32767).toInt();buffer[2*i]=sample.toByte();buffer[2*i+1]=(sample shr 8).toByte()}
                if(callback.audioAvailable(buffer,0,n*2)!=TextToSpeech.SUCCESS)return
                index+=n
            }
            if(active())callback.done()
        }catch(_:Exception){if(active())callback.error(TextToSpeech.ERROR_SYNTHESIS)}
        catch(_:LinkageError){if(active())callback.error(TextToSpeech.ERROR_SERVICE)}
    }
    private fun loadModel(active:()->Boolean):OfflineTts{
        val data=File(noBackupFilesDir,"faber-phonemes-v1")
        val ready=File(data,".ready")
        if(!ready.exists()){
            fun copy(asset:String,target:File){
                check(active());val children=assets.list(asset).orEmpty()
                if(children.isNotEmpty()){target.mkdirs();children.forEach{copy("$asset/$it",File(target,it))}}
                else{target.parentFile?.mkdirs();assets.open(asset).use{input->target.outputStream().use{input.copyTo(it)}}}
            }
            copy("voice-a/espeak-ng-data",data);ready.writeText("executive-a-v1")
        }
        check(active())
        val config=OfflineTtsConfig(model=OfflineTtsModelConfig(
            vits=OfflineTtsVitsModelConfig(model="voice-a/model.onnx",tokens="voice-a/tokens.txt",dataDir=data.absolutePath,
                noiseScale=0.55f,noiseScaleW=0.70f,lengthScale=1.08f),numThreads=2,debug=false,provider="cpu"),maxNumSentences=1,silenceScale=0.0f)
        return OfflineTts(assets,config)
    }
    override fun onDestroy(){
        serial.incrementAndGet()
        Thread({synchronized(lock){model?.release();model=null}},"jarvis-voice-a-release").apply{isDaemon=true;start()}
        super.onDestroy()
    }
    companion object{const val VOICE="jarvis-faber-pt-BR";const val MAX_TEXT=1200}
}
