// SPDX-License-Identifier: GPL-3.0-or-later
package br.com.jarvis.voicea

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.ViewGroup
import android.widget.*
import java.util.Locale

class EngineActivity:Activity(){
    private var voice:TextToSpeech?=null
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        val layout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(28,48,28,28);setBackgroundColor(Color.rgb(3,12,20))}
        fun text(value:String,size:Float,color:Int)=TextView(this).apply{this.text=value;textSize=size;setTextColor(color);setPadding(0,12,0,20)}
        layout.addView(text("JARVIS · VOZ A",24f,Color.rgb(83,232,255)))
        layout.addView(text("Executivo clássico\nPortuguês brasileiro · Faber",19f,Color.rgb(214,186,122)))
        layout.addView(text("Voz neural instalada neste aparelho. Sem permissão de internet ou microfone. O JARVIS usa este motor automaticamente; não é preciso mudar a voz de todos os aplicativos.",16f,Color.LTGRAY))
        layout.addView(text("O tom natural foi preservado. A velocidade pode ser ajustada no JARVIS. Cada resposta é sintetizada no celular, inclusive textos novos.",15f,Color.LTGRAY))
        val status=text("Pronto para testar a voz.",15f,Color.LTGRAY);layout.addView(status)
        layout.addView(Button(this).apply{text="Ouvir a voz A";setOnClickListener{
            isEnabled=false;status.text="Preparando a voz local…"
            val button=this
            voice?.shutdown()
            voice=TextToSpeech(this@EngineActivity,{result->runOnUiThread{
                button.isEnabled=true
                if(result==TextToSpeech.SUCCESS){voice?.setLanguage(Locale.forLanguageTag("pt-BR"));voice?.setSpeechRate(1f);voice?.setPitch(1f)
                    voice?.speak("Às suas ordens, senhor.",TextToSpeech.QUEUE_FLUSH,null,"voice-a-preview");status.text="Confira o volume de mídia e a saída de áudio."}
                else status.text="Não foi possível iniciar. Feche e abra o motor novamente."
            }},packageName)
        }})
        layout.addView(Button(this).apply{text="Licenças e código-fonte";setOnClickListener{
            AlertDialog.Builder(this@EngineActivity).setTitle("Motor local de voz A")
                .setMessage("Este componente separado usa GPL-3.0-or-later, sherpa-onnx e eSpeak NG. O dataset Faber é declarado CC0.\n\nFontes e instruções: github.com/bassijosehugo29-stack/Jarvis---Mobile-/tree/main/voice-engine\n\nOs textos recebidos para síntese não são registrados ou enviados à rede.")
                .setPositiveButton("Fechar",null).show()
        }})
        setContentView(ScrollView(this).apply{addView(layout,ViewGroup.LayoutParams(-1,-2))})
    }
    override fun onStop(){voice?.stop();super.onStop()}
    override fun onDestroy(){voice?.shutdown();voice=null;super.onDestroy()}
}
class CheckVoiceDataActivity:Activity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
    setResult(TextToSpeech.Engine.CHECK_VOICE_DATA_PASS,Intent().putStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES,arrayListOf("por-BRA"))
        .putStringArrayListExtra(TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES,arrayListOf()));finish()}}
class SampleTextActivity:Activity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
    setResult(TextToSpeech.LANG_COUNTRY_AVAILABLE,Intent().putExtra(TextToSpeech.Engine.EXTRA_SAMPLE_TEXT,"Às suas ordens, senhor."));finish()}}
