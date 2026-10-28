package br.com.jarvis.mobile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.jarvis.mobile.JarvisState
import br.com.jarvis.mobile.voice.*
import android.content.Intent
import android.content.ComponentName
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import br.com.jarvis.mobile.experience.*
import kotlin.math.roundToInt

@Composable
internal fun ModeSelector(settings: ExperienceSettings, onChange: (ExperienceSettings) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OperationMode.entries.forEach { mode ->
            val selected = settings.mode == mode
            OutlinedButton(onClick = { onChange(settings.copy(mode = mode)) },
                modifier = Modifier.weight(1f).heightIn(min = 46.dp).testTag("mode-${mode.name}")
                    .semantics { contentDescription = mode.title },
                contentPadding = PaddingValues(horizontal = 3.dp, vertical = 10.dp),
                shape = CutCornerShape(topStart = 7.dp, bottomEnd = 7.dp),
                border = BorderStroke(if (selected) 1.dp else 0.6.dp, if (selected) Cyan else Outline),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected) Cyan.copy(alpha = 0.09f) else Panel.copy(alpha = 0.50f))) {
                Text(mode.shortTitle, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                    color = if (selected) Cyan else Muted, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
internal fun ExperienceControls(state: JarvisState, onChange: (ExperienceSettings) -> Unit, onLoadVoices: () -> Unit) {
    val s = state.experience
    val silent = s.mode == OperationMode.SILENT
    Text("Experiência do JARVIS", color = Gold, fontSize = 16.sp)
    ModeSelector(s, onChange, Modifier.padding(top = 12.dp))
    Text(s.mode.detail, color = Muted, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 10.dp))
    if (silent) Text("O JARVIS não reproduz voz, música ou efeitos. Cada toque autoriza uma única escuta; a resposta aparece em texto.",
        color = Cyan, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp))
    AudioSwitch("Respostas faladas", s.voiceEnabled && !silent, !silent, "voice-toggle") { onChange(s.copy(voiceEnabled = it)) }
    AudioSlider("Volume da voz", s.voiceVolume, 0f..1f, !silent && s.voiceEnabled, "voice-volume") { onChange(s.copy(voiceVolume = it)) }
    AudioSwitch("Música ambiente", s.musicEnabled, !silent, "music-toggle") { onChange(s.withMusic(it)) }
    AudioSlider("Volume da música", s.musicVolume, 0f..1f, !silent && s.musicEnabled, "music-volume") { onChange(s.copy(musicVolume = it)) }
    AudioSwitch("Efeitos sonoros", s.effectsEnabled && !silent, !silent, "effects-toggle") { onChange(s.copy(effectsEnabled = it)) }
    AudioSlider("Volume dos efeitos", s.effectsVolume, 0f..1f, !silent && s.effectsEnabled, "effects-volume") { onChange(s.copy(effectsVolume = it)) }
    Text("A música diminui durante a fala e para durante a escuta. Todo áudio do app para ao sair ou perder o foco do Android.",
        color = Muted, fontSize = 12.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 8.dp))
    HorizontalDivider(Modifier.padding(vertical = 22.dp), color = Outline)
    Text("Personalidade vocal", color = Gold, fontSize = 16.sp)
    ChoiceMenu("Identidade vocal",s.voiceProfile.title,VoiceProfile.entries.map{it.name to it.title}){selected->
        onChange(s.copy(voiceProfile=VoiceProfile.valueOf(selected),speechRate=1f,pitch=1f,enginePackage="",voiceName=""))
    }
    val context=LocalContext.current
    if(s.voiceProfile==VoiceProfile.OFFICIAL_A){
        val installed=OfficialVoiceA.installation(context)==VoiceAInstallation.READY
        Text("Executivo clássico · Faber pt-BR. Ritmo calmo e tom natural da voz aprovada. A fala é gerada localmente, inclusive para textos novos.",
            color=Muted,fontSize=13.sp,lineHeight=21.sp,modifier=Modifier.padding(top=8.dp))
        Text(if(installed)"JARVIS Voz A instalado e verificado." else OfficialVoiceA.unavailableMessage(context),color=if(installed)Cyan else Gold,fontSize=12.sp,modifier=Modifier.padding(top=8.dp))
        AudioSlider("Velocidade relativa à voz A",s.speechRate,0.70f..1.15f,!silent,"speech-rate",percent=false){onChange(s.copy(speechRate=it))}
        OutlinedButton(onClick={
            val intent=if(installed)Intent().setComponent(ComponentName(OfficialVoiceA.PACKAGE,"br.com.jarvis.voicea.EngineActivity"))
                else Intent(Intent.ACTION_VIEW,Uri.parse(OfficialVoiceA.GUIDE))
            try{context.startActivity(intent)}catch(_:RuntimeException){/* Missing activity leaves the visible installation guidance. */}
        },modifier=Modifier.fillMaxWidth().padding(top=8.dp).testTag("voice-a-setup")){Text(if(installed)"Abrir motor da voz A" else "Como instalar JARVIS Voz A")}
        Text("O motor usa o mesmo certificado do JARVIS. Instale os dois APKs da mesma entrega. Nenhuma troca automática para uma voz diferente.",color=Muted,fontSize=12.sp,lineHeight=19.sp,modifier=Modifier.padding(top=8.dp))
    }else{
        Text("Alternativa do aparelho. O timbre e a disponibilidade offline dependem do mecanismo e dos dados de voz instalados.",color=Muted,fontSize=13.sp,lineHeight=21.sp,modifier=Modifier.padding(top=8.dp))
        AudioSlider("Velocidade",s.speechRate,0.70f..1.15f,!silent,"speech-rate",percent=false){onChange(s.copy(speechRate=it))}
        AudioSlider("Tom",s.pitch,0.70f..1.10f,!silent,"speech-pitch",percent=false){onChange(s.copy(pitch=it))}
        ChoiceMenu("Mecanismo de voz",state.engines.firstOrNull{it.packageName==s.enginePackage}?.label?:"Automático (Android)",
            listOf("" to "Automático (Android)")+state.engines.map{it.packageName to it.label}){onChange(s.copy(enginePackage=it,voiceName=""))}
        OutlinedButton(onClick=onLoadVoices,modifier=Modifier.fillMaxWidth().padding(top=8.dp)){Text("Listar vozes instaladas")}
        ChoiceMenu("Voz portuguesa",state.voices.firstOrNull{it.name==s.voiceName}?.label?:"Automática · prioridade pt-BR",
            listOf("" to "Automática · prioridade pt-BR")+state.voices.map{it.name to it.label}){onChange(s.copy(voiceName=it))}
        Text("Dados pessoais continuam limitados a uma voz declarada local e instalada. Sem ela, a resposta aparece em texto.",color=Muted,fontSize=12.sp,lineHeight=19.sp,modifier=Modifier.padding(top=8.dp))
    }
}

@Composable
private fun AudioSwitch(label: String, checked: Boolean, enabled: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = if (enabled) Cyan else Muted, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Switch(checked, onChange, enabled = enabled, modifier = Modifier.testTag(tag).semantics { contentDescription = label })
    }
}

@Composable
private fun AudioSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, enabled: Boolean, tag: String,
    percent: Boolean = true, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(if (percent) "${(value * 100).roundToInt()}%" else String.format(java.util.Locale.ROOT, "%.2f×", value),
            color = Muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
    Slider(value, onChange, enabled = enabled, valueRange = range,
        modifier = Modifier.fillMaxWidth().testTag(tag).semantics { contentDescription = label })
}

@Composable
private fun ChoiceMenu(label: String, value: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(label, color = Muted, fontSize = 13.sp)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(value, fontSize = 12.sp, lineHeight = 18.sp)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 280.dp)) {
                options.forEach { (key, title) -> DropdownMenuItem(text = { Text(title, fontSize = 13.sp) },
                    onClick = { open = false; onSelect(key) }) }
            }
        }
    }
}
