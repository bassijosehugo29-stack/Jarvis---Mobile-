package br.com.jarvis.mobile.ui

import android.animation.ValueAnimator
import android.os.SystemClock
import android.content.Context
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import br.com.jarvis.mobile.experience.CorePhase
import br.com.jarvis.mobile.experience.OperationMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.*

private val Brazilian = Locale.forLanguageTag("pt-BR")
private val ClockFormat = DateTimeFormatter.ofPattern("HH:mm:ss", Brazilian)
private val DateFormat = DateTimeFormatter.ofPattern("EEE · dd MMM", Brazilian)

internal data class HudDeviceState(
    val clock: String, val date: String, val battery: String, val visible: Boolean
)

/** Real system clock/battery only. Polling and animation stop when the Activity is no longer visible. */
@Composable
internal fun rememberHudDeviceState(): HudDeviceState {
    val context = LocalContext.current.applicationContext
    val owner = LocalLifecycleOwner.current
    fun readDevice() = LocalDateTime.now().let { time ->
        val battery = (context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager)
            ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 }
        HudDeviceState(time.format(ClockFormat), time.format(DateFormat).uppercase(Brazilian),
            battery?.let { "$it%" } ?: "—", owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    var state by remember(context, owner) { mutableStateOf(readDevice()) }
    DisposableEffect(context, owner) {
        val handler = Handler(Looper.getMainLooper())
        val ticker = object : Runnable {
            override fun run() {
                state = readDevice()
                if (state.visible) handler.postDelayed(this, 1_000L)
            }
        }
        val observer = LifecycleEventObserver { _, _ ->
            handler.removeCallbacks(ticker)
            state = readDevice()
            if (state.visible) ticker.run()
        }
        owner.lifecycle.addObserver(observer)
        handler.removeCallbacks(ticker)
        if (state.visible) ticker.run()
        onDispose { handler.removeCallbacks(ticker); owner.lifecycle.removeObserver(observer) }
    }
    return state
}

@Composable
internal fun HudPanel(
    modifier: Modifier = Modifier,
    accent: Color = Cyan,
    content: @Composable () -> Unit
) {
    Surface(modifier = modifier, shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
        color = Panel.copy(alpha = 0.90f), border = BorderStroke(0.7.dp, accent.copy(alpha = 0.27f))) {
        content()
    }
}

@Composable
internal fun HolographicBackground(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(Brush.radialGradient(listOf(Color(0xFF00425D).copy(alpha = 0.32f), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * 0.36f), radius = size.width * 0.85f))
        val grid = 28.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(Cyan.copy(alpha = 0.025f), Offset(x, 0f), Offset(x, size.height), 0.6.dp.toPx())
            x += grid
        }
        var y = 0f
        while (y < size.height) {
            drawLine(Cyan.copy(alpha = 0.025f), Offset(0f, y), Offset(size.width, y), 0.6.dp.toPx())
            y += grid
        }
        val horizon = size.height * 0.67f
        for (i in -6..6) {
            drawLine(Cyan.copy(alpha = 0.065f), Offset(size.width / 2f + i * 12.dp.toPx(), horizon),
                Offset(size.width / 2f + i * size.width * 0.26f, size.height), 0.7.dp.toPx())
        }
        for (i in 1..6) {
            val row = horizon + (size.height - horizon) * (i / 6f).pow(2)
            drawLine(Cyan.copy(alpha = 0.055f), Offset(0f, row), Offset(size.width, row), 0.7.dp.toPx())
        }
        for (side in listOf(14.dp.toPx(), size.width - 14.dp.toPx())) {
            drawLine(Cyan.copy(alpha = 0.16f), Offset(side, 100.dp.toPx()), Offset(side, size.height - 80.dp.toPx()), 0.6.dp.toPx())
            for (i in 0..3) {
                val top = size.height * 0.19f + i * 18.dp.toPx()
                drawLine(Cyan.copy(alpha = if (i == 0) 0.65f else 0.15f), Offset(side - 2.dp.toPx(), top),
                    Offset(side + 2.dp.toPx(), top), 1.dp.toPx())
            }
        }
    }
}

private data class Vertex(val x: Float, val y: Float, val z: Float)
private data class Projected(val position: Offset, val depth: Float)
private val Phi = ((1f + sqrt(5f)) / 2f)
private val MeshVertices = listOf(
    Vertex(-1f, Phi, 0f), Vertex(1f, Phi, 0f), Vertex(-1f, -Phi, 0f), Vertex(1f, -Phi, 0f),
    Vertex(0f, -1f, Phi), Vertex(0f, 1f, Phi), Vertex(0f, -1f, -Phi), Vertex(0f, 1f, -Phi),
    Vertex(Phi, 0f, -1f), Vertex(Phi, 0f, 1f), Vertex(-Phi, 0f, -1f), Vertex(-Phi, 0f, 1f)
)
private val MeshEdges = buildList {
    for (i in MeshVertices.indices) for (j in i + 1 until MeshVertices.size) {
        val a = MeshVertices[i]; val b = MeshVertices[j]
        val distance = (a.x - b.x).pow(2) + (a.y - b.y).pow(2) + (a.z - b.z).pow(2)
        if (abs(distance - 4f) < 0.01f) add(i to j)
    }
}
private val MeshFaces = buildList {
    for (i in MeshVertices.indices) for (j in i + 1 until MeshVertices.size) for (k in j + 1 until MeshVertices.size) {
        if ((i to j) in MeshEdges && (i to k) in MeshEdges && (j to k) in MeshEdges) add(Triple(i, j, k))
    }
}

