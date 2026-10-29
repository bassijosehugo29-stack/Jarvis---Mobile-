package br.com.jarvis.mobile.diagnostics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.com.jarvis.mobile.JarvisState
import br.com.jarvis.mobile.experience.*
import br.com.jarvis.mobile.ui.*
import kotlinx.coroutines.delay

/** Synthetic visual states, labelled. No microphone, accounts, personal data or device actions. */
class CoreShowcaseActivity : ComponentActivity() {
    var phase by mutableStateOf(CorePhase.IDLE)
    var mode by mutableStateOf(OperationMode.CINEMATIC)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        phase = CorePhase.entries.firstOrNull { it.name == intent.getStringExtra("phase") } ?: CorePhase.IDLE
        mode = OperationMode.entries.firstOrNull { it.name == intent.getStringExtra("mode") } ?: OperationMode.CINEMATIC
        setContent {
            JarvisTheme {
                var level by remember { mutableFloatStateOf(0f) }
                LaunchedEffect(phase) {
                    if (phase == CorePhase.LISTENING || phase == CorePhase.RESPONDING) {
                        var t=0f
                        while (true) { delay(50); t+=0.18f; level=(0.5f+0.4f*kotlin.math.sin(t)).coerceIn(0f,1f) }
                    } else level=0f
                }
                if (intent.getBooleanExtra("sequence", false)) LaunchedEffect(Unit) {
                    for (p in CorePhase.entries) { phase=p; delay(2600) }
                    phase=CorePhase.IDLE
                }
                Column(Modifier.fillMaxSize()) {
                    Text("DEMONSTRAÇÃO · SINAL SIMULADO", color=Color(0xFF74DFFF), modifier=Modifier.padding(8.dp))
                    JarvisScreen(JarvisState(initializing=phase==CorePhase.INITIALIZING,
                        listening=phase==CorePhase.LISTENING, voicePlaying=phase==CorePhase.RESPONDING,
                        speaking=phase==CorePhase.RESPONDING, thinking=phase==CorePhase.PROCESSING,
                        visualError=phase==CorePhase.ERROR, audioLevel=if(phase==CorePhase.LISTENING)level else 0f,
                        speechLevel=if(phase==CorePhase.RESPONDING)level else null,
                        reply="Prévia visual · ${phase.label.lowercase()}", experience=ExperienceSettings(mode=mode)), {}, {})
                }
            }
        }
    }
}
