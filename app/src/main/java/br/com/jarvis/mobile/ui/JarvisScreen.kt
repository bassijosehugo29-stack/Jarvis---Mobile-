package br.com.jarvis.mobile.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.jarvis.mobile.JarvisState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisScreen(
    state: JarvisState,
    onToggle: () -> Unit,
    onChatGpt: () -> Unit,
    onAlarm: () -> Unit,
    onSettings: () -> Unit
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = Background) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.radialGradient(
                    colors = listOf(Color(0xFF113044).copy(alpha = 0.40f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.28f),
                    radius = size.width * 0.85f
                ))
            }
            // Header and privacy line are outside the scroll container: they never scroll under system bars.
            Column(Modifier.fillMaxHeight().widthIn(max = 600.dp).fillMaxWidth().safeDrawingPadding()
                .align(Alignment.TopCenter)) {
                Header(onSettings = { showSettings = true })
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val compact = maxHeight < 660.dp
                    val coreSize = when {
                        maxHeight < 540.dp -> 174.dp
                        compact -> 206.dp
                        else -> 246.dp
                    }
                    val stackActions = LocalDensity.current.fontScale >= 1.35f || maxWidth < 350.dp
                    Column(
                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                            .heightIn(min = maxHeight).testTag("jarvis-content")
                            .padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            StateBadge(state)
                            Spacer(Modifier.height(if (compact) 4.dp else 12.dp))
                            VoiceCore(state, coreSize, onToggle)
                            Spacer(Modifier.height(if (compact) 14.dp else 22.dp))
                            Text(
                                when {
                                    state.speaking -> "Às suas ordens."
                                    state.listening -> "Estou ouvindo."
                                    state.enabled -> "Pode falar."
                                    else -> "Como posso ajudar?"
                                },
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 26.sp, lineHeight = 32.sp,
                                letterSpacing = (-0.7).sp, fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                when {
                                    state.speaking -> "JARVIS está respondendo."
                                    state.listening -> "Diga seu comando. Estou aqui."
                                    state.enabled -> state.status
                                    else -> "Toque no microfone e diga “Jarvis”."
                                },
                                color = Muted, fontSize = 14.sp, lineHeight = 21.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp).widthIn(max = 290.dp)
                            )
                        }
                        Column(Modifier.fillMaxWidth().padding(top = 28.dp)) {
                            if (state.heard.isNotBlank()) {
                                Conversation(state)
                                Spacer(Modifier.height(20.dp))
                            }
                            Text("Ações rápidas", fontSize = 15.sp, fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 12.dp))
                            if (stackActions) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    QuickAction("ChatGPT", "Abrir aplicativo", Symbols.Chat, Cyan,
                                        "Abrir ChatGPT", onChatGpt, Modifier.fillMaxWidth(), horizontal = true)
                                    QuickAction("Alarmes", "Definir horário", Symbols.Alarm, Gold,
                                        "Criar alarme", onAlarm, Modifier.fillMaxWidth(), horizontal = true)
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    QuickAction("ChatGPT", "Abrir aplicativo", Symbols.Chat, Cyan,
                                        "Abrir ChatGPT", onChatGpt, Modifier.weight(1f))
                                    QuickAction("Alarmes", "Definir horário", Symbols.Alarm, Gold,
                                        "Criar alarme", onAlarm, Modifier.weight(1f))
                                }
                            }
                            if (state.notice.isNotBlank()) {
                                TextButton(onClick = { showSettings = true },
                                    contentPadding = PaddingValues(horizontal = 0.dp),
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                                    Icon(Symbols.Info, null, Modifier.size(16.dp), tint = Gold)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Ver orientação de voz", fontSize = 13.sp, color = Gold)
                                    Spacer(Modifier.weight(1f))
                                    Icon(Symbols.External, null, Modifier.size(16.dp), tint = Gold)
                                }
                            }
                        }
                    }
                }
                PrivacyLine(state)
            }
        }
    }
    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Panel, contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Muted.copy(alpha = 0.45f)) }
        ) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
                Text("Voz e permissões", fontSize = 24.sp, fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.5).sp)
                Text("Você controla quando o JARVIS pode ouvir.", color = Muted,
                    fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 8.dp))
                HorizontalDivider(Modifier.padding(vertical = 22.dp), color = Outline)
                Text("Como falar", fontSize = 15.sp, color = Gold, fontWeight = FontWeight.Medium)
                Text("Ative o microfone e diga “Jarvis”. Depois da resposta, fale seu comando em até 20 segundos.",
                    color = Muted, fontSize = 14.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 8.dp))
                Text("“Jarvis, abrir ChatGPT”\n“Jarvis, alarme para 7:30”", color = Cyan,
                    fontSize = 14.sp, lineHeight = 25.sp, modifier = Modifier.padding(top = 12.dp))
                HorizontalDivider(Modifier.padding(vertical = 22.dp), color = Outline)
                Text("Reconhecimento de voz", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(state.voiceInfo, color = Muted, fontSize = 14.sp, lineHeight = 22.sp,
                    modifier = Modifier.padding(top = 8.dp))
                if (state.notice.isNotBlank()) {
                    Surface(color = Gold.copy(alpha = 0.08f), shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        Text(state.notice, color = Gold, fontSize = 14.sp, lineHeight = 22.sp,
                            modifier = Modifier.padding(16.dp))
                    }
                }
                OutlinedButton(onClick = { showSettings = false; onSettings() },
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp).heightIn(min = 52.dp),
                    shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Outline)) {
                    Icon(Symbols.Settings, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Abrir permissões do aplicativo")
                }
                Text("JARVIS Mobile · 0.2.0", color = Muted.copy(alpha = 0.65f), fontSize = 12.sp,
                    modifier = Modifier.padding(top = 18.dp))
            }
        }
    }
}