/** Native vector HUD: rotating arcs, a projected wire polyhedron and a voice envelope. */
@Composable
internal fun HolographicCore(
    visible: Boolean, phase: CorePhase, mode: OperationMode, audioLevel: Float,
    speechProgress: Float, speechPulse: Int, modifier: Modifier = Modifier
) {
    val rotation = remember { mutableFloatStateOf(0f) }
    val formation = remember { Animatable(0f) }
    val wordEnvelope = remember { Animatable(0.15f) }
    LaunchedEffect(visible, phase) {
        if (visible && phase == CorePhase.INITIALIZING) {
            formation.snapTo(0.05f); formation.animateTo(1f, tween(1_100))
        } else formation.snapTo(1f)
    }
    LaunchedEffect(speechPulse, phase) {
        if (phase == CorePhase.RESPONDING) {
            wordEnvelope.snapTo(0.95f); wordEnvelope.animateTo(0.15f, tween(260))
        } else wordEnvelope.snapTo(0f)
    }
    // Bounded redraw rate. Idle silent is static; Android's animation setting is respected.
    LaunchedEffect(visible, phase, mode) {
        if (!visible || !ValueAnimator.areAnimatorsEnabled() || (mode == OperationMode.SILENT && phase == CorePhase.IDLE)) return@LaunchedEffect
        val interval = when {
            mode == OperationMode.SILENT -> 250L
            phase == CorePhase.IDLE && mode == OperationMode.ASSISTANT -> 200L
            phase == CorePhase.IDLE -> 66L
            mode == OperationMode.CINEMATIC -> 33L
            else -> 66L
        }
        val speed = when (phase) { CorePhase.PROCESSING -> 32f; CorePhase.INITIALIZING -> 25f
            CorePhase.RESPONDING -> 18f; CorePhase.LISTENING -> 10f; else -> 5f }
        var previous = SystemClock.uptimeMillis()
        while (isActive) {
            delay(interval)
            val now = SystemClock.uptimeMillis()
            rotation.floatValue = (rotation.floatValue + (now - previous).coerceAtMost(300) * speed / 1000f) % 360f
            previous = now
        }
    }
    Canvas(modifier) {
        val accent = if (phase == CorePhase.ERROR) Color(0xFFFF997C) else Cyan
        val d = size.minDimension
        val turn = rotation.floatValue
        val level = if (phase == CorePhase.LISTENING && audioLevel.isFinite()) audioLevel.coerceIn(0f, 1f) else 0f
        val pulse = when (phase) {
            CorePhase.RESPONDING -> max(wordEnvelope.value, 0.20f + 0.12f * abs(sin(turn * PI.toFloat() / 10f)))
            CorePhase.PROCESSING -> 0.45f + 0.18f * sin(turn * PI.toFloat() / 12f)
            CorePhase.IDLE -> 0.08f + 0.04f * sin(turn * PI.toFloat() / 15f)
            else -> level
        }
        val formed = formation.value
        val outer = d * 0.46f
        drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.13f + pulse * 0.08f), Color.Transparent),
            center = center, radius = d * 0.5f), radius = d * 0.5f)
        drawCircle(accent.copy(alpha = 0.12f), radius = outer, style = Stroke(0.6.dp.toPx()))
        rotate(turn * 0.28f) {
            repeat(96) { index ->
                val radians = index * (2f * PI.toFloat() / 96f)
                val major = index % 8 == 0
                val length = if (major) 7.dp.toPx() else 3.dp.toPx()
                val inner = outer - length
                val vector = Offset(cos(radians), sin(radians))
                drawLine(accent.copy(alpha = if (major) 0.7f else 0.25f), center + vector * inner,
                    center + vector * outer, if (major) 1.2.dp.toPx() else 0.7.dp.toPx())
            }
        }
        rotate(-turn * 0.65f) {
            for (index in 0..3) glowArc(d * 0.405f, index * 90f + 5f, 64f * formed, accent, 1.7.dp.toPx())
        }
        rotate(turn * 0.85f) {
            glowArc(d * 0.365f, 18f, 205f * formed, Color(0xFF2FAEFF), 2.dp.toPx())
            glowArc(d * 0.365f, 246f, 48f * formed, Gold, 1.1.dp.toPx())
        }
        drawCircle(accent.copy(alpha = 0.25f), radius = d * 0.335f, style = Stroke(0.6.dp.toPx()))
        val globe = Path().apply { addOval(Rect(center - Offset(d * 0.30f, d * 0.30f), Size(d * 0.60f, d * 0.60f))) }
        clipPath(globe) {
            rotate(-18f) {
                for (fraction in listOf(0.3f, 0.65f, 1f)) {
                    drawOval(accent.copy(alpha = 0.07f), topLeft = Offset(center.x - d * 0.3f * fraction, center.y - d * 0.3f),
                        size = Size(d * 0.6f * fraction, d * 0.6f), style = Stroke(0.7.dp.toPx()))
                }
                for (offset in listOf(-0.17f, 0f, 0.17f)) {
                    drawOval(accent.copy(alpha = 0.08f), topLeft = Offset(center.x - d * 0.3f, center.y - d * 0.065f + d * offset),
                        size = Size(d * 0.6f, d * 0.13f), style = Stroke(0.6.dp.toPx()))
                }
            }
        }
        drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.12f), Color.Transparent)), radius = d * 0.29f)
        drawMesh(turn, d * (0.65f + 0.35f * formed), accent)
        rotate(-turn * 0.4f) {
            repeat(60) { index ->
                val radians = index * (2f * PI.toFloat() / 60f)
                val extension = pulse * (3f + 8f * abs(sin(index * 0.72f))) * density
                val vector = Offset(cos(radians), sin(radians))
                drawLine(accent.copy(alpha = 0.2f + pulse * 0.7f), center + vector * (d * 0.315f),
                    center + vector * (d * 0.315f + extension), 1.1.dp.toPx())
            }
        }
        if (mode == OperationMode.CINEMATIC && visible) {
            repeat(18) { index ->
                val a = (turn * (0.3f + index % 3 * 0.1f) + index * 137.5f) * PI.toFloat() / 180f
                val distance = d * (0.22f + (index % 7) * 0.035f)
                val position = center + Offset(cos(a), sin(a)) * distance
                drawCircle(accent.copy(alpha = 0.15f + 0.35f * abs(sin(a * 2))),
                    radius = if (index % 4 == 0) 1.2.dp.toPx() else 0.65.dp.toPx(), center = position)
            }
        }
        if (phase == CorePhase.RESPONDING && speechProgress.isFinite()) {
            glowArc(d * 0.285f, -90f, 360f * speechProgress.coerceIn(0f, 1f), Gold, 0.8.dp.toPx())
        }
        val strut = d * 0.15f
        for (side in listOf(-1f, 1f)) {
            drawLine(accent.copy(alpha = 0.6f), Offset(center.x + side * d * 0.43f, center.y - strut),
                Offset(center.x + side * d * 0.49f, center.y - strut), 1.dp.toPx())
            drawLine(accent.copy(alpha = 0.4f), Offset(center.x + side * d * 0.43f, center.y + strut),
                Offset(center.x + side * d * 0.49f, center.y + strut), 1.dp.toPx())
        }
    }
}

