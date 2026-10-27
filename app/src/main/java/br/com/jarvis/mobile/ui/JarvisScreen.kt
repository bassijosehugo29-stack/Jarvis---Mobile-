package br.com.jarvis.mobile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.jarvis.mobile.JarvisState
import kotlin.math.abs
import kotlin.math.sin

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
    val device = rememberHudDeviceState()
    Surface(Modifier.fillMaxSize(), color = Background) {
        Box(Modifier.fillMaxSize()) {
            HolographicBackground(Modifier.fillMaxSize())
            Column(Modifier.fillMaxHeight().widthIn(max = 620.dp).fillMaxWidth()
                .safeDrawingPadding().align(Alignment.TopCenter)) {
                Header(onSettings = { showSettings = true })
                DeviceStrip(device)
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val diameter = (maxHeight * 0.52f).coerceIn(184.dp, 286.dp)
                    val stacked = LocalDensity.current.fontScale >= 1.35f || maxWidth < 350.dp
                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight).testTag("jarvis-content")
                        .padding(horizontal = 22.dp).padding(top = 6.dp, bottom = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(4.dp).background(if (state.listening) Cyan else Gold, CircleShape))
                                HudLabel(when {
                                    state.speaking -> "RESPONDENDO"
                                    state.listening -> "OUVINDO"
                                    state.enabled -> "PREPARANDO A VOZ"
                                    else -> "EM ESPERA"
                                }, Modifier.padding(start = 7.dp), if (state.listening) Cyan else Gold)
                                Spacer(Modifier.weight(1f))
                                HudLabel("PT-BR / VOZ", color = Muted)
                            }
                            Spacer(Modifier.height(6.dp))
                            VoiceControl(state, device.visible, diameter, onToggle)
                            Spacer(Modifier.height(13.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                HudLabel(if (state.listening) "MIC ATIVO" else "MIC PAUSADO", color = Muted)
                                VoiceSignal(state, Modifier.weight(1f).padding(start = 14.dp).height(16.dp))
                            }
                        }
                        Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
                            CommunicationPanel(state)
                            Spacer(Modifier.height(12.dp))
                            if (stacked) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    HudAction("CHATGPT", "Abrir aplicativo", Symbols.Chat, "Abrir ChatGPT",
                                        onChatGpt, Modifier.fillMaxWidth())
                                    HudAction("ALARMES", "Definir horário", Symbols.Alarm, "Criar alarme",
                                        onAlarm, Modifier.fillMaxWidth())
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    HudAction("CHATGPT", "Abrir aplicativo", Symbols.Chat, "Abrir ChatGPT",
                                        onChatGpt, Modifier.weight(1f))
                                    HudAction("ALARMES", "Definir horário", Symbols.Alarm, "Criar alarme",
                                        onAlarm, Modifier.weight(1f))
                                }
                            }
                            if (state.notice.isNotBlank()) {
                                TextButton(onClick = { showSettings = true },
                                    contentPadding = PaddingValues(horizontal = 0.dp),
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
                                    Icon(Symbols.Info, null, Modifier.size(14.dp), tint = Gold)
                                    Text("Orientações de voz", color = Gold, fontSize = 12.sp,
                                        modifier = Modifier.padding(start = 7.dp))
                                    Spacer(Modifier.weight(1f))
                                    Icon(Symbols.External, null, Modifier.size(14.dp), tint = Gold)
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
                Text("JARVIS Mobile · 0.3.0", color = Muted.copy(alpha = 0.65f), fontSize = 12.sp,
                    modifier = Modifier.padding(top = 18.dp))
            }
        }
    }
}

@Composable
private fun Header(onSettings: () -> Unit) {
    val large = LocalDensity.current.fontScale >= 1.35f
    Row(Modifier.fillMaxWidth().testTag("jarvis-header").padding(horizontal = 22.dp)
        .padding(top = 8.dp, bottom = 8.dp).heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Symbols.Brand, null, Modifier.size(26.dp), tint = Cyan)
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text("J.A.R.V.I.S.", fontSize = if (large) 18.sp else 21.sp,
                letterSpacing = 1.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace,
                color = Color(0xFFE7FCFF), style = TextStyle(shadow = Shadow(Cyan.copy(alpha = 0.55f), blurRadius = 12f)))
            Text("ASSISTENTE DE VOZ", color = Muted, fontSize = 8.sp, letterSpacing = 1.8.sp,
                fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 3.dp))
        }
        HudPanel {
            IconButton(onClick = onSettings, modifier = Modifier.size(44.dp)) {
                Icon(Symbols.Settings, "Configurações", Modifier.size(20.dp), tint = Cyan)
            }
        }
    }
}

