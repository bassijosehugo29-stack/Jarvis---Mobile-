package br.com.jarvis.mobile.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import br.com.jarvis.mobile.JarvisState

@Composable
fun JarvisScreen(
    state: JarvisState,
    onToggle: () -> Unit,
    onChatGpt: () -> Unit,
    onAlarm: () -> Unit,
    onSettings: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Background) {
        Column(
            modifier = Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0C1B2C), Background, Background)))
                .safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("J A R V I S", color = Gold, fontSize = 30.sp, fontWeight = FontWeight.Light)
            Text("M O B I L E   /   V 0.1", color = Cyan, fontSize = 11.sp)
            Spacer(Modifier.height(22.dp))
            Reactor(active = state.listening || state.speaking)
            Spacer(Modifier.height(12.dp))
            Text(state.status, color = Cyan, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text("SEU ASSISTENTE PESSOAL", color = Gold, fontSize = 10.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Panel),
                modifier = Modifier.fillMaxWidth().border(1.dp, Cyan.copy(alpha = 0.22f), RoundedCornerShape(12.dp))) {
                Column(Modifier.padding(18.dp)) {
                    Text("JARVIS", color = Gold, style = MaterialTheme.typography.labelMedium)
                    Text(state.reply, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 6.dp))
                    if (state.heard.isNotBlank()) {
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Cyan.copy(alpha = 0.15f))
                        Text("VOCÊ DISSE", color = Cyan, style = MaterialTheme.typography.labelSmall)
                        Text(state.heard, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = onToggle, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (state.enabled) Gold else Cyan)) {
                Text(if (state.enabled) "DESATIVAR ESCUTA" else "ATIVAR JARVIS", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onChatGpt, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("Abrir ChatGPT", textAlign = TextAlign.Center)
                }
                OutlinedButton(onClick = onAlarm, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("Criar alarme", textAlign = TextAlign.Center)
                }
            }
            Text("Diga “Jarvis” e aguarde a resposta. Depois, fale seu comando em até 20 segundos.",
                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp), color = Color(0xFFA7B8C8))
            Text("“Jarvis, abrir ChatGPT”  •  “Jarvis, alarme para 7:30”",
                color = Gold, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp))
            if (state.notice.isNotBlank()) {
                Text(state.notice, color = Gold, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp))
            }
            Text(state.voiceInfo, color = Color(0xFF8D9FB1), style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 20.dp))
            TextButton(onClick = onSettings) { Text("Permissões do aplicativo", color = Cyan) }
            Text("SEM API PAGA  /  CONTROLE PELO ANDROID", color = Gold.copy(alpha = 0.65f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun Reactor(active: Boolean) {
    val rotation = if (active) {
        val transition = rememberInfiniteTransition(label = "Reator")
        val angle by transition.animateFloat(0f, 360f,
            infiniteRepeatable(tween(12_000, easing = LinearEasing), RepeatMode.Restart), label = "Rotação")
        angle
    } else 0f
    Box(modifier = Modifier.size(210.dp).semantics { contentDescription = "Núcleo JARVIS" },
        contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val width = size.minDimension
            drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha = 0.20f), Color.Transparent)), radius = width / 2)
            drawCircle(Cyan.copy(alpha = 0.14f), radius = width * 0.47f, style = Stroke(1.dp.toPx()))
            rotate(rotation) {
                repeat(3) { index ->
                    drawArc(Cyan, index * 120f + 8f, 92f, false,
                        topLeft = Offset(width * 0.08f, width * 0.08f),
                        size = Size(width * 0.84f, width * 0.84f), style = Stroke(3.dp.toPx()))
                }
            }
            rotate(-rotation) {
                repeat(24) { index ->
                    rotate(index * 15f) {
                        drawLine(Gold.copy(alpha = 0.65f), Offset(center.x, width * 0.18f),
                            Offset(center.x, width * 0.21f), strokeWidth = 2.dp.toPx())
                    }
                }
            }
            drawCircle(Cyan, radius = width * 0.23f, style = Stroke(1.dp.toPx()))
        }
        Text("J", color = Gold, fontSize = 52.sp, fontWeight = FontWeight.ExtraLight)
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun JarvisPreview() { JarvisTheme { JarvisScreen(JarvisState(), {}, {}, {}, {}) } }