@Composable
private fun Header(onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().testTag("jarvis-header").padding(horizontal = 24.dp).padding(top = 10.dp, bottom = 12.dp)
        .heightIn(min = 54.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Symbols.Brand, null, Modifier.size(34.dp), tint = Gold)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text("JARVIS", fontSize = 18.sp, letterSpacing = 3.5.sp,
                fontWeight = FontWeight.SemiBold, color = Color(0xFFF1F4F9))
            Text("Assistente pessoal", color = Muted, fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp))
        }
        Surface(color = Panel, shape = CircleShape, border = BorderStroke(1.dp, Outline)) {
            IconButton(onClick = onSettings, modifier = Modifier.size(44.dp)) {
                Icon(Symbols.Settings, "Configurações", Modifier.size(20.dp), tint = Muted)
            }
        }
    }
}

@Composable
private fun StateBadge(state: JarvisState) {
    val label = when {
        state.speaking -> "RESPONDENDO"
        state.listening -> "OUVINDO"
        state.enabled -> "CONECTANDO À VOZ"
        else -> "EM ESPERA"
    }
    val color = if (state.listening) Cyan else Gold
    Surface(color = color.copy(alpha = 0.06f), shape = CircleShape,
        border = BorderStroke(1.dp, color.copy(alpha = 0.12f))) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(5.dp).background(color, CircleShape))
            Text(label, color = color, fontSize = 9.sp, letterSpacing = 1.8.sp,
                fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 7.dp))
        }
    }
}

