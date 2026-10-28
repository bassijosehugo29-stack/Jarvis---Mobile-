package br.com.jarvis.mobile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.jarvis.mobile.JarvisState
import br.com.jarvis.mobile.experience.ExperienceSettings
import br.com.jarvis.mobile.experience.OperationMode
import br.com.jarvis.mobile.experience.CorePhase
import kotlin.math.abs
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisScreen(
    state: JarvisState,
    onToggle: () -> Unit,
    onSettings: () -> Unit,
    onVoiceTest: () -> Unit = {},
    onVoiceSettings: () -> Unit = {},
    onDownloadModel: () -> Unit = {},
    onImportModel: () -> Unit = {},
    onSendMessage: (String) -> Unit = {},
    onCancelModel: () -> Unit = {},
    onExperienceChange: (ExperienceSettings) -> Unit = {},
    onLoadVoices: () -> Unit = {},
    onOpenHub: () -> Unit = {}
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val device = rememberHudDeviceState()
    var message by rememberSaveable { mutableStateOf("") }
    val send = {
        if (message.isNotBlank() && !state.thinking && !state.speaking && !state.working) {
            onSendMessage(message.trim()); message = ""
        }
    }
    Surface(Modifier.fillMaxSize(), color = Background) {
        Box(Modifier.fillMaxSize()) {
            HolographicBackground(Modifier.fillMaxSize())
            Column(Modifier.fillMaxHeight().widthIn(max = 620.dp).fillMaxWidth()
                .safeDrawingPadding().imePadding().align(Alignment.TopCenter)) {
                Header(onSettings = { showSettings = true }, onHub = onOpenHub)
                DeviceStrip(device)
                ModeSelector(state.experience, onExperienceChange, Modifier.padding(horizontal = 22.dp).padding(bottom = 10.dp))
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val diameter = (maxHeight * 0.60f).coerceIn(184.dp, 320.dp)
                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight).testTag("jarvis-content")
                        .padding(horizontal = 22.dp).padding(top = 6.dp, bottom = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(4.dp).background(if (state.listening) Cyan else Gold, CircleShape))
                                HudLabel(state.corePhase.label, Modifier.padding(start = 7.dp),
                                    if (state.corePhase == CorePhase.ERROR) Color(0xFFFF997C) else if (state.listening) Cyan else Gold)
                                Spacer(Modifier.weight(1f))
                                HudLabel(if (state.experience.mode == OperationMode.SILENT) "PT-BR / TEXTO" else "PT-BR / VOZ", color = Muted)
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
                            if (state.voiceIssue.isNotBlank()) {
                                Text(state.voiceIssue, color = Gold, fontSize = 13.sp, lineHeight = 20.sp,
                                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                                TextButton(onClick = { showSettings = true }) { Text("Ajustar a voz", color = Cyan) }
                            }
                            OutlinedTextField(value = message, onValueChange = { message = it.take(480) },
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("jarvis-message"),
                                label = { Text("Converse com o JARVIS") },
                                placeholder = { Text("Fale ou escreva sua mensagem") },
                                minLines = 1, maxLines = 3, enabled = !state.thinking && !state.speaking && !state.working,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = { send() }),
                                trailingIcon = {
                                    IconButton(onClick = { send() }, enabled = message.isNotBlank() && !state.thinking && !state.speaking && !state.working) {
                                        Icon(Symbols.External, "Enviar mensagem", tint = Cyan)
                                    }
                                })
                            if (!state.modelReady) {
                                TextButton(onClick = { showSettings = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (state.modelBusy) state.modelInfo else "Preparar conversa offline • grátis", color = Gold, fontSize = 12.sp)
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
                ExperienceControls(state, onExperienceChange, onLoadVoices)
                HorizontalDivider(Modifier.padding(vertical = 22.dp), color = Outline)
                Text("Como falar", fontSize = 15.sp, color = Gold, fontWeight = FontWeight.Medium)
                Text("Nos modos Assistente e Cinema, ative o núcleo para conversar. O JARVIS responde e volta a ouvir sem você precisar repetir o nome em cada frase. No modo Silencioso, cada toque autoriza uma escuta. Para parar, toque no núcleo ou diga parar.",
                    color = Muted, fontSize = 14.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 8.dp))
                Text("“Jarvis, abrir WhatsApp”\n“Jarvis, agende dentista amanhã às 15 horas”\n“Jarvis, alarme para sete e trinta”", color = Cyan,
                    fontSize = 14.sp, lineHeight = 25.sp, modifier = Modifier.padding(top = 12.dp))
                HorizontalDivider(Modifier.padding(vertical = 22.dp), color = Outline)
                Text("Agenda: permita o calendário no primeiro pedido. O compromisso dura uma hora se você não disser a duração. Com várias agendas, escolha por voz no primeiro uso. Ao abrir outro aplicativo, o microfone é desligado.", color = Muted, fontSize = 14.sp, lineHeight = 22.sp)
                Spacer(Modifier.height(18.dp))
                Text("Saída de voz", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(state.speechInfo, color = Muted, fontSize = 14.sp, lineHeight = 22.sp,
                    modifier = Modifier.padding(top = 8.dp))
                Text("Com o JARVIS aberto, os botões de volume ajustam a mídia. Se o áudio sair em fones ou Bluetooth, confira a saída de mídia do Samsung.",
                    color = Muted, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp))
                Button(onClick = onVoiceTest, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("Testar voz") }
                OutlinedButton(onClick = { showSettings = false; onVoiceSettings() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("Ajustar voz do Android")
                }
                HorizontalDivider(Modifier.padding(vertical = 22.dp), color = Outline)
                Text("Conversa offline", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(state.modelInfo, color = Muted, fontSize = 14.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 8.dp))
                Text("Modelo gratuito de 491 MB, baixado uma vez por Wi-Fi. Depois, as respostas são geradas no celular. O modelo é leve: pode errar, responder de forma simples e levar alguns segundos. Ao desinstalar o app, o arquivo local é removido.",
                    color = Muted, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp))
                if (state.modelBusy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                    TextButton(onClick = onCancelModel, modifier = Modifier.fillMaxWidth()) { Text("Cancelar preparação") }
                }
                if (!state.modelReady) {
                    Button(onClick = onDownloadModel, enabled = !state.modelBusy,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("Baixar conversa offline • 491 MB") }
                    OutlinedButton(onClick = { showSettings = false; onImportModel() }, enabled = !state.modelBusy,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Importar modelo já baixado") }
                }
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
                Text("JARVIS Mobile · 3.0.0", color = Muted.copy(alpha = 0.65f), fontSize = 12.sp,
                    modifier = Modifier.padding(top = 18.dp))
            }
        }
    }
}

@Composable
private fun Header(onSettings: () -> Unit, onHub: () -> Unit) {
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
        IconButton(onClick = onHub, modifier = Modifier.size(44.dp).testTag("hub-open")) {
            Text("⋮", color = Cyan, fontSize = 26.sp, modifier = Modifier.semantics { contentDescription = "Centrais pessoais" })
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
    val label = when {
        state.thinking || state.working -> "Parar resposta"
        state.speaking && !state.enabled -> "Interromper fala"
        state.enabled -> "Pausar escuta"
        else -> "Ativar JARVIS"
    }
    Column(Modifier.width(diameter).semantics(mergeDescendants = true) {
        contentDescription = label
        stateDescription = state.corePhase.label + if (state.enabled) " · Escuta ativada" else " · Escuta desativada"
    }.clickable(role = Role.Button, onClick = onToggle), horizontalAlignment = Alignment.CenterHorizontally) {
        HolographicCore(visible, state.corePhase, state.experience.mode, state.audioLevel,
            state.speechProgress, state.speechPulse, Modifier.size(diameter).testTag("jarvis-core"))
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
                    fontFamily = FontFamily.Monospace, color = Muted, modifier = Modifier.testTag("jarvis-heard"))
            }
            Text(if (state.thinking) "Pensando na sua resposta…" else state.reply,
                color = Color(0xFFD1F8FF), fontSize = 14.sp, lineHeight = 21.sp,
                modifier = Modifier.padding(top = if (state.heard.isBlank()) 0.dp else 8.dp).testTag("jarvis-reply"))
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
            state.thinking -> "Microfone pausado enquanto penso"
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
private fun JarvisPreview() { JarvisTheme { JarvisScreen(JarvisState(), {}, {}) } }

@Preview(showBackground = true, widthDp = 360, heightDp = 740, fontScale = 1.5f)
@Composable
private fun JarvisLargeFontPreview() { JarvisTheme { JarvisScreen(JarvisState(), {}, {}) } }