private fun DrawScope.glowArc(radius: Float, start: Float, sweep: Float, color: Color, width: Float) {
    val origin = center - Offset(radius, radius)
    val bounds = Size(radius * 2, radius * 2)
    for ((scale, alpha) in listOf(5f to 0.055f, 2.4f to 0.17f, 1f to 0.9f)) {
        drawArc(color.copy(alpha = alpha), start, sweep, false, origin, bounds, style = Stroke(width * scale))
    }
}

private fun DrawScope.drawMesh(turn: Float, diameter: Float, accent: Color) {
    val yaw = (turn + 20f) * PI.toFloat() / 180f
    val tilt = (turn * 0.42f + 28f) * PI.toFloat() / 180f
    val vertices = MeshVertices.map { vertex ->
        val x = vertex.x * cos(yaw) + vertex.z * sin(yaw)
        val z = -vertex.x * sin(yaw) + vertex.z * cos(yaw)
        val y = vertex.y * cos(tilt) - z * sin(tilt)
        val depth = vertex.y * sin(tilt) + z * cos(tilt)
        val scale = diameter * 0.11f * 5f / (5f - depth)
        Projected(center + Offset(x * scale, -y * scale), depth)
    }
    for ((a, b, c) in MeshFaces.sortedBy { (vertices[it.first].depth + vertices[it.second].depth + vertices[it.third].depth) }) {
        val path = Path().apply {
            moveTo(vertices[a].position.x, vertices[a].position.y)
            lineTo(vertices[b].position.x, vertices[b].position.y)
            lineTo(vertices[c].position.x, vertices[c].position.y); close()
        }
        drawPath(path, accent.copy(alpha = 0.025f))
    }
    for ((a, b) in MeshEdges) {
        val alpha = ((vertices[a].depth + vertices[b].depth) / 8f + 0.55f).coerceIn(0.2f, 0.95f)
        drawLine(accent.copy(alpha = alpha * 0.16f), vertices[a].position, vertices[b].position, 4.dp.toPx())
        drawLine(accent.copy(alpha = alpha), vertices[a].position, vertices[b].position, 0.9.dp.toPx())
    }
    vertices.forEachIndexed { index, vertex ->
        drawCircle(if (index % 4 == 0) Gold.copy(alpha = 0.9f) else Color(0xFFD8FAFF),
            radius = 1.3.dp.toPx(), center = vertex.position)
    }
}