@Composable
private fun VoiceCore(state: JarvisState, diameter: Dp, onToggle: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.965f else 1f, tween(150), label = "Pressão")
    val active = state.listening || state.speaking
    val angle = if (active) {
        val transition = rememberInfiniteTransition(label = "Voz")
        val rotation by transition.animateFloat(0f, 360f,
            infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart), label = "Órbita")
        rotation
    } else 0f
    val label = if (state.enabled) "Pausar escuta" else "Ativar JARVIS"
    Column(Modifier.width(diameter).graphicsLayer(scaleX = scale, scaleY = scale)
        .semantics(mergeDescendants = true) {
            contentDescription = label
            stateDescription = if (state.enabled) "Escuta ativada" else "Escuta desativada"
        }.clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onToggle),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val d = size.minDimension
                val radius = d * 0.325f
                drawCircle(Brush.radialGradient(
                    listOf(Cyan.copy(alpha = if (active) 0.17f else 0.095f), Color.Transparent),
                    center = center, radius = d * 0.50f
                ), radius = d * 0.50f)
                drawCircle(Brush.radialGradient(
                    colors = listOf(Color(0xFF2A627C), Color(0xFF102D40), Color(0xFF09141F)),
                    center = Offset(center.x - radius * 0.55f, center.y - radius * 0.65f),
                    radius = radius * 2.3f
                ), radius = radius)
                val globe = Path().apply { addOval(androidx.compose.ui.geometry.Rect(
                    center.x - radius, center.y - radius, center.x + radius, center.y + radius)) }
                clipPath(globe) {
                    rotate(-25f) {
                        for (fraction in listOf(0.32f, 0.67f, 1f)) {
                            drawOval(Cyan.copy(alpha = 0.075f),
                                topLeft = Offset(center.x - radius * fraction, center.y - radius),
                                size = Size(radius * 2f * fraction, radius * 2f), style = Stroke(0.7.dp.toPx()))
                        }
                        for (fraction in listOf(-0.45f, 0f, 0.45f)) {
                            drawOval(Cyan.copy(alpha = 0.07f),
                                topLeft = Offset(center.x - radius, center.y - radius * 0.21f + radius * fraction),
                                size = Size(radius * 2f, radius * 0.42f), style = Stroke(0.7.dp.toPx()))
                        }
                    }
                }
                drawCircle(Brush.linearGradient(listOf(Cyan.copy(alpha = 0.65f), Color(0xFF142A39),
                    Cyan.copy(alpha = 0.16f))), radius = radius, style = Stroke(0.85.dp.toPx()))
                rotate(-32f + angle) {
                    drawOval(Brush.linearGradient(listOf(Color.Transparent, Cyan.copy(alpha = 0.65f),
                        Color.Transparent)), topLeft = Offset(d * 0.055f, d * 0.26f),
                        size = Size(d * 0.89f, d * 0.48f), style = Stroke(1.dp.toPx()))
                }
                rotate(44f - angle * 0.45f) {
                    drawOval(Gold.copy(alpha = 0.18f), topLeft = Offset(d * 0.12f, d * 0.32f),
                        size = Size(d * 0.76f, d * 0.36f), style = Stroke(0.7.dp.toPx()))
                }
                drawArc(Gold.copy(alpha = 0.70f), startAngle = 223f, sweepAngle = 16f, useCenter = false,
                    topLeft = Offset(d * 0.09f, d * 0.09f), size = Size(d * 0.82f, d * 0.82f),
                    style = Stroke(1.4.dp.toPx()))
            }
            Surface(shape = CircleShape, color = Color(0xFF0B1824).copy(alpha = 0.88f),
                border = BorderStroke(1.dp, Cyan.copy(alpha = 0.22f)),
                shadowElevation = 8.dp, modifier = Modifier.size(diameter * 0.31f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(if (state.enabled) Symbols.Pause else Symbols.Microphone, null,
                        Modifier.size(29.dp), tint = if (state.enabled) Cyan else Gold)
                }
            }
        }
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            letterSpacing = 0.2.sp, color = if (state.enabled) Cyan else Gold,
            modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun QuickAction(
    title: String, subtitle: String, symbol: ImageVector, accent: Color,
    description: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    horizontal: Boolean = false
) {
    Surface(onClick = onClick, modifier = modifier.semantics { contentDescription = description },
        shape = RoundedCornerShape(20.dp), color = Panel,
        border = BorderStroke(1.dp, Outline.copy(alpha = 0.65f))) {
        if (horizontal) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(symbol, null, Modifier.size(26.dp), tint = accent)
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(subtitle, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Icon(Symbols.External, null, Modifier.size(16.dp), tint = Muted)
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(symbol, null, Modifier.size(25.dp), tint = accent)
                    Spacer(Modifier.weight(1f))
                    Icon(Symbols.External, null, Modifier.size(15.dp), tint = Muted.copy(alpha = 0.65f))
                }
                Spacer(Modifier.height(20.dp))
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun Conversation(state: JarvisState) {
    Surface(color = Cyan.copy(alpha = 0.035f), shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Outline.copy(alpha = 0.7f))) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("VOCÊ", fontSize = 9.sp, letterSpacing = 1.5.sp, color = Muted)
            Text(state.heard, fontSize = 13.sp, lineHeight = 20.sp,
                color = Muted, modifier = Modifier.padding(top = 5.dp))
            Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(4.dp).background(Gold, CircleShape))
                Text("JARVIS", fontSize = 9.sp, letterSpacing = 1.5.sp,
                    color = Gold, modifier = Modifier.padding(start = 7.dp))
            }
            Text(state.reply, fontSize = 15.sp, lineHeight = 23.sp,
                modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun PrivacyLine(state: JarvisState) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Icon(Symbols.Shield, null, Modifier.size(13.dp), tint = Muted.copy(alpha = 0.7f))
        Text(when {
            state.listening -> "Microfone em uso nesta tela"
            state.speaking -> "Microfone pausado durante a resposta"
            state.enabled -> "Preparando a escuta"
            else -> "Microfone desligado"
        }, fontSize = 10.sp, lineHeight = 15.sp, color = Muted.copy(alpha = 0.8f),
            textAlign = TextAlign.Center, modifier = Modifier.padding(start = 7.dp))
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun JarvisPreview() { JarvisTheme { JarvisScreen(JarvisState(), {}, {}, {}, {}) } }

@Preview(showBackground = true, widthDp = 360, heightDp = 740, fontScale = 1.5f)
@Composable
private fun JarvisLargeFontPreview() { JarvisTheme { JarvisScreen(JarvisState(), {}, {}, {}, {}) } }