@Composable
private fun DeviceStrip(device: HudDeviceState) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(device.clock, fontSize = 23.sp, letterSpacing = 1.3.sp,
                fontFamily = FontFamily.Monospace, color = Cyan,
                style = TextStyle(shadow = Shadow(Cyan.copy(alpha = 0.45f), blurRadius = 10f)))
            Text(device.date, fontSize = 9.sp, letterSpacing = 1.sp, fontFamily = FontFamily.Monospace,
                color = Muted, modifier = Modifier.padding(top = 3.dp))
        }
        Column(horizontalAlignment = Alignment.End) {
            HudLabel("BATERIA", color = Muted)
            Text(device.battery, color = Gold, fontFamily = FontFamily.Monospace,
                fontSize = 18.sp, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun HudLabel(text: String, modifier: Modifier = Modifier, color: Color = Cyan) {
    Text(text, color = color, fontFamily = FontFamily.Monospace, fontSize = 9.sp,
        lineHeight = 14.sp, letterSpacing = 1.sp, modifier = modifier)
}

@Composable
private fun VoiceControl(state: JarvisState, visible: Boolean, diameter: Dp, onToggle: () -> Unit) {
    val label = if (state.enabled) "Pausar escuta" else "Ativar JARVIS"
    Column(Modifier.width(diameter).semantics(mergeDescendants = true) {
        contentDescription = label
        stateDescription = if (state.enabled) "Escuta ativada" else "Escuta desativada"
    }.clickable(role = Role.Button, onClick = onToggle), horizontalAlignment = Alignment.CenterHorizontally) {
        HolographicCore(visible, state.listening, state.speaking, state.audioLevel, Modifier.size(diameter))
        HudPanel(Modifier.widthIn(min = 170.dp)) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(if (state.enabled) Symbols.Pause else Symbols.Microphone, null, Modifier.size(16.dp), tint = Cyan)
                Text(label, color = Cyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(start = 9.dp))
            }
        }
    }
}

@Composable
private fun VoiceSignal(state: JarvisState, modifier: Modifier) {
    Canvas(modifier) {
        val amplitude = if (state.listening && state.audioLevel.isFinite()) state.audioLevel.coerceIn(0f, 1f) else 0f
        val spacing = size.width / 36f
        for (i in 0 until 36) {
            val height = 1.5.dp.toPx() + amplitude * abs(sin(i * 0.73f)) * (size.height - 2.dp.toPx())
            drawLine(Cyan.copy(alpha = if (state.listening) 0.9f else 0.20f),
                Offset(i * spacing, (size.height - height) / 2f),
                Offset(i * spacing, (size.height + height) / 2f), 1.4.dp.toPx())
        }
    }
}

@Composable
private fun CommunicationPanel(state: JarvisState) {
    HudPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HudLabel("COMUNICAÇÃO")
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(4.dp).background(if (state.enabled) Cyan else Muted, CircleShape))
            }
            HorizontalDivider(Modifier.padding(top = 10.dp, bottom = 11.dp), color = Cyan.copy(alpha = 0.13f))
            if (state.heard.isNotBlank()) {
                Text("> " + state.heard, fontSize = 12.sp, lineHeight = 19.sp,
                    fontFamily = FontFamily.Monospace, color = Muted)
                Text(state.reply, color = Color(0xFFD1F8FF), fontSize = 14.sp, lineHeight = 21.sp,
                    modifier = Modifier.padding(top = 8.dp))
            } else {
                Text(when { state.listening -> "Diga “Jarvis”. Estou ouvindo."; state.enabled -> "Preparando o microfone…"; else -> "Às suas ordens, senhor." },
                    color = Color(0xFFD1F8FF), fontSize = 15.sp, lineHeight = 22.sp)
                Text(when { state.listening -> "Depois, fale seu comando."; state.enabled -> "Aguarde o indicador de escuta."; else -> "Ative o núcleo e diga “Jarvis”." },
                    color = Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun HudAction(title: String, subtitle: String, symbol: ImageVector,
    description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    HudPanel(modifier.semantics { contentDescription = description }.clickable(role = Role.Button, onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(symbol, null, Modifier.size(22.dp), tint = Cyan)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(title, color = Color(0xFFE2FAFF), fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                Text(subtitle, color = Muted, fontSize = 10.sp, lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun PrivacyLine(state: JarvisState) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Icon(Symbols.Shield, null, Modifier.size(12.dp), tint = Muted)
        Text(when {
            state.listening -> "Microfone em uso nesta tela"
            state.speaking -> "Microfone pausado durante a resposta"
            state.enabled -> "Preparando a escuta"
            else -> "Microfone desligado"
        }, color = Muted, fontSize = 10.sp, lineHeight = 15.sp,
            fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 7.dp))
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun JarvisPreview() { JarvisTheme { JarvisScreen(JarvisState(), {}, {}, {}, {}) } }

@Preview(showBackground = true, widthDp = 360, heightDp = 740, fontScale = 1.5f)
@Composable
private fun JarvisLargeFontPreview() { JarvisTheme { JarvisScreen(JarvisState(), {}, {}, {}, {}) } }
