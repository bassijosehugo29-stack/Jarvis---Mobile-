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
import androidx.compose.ui.draw.drawWithCache
import br.com.jarvis.mobile.experience.CinematicMotion
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
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
        drawRect(Brush.radialGradient(listOf(Color(0xFF031334).copy(alpha = 0.28f), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * 0.36f), radius = size.width * 0.85f))
        val grid = 28.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(Cyan.copy(alpha = 0.014f), Offset(x, 0f), Offset(x, size.height), 0.6.dp.toPx())
            x += grid
        }
        var y = 0f
        while (y < size.height) {
            drawLine(Cyan.copy(alpha = 0.014f), Offset(0f, y), Offset(size.width, y), 0.6.dp.toPx())
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

/** Electric-blue plasma core: cached geometry/brushes, independent orbital rings, no video/blur filter. */
@Composable
internal fun HolographicCore(
    visible: Boolean, phase: CorePhase, mode: OperationMode, audioLevel: Float,
    speechProgress: Float, speechPulse: Int, modifier: Modifier = Modifier,
    outputAudioLevel: Float? = null
) {
    val time = remember { mutableFloatStateOf(0f) }
    val formation = remember { Animatable(1f) }
    val params = rememberUpdatedState(CoreVisual(phase, mode, audioLevel, outputAudioLevel, speechProgress))
    LaunchedEffect(visible, phase) {
        if (visible && phase == CorePhase.INITIALIZING && ValueAnimator.areAnimatorsEnabled()) {
            formation.snapTo(0.08f); formation.animateTo(1f, tween(1_000, easing = FastOutSlowInEasing))
        } else formation.snapTo(1f)
    }
    LaunchedEffect(visible, phase, mode) {
        val interval = CinematicMotion.interval(visible, ValueAnimator.areAnimatorsEnabled(), phase, mode)
        if (interval == 0L) return@LaunchedEffect
        var previous = SystemClock.uptimeMillis()
        while (isActive) {
            delay(interval)
            val now = SystemClock.uptimeMillis()
            time.floatValue += (now - previous).coerceIn(0, 200) / 1000f * CinematicMotion.speed(phase)
            // Bound phase precision without observable jumps in the long periodic motions.
            if (time.floatValue > 20_000f) time.floatValue = 0f
            previous = now
        }
    }
    // Isolate draw invalidation from the static HUD; geometry stays stable for the render cache.
    Box(modifier.graphicsLayer().drawWithCache {
        val d = size.minDimension
        val c = Offset(size.width / 2f, size.height / 2f)
        val blue = Color(0xFF238CFF)
        val ice = Color(0xFF7CE6FF)
        val halo = Brush.radialGradient(0f to Color(0xFF237DFF).copy(alpha = 0.34f),
            0.48f to blue.copy(alpha = 0.12f), 1f to Color.Transparent, center = c, radius = d * 0.49f)
        val plasma = Brush.radialGradient(0f to Color.White, 0.22f to Color(0xFFF2FCFF),
            0.43f to Color(0xFFB6EFFF), 0.63f to Color(0xFF208BFF).copy(alpha = 0.75f),
            1f to Color.Transparent, center = c, radius = d * 0.185f)
        val vectors = Array(96) { i -> val a = i * 2f * PI.toFloat() / 96f; Offset(cos(a), sin(a)) }
        val orbitBounds = Size(d * 0.79f, d * 0.24f)
        val guideStroke = Stroke(0.6.dp.toPx())
        val ticks = Array(2) { Path() }
        vectors.forEachIndexed { i, vector ->
            val major = i % 8 == 0
            val outer = d * 0.478f
            val from = c + vector * (outer - (if (major) 5.dp.toPx() else 2.dp.toPx()))
            val to = c + vector * outer
            ticks[if (major) 1 else 0].apply { moveTo(from.x, from.y); lineTo(to.x, to.y) }
        }
        val tickStrokes = arrayOf(Stroke(0.55.dp.toPx()), Stroke(1.dp.toPx()))
        fun arc(radius: Float, start: Float, sweep: Float) = Path().apply {
            arcTo(Rect(c - Offset(radius, radius), Size(radius * 2f, radius * 2f)), start, sweep, true)
        }
        fun strokes(width: Float) = arrayOf(Stroke(width * 6f), Stroke(width * 2.8f), Stroke(width))
        val rings = Array(6) { ring ->
            val r = d * (0.215f + ring * 0.046f)
            CoreRing(r, arc(r, 0f, if (ring % 2 == 0) 112f else 188f), arc(r, 187f, 48f),
                strokes((if (ring == 2) 1.8f else 1.05f).dp.toPx()), strokes(0.65.dp.toPx()))
        }
        // Reuse the two organic contours and transform them; don't rebuild 192 vertices per frame.
        val coronas = Array(2) { band -> Path().apply {
            vectors.forEachIndexed { i, vector ->
                val noise = sin(i * 0.53f + band) * 0.014f + sin(i * 0.91f) * 0.009f
                val point = c + vector * (d * (0.13f + band * 0.028f + noise * 0.8f))
                if (i == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
            close()
        } }
        val coronaStrokes = arrayOf(Stroke(1.2.dp.toPx()), Stroke(0.8.dp.toPx()))
        onDrawBehind {
            val v = params.value
            val t = time.floatValue
            val error = v.phase == CorePhase.ERROR
            val accent = if (error) Color(0xFFFF735E) else blue
            val signal = when (v.phase) {
                CorePhase.LISTENING -> CinematicMotion.level(v.input) ?: 0f
                CorePhase.RESPONDING -> CinematicMotion.level(v.output) ?: (0.24f + 0.12f * sin(t * 4f))
                CorePhase.PROCESSING -> 0.38f + 0.10f * sin(t * 5f)
                CorePhase.ERROR -> 0.28f + 0.10f * sin(t * 4f)
                else -> 0.12f
            }
            val breath = 1f + 0.028f * sin(t * 2.1f) + 0.012f * sin(t * 3.7f + 0.9f)
            val formed = formation.value
            drawCircle(halo, d * 0.49f, c, alpha = 0.82f + signal * 0.18f)
            // An oblique back orbit establishes depth, without a rotating polygon or bitmap.
            rotate(-24f + 7f * sin(t * 0.2f)) {
                drawOval(accent.copy(alpha = 0.12f), c - Offset(orbitBounds.width/2, orbitBounds.height/2), orbitBounds,
                    style = Stroke(1.dp.toPx()))
            }
            for (ring in rings.indices) {
                val geometry = rings[ring]
                val direction = if (ring % 2 == 0) 1f else -1f
                val angle = direction * t * (11f + ring * 2.6f) + ring * 41f
                scale(0.85f + formed * 0.15f, pivot = c) {
                    drawCircle(accent.copy(alpha = (0.10f + ring * 0.018f) * formed), geometry.radius, c, style = guideStroke)
                    rotate(angle, pivot = c) {
                        drawCachedArc(geometry.primary, accent, geometry.primaryStrokes, formed, v.mode == OperationMode.CINEMATIC)
                        drawCachedArc(geometry.secondary, ice, geometry.secondaryStrokes, formed * (0.82f + signal * 0.18f), v.mode == OperationMode.CINEMATIC)
                    }
                }
            }
            // Two stable paths replace 96 independent line commands on every frame.
            drawPath(ticks[0], accent.copy(alpha = 0.17f), style = tickStrokes[0])
            drawPath(ticks[1], accent.copy(alpha = 0.62f), style = tickStrokes[1])
            val energy = (breath + signal * 0.12f) * formed.coerceAtLeast(0.1f)
            // White luminous centre with blue falloff. Scale is geometry, not expensive layer blur.
            drawCircle(plasma, d * 0.185f * energy, c)
            for (band in 0..1) {
                scale(energy * (1f + 0.018f * sin(t * (2f + band))), pivot = c) {
                    rotate(t * (if (band == 0) 8f else -5f), pivot = c) {
                        drawPath(coronas[band], (if (error) accent else ice).copy(alpha = 0.23f + signal * 0.25f), style = coronaStrokes[band])
                    }
                }
            }
            drawCircle(if (error) Color(0xFFFFDFD9) else Color(0xFFF6FDFF), d * 0.056f * energy, c)
            drawCircle(Color.White, d * 0.032f * energy, c)
            // Thin front orbit, highlights and small gold reference marks retain a full HUD.
            rotate(26f + t * 2.5f) {
                drawArc(ice.copy(alpha = 0.42f), 12f, 145f, false, c - Offset(d * 0.395f, d * 0.12f), orbitBounds,
                    style = Stroke(0.8.dp.toPx()))
            }
            if (v.mode == OperationMode.CINEMATIC) repeat(14) { i ->
                val angle = t * (0.15f + (i % 3) * 0.08f) + i * 2.39996f
                val distance = d * (0.23f + (i % 6) * 0.037f)
                drawCircle(ice.copy(alpha = 0.20f + 0.30f * abs(sin(angle))), (if (i % 4 == 0) 1.1f else 0.6f).dp.toPx(), c + Offset(cos(angle), sin(angle)) * distance)
            }
            if (v.phase == CorePhase.RESPONDING) drawGlowRing(d * 0.46f, -90f,
                360f * (CinematicMotion.level(v.progress) ?: 0f), Gold.copy(alpha = 0.6f), 0.65.dp.toPx())
            for (side in intArrayOf(-1, 1)) {
                drawLine(Gold.copy(alpha = 0.6f), c + Offset(side * d * 0.43f, -d * 0.22f),
                    c + Offset(side * d * 0.48f, -d * 0.22f), 0.7.dp.toPx())
            }
        }
    })
}
private data class CoreVisual(val phase: CorePhase, val mode: OperationMode, val input: Float, val output: Float?, val progress: Float)
private data class CoreRing(val radius: Float, val primary: Path, val secondary: Path,
    val primaryStrokes: Array<Stroke>, val secondaryStrokes: Array<Stroke>)
private fun DrawScope.drawCachedArc(path: Path, color: Color, strokes: Array<Stroke>, opacity: Float, cinematic: Boolean) {
    if (cinematic) drawPath(path, color.copy(alpha = color.alpha * 0.055f * opacity), style = strokes[0])
    drawPath(path, color.copy(alpha = color.alpha * 0.18f * opacity), style = strokes[1])
    drawPath(path, color.copy(alpha = color.alpha * 0.90f * opacity), style = strokes[2])
}
private fun DrawScope.drawGlowRing(radius: Float, start: Float, sweep: Float, color: Color, width: Float) {
    val origin = center - Offset(radius, radius)
    val bounds = Size(radius * 2f, radius * 2f)
    drawArc(color.copy(alpha = color.alpha * 0.055f), start, sweep, false, origin, bounds, style = Stroke(width * 6f))
    drawArc(color.copy(alpha = color.alpha * 0.18f), start, sweep, false, origin, bounds, style = Stroke(width * 2.8f))
    drawArc(color.copy(alpha = color.alpha * 0.90f), start, sweep, false, origin, bounds, style = Stroke(width))
}
