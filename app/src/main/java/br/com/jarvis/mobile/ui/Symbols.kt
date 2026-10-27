package br.com.jarvis.mobile.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Small, original vector symbols. No icon SDK or downloaded assets. */
internal object Symbols {
    private fun outline(name: String, draw: PathBuilder.() -> Unit) = ImageVector.Builder(
        name = name, defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(fill = null, stroke = androidx.compose.ui.graphics.SolidColor(Color.White),
            strokeLineWidth = 1.65f, strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round, pathFillType = PathFillType.NonZero, pathBuilder = draw)
    }.build()

    val Brand = outline("Jarvis") {
        moveTo(12f, 2.5f); lineTo(20.3f, 7.3f); lineTo(20.3f, 16.7f)
        lineTo(12f, 21.5f); lineTo(3.7f, 16.7f); lineTo(3.7f, 7.3f); close()
        moveTo(8.5f, 10f); lineTo(12f, 8f); lineTo(15.5f, 10f)
        lineTo(15.5f, 14f); lineTo(12f, 16f); lineTo(8.5f, 14f); close()
        moveTo(12f, 16f); lineTo(12f, 21.5f)
    }
    val Microphone = outline("Microfone") {
        moveTo(9f, 6f); curveTo(9f, 2f, 15f, 2f, 15f, 6f)
        lineTo(15f, 11f); curveTo(15f, 15f, 9f, 15f, 9f, 11f); close()
        moveTo(5.5f, 10.5f); lineTo(5.5f, 11.5f)
        curveTo(5.5f, 20f, 18.5f, 20f, 18.5f, 11.5f); lineTo(18.5f, 10.5f)
        moveTo(12f, 18f); lineTo(12f, 21f)
        moveTo(8.5f, 21f); lineTo(15.5f, 21f)
    }
    val Pause = outline("Pausar") {
        moveTo(8.5f, 6f); lineTo(8.5f, 18f)
        moveTo(15.5f, 6f); lineTo(15.5f, 18f)
    }
    val Chat = outline("Chat") {
        moveTo(6f, 4f); lineTo(18f, 4f); curveTo(20f, 4f, 21f, 5f, 21f, 7f)
        lineTo(21f, 15f); curveTo(21f, 17f, 20f, 18f, 18f, 18f)
        lineTo(10f, 18f); lineTo(5f, 21f); lineTo(5f, 17.8f)
        curveTo(3.5f, 17.3f, 3f, 16.5f, 3f, 15f); lineTo(3f, 7f)
        curveTo(3f, 5f, 4f, 4f, 6f, 4f); close()
        moveTo(7.5f, 9f); lineTo(16.5f, 9f)
        moveTo(7.5f, 13f); lineTo(13.5f, 13f)
    }
    val Alarm = outline("Alarme") {
        moveTo(19f, 13f); curveTo(19f, 22.3f, 5f, 22.3f, 5f, 13f)
        curveTo(5f, 3.7f, 19f, 3.7f, 19f, 13f); close()
        moveTo(12f, 9f); lineTo(12f, 13f); lineTo(15f, 15f)
        moveTo(4f, 5f); lineTo(7f, 2.8f)
        moveTo(17f, 2.8f); lineTo(20f, 5f)
        moveTo(7.5f, 19f); lineTo(5.5f, 21f)
        moveTo(16.5f, 19f); lineTo(18.5f, 21f)
    }
    val Settings = outline("Ajustes") {
        moveTo(4f, 6f); lineTo(10f, 6f); moveTo(15f, 6f); lineTo(20f, 6f)
        moveTo(4f, 12f); lineTo(6f, 12f); moveTo(11f, 12f); lineTo(20f, 12f)
        moveTo(4f, 18f); lineTo(13f, 18f); moveTo(18f, 18f); lineTo(20f, 18f)
        moveTo(10f, 3.5f); lineTo(15f, 3.5f); lineTo(15f, 8.5f); lineTo(10f, 8.5f); close()
        moveTo(6f, 9.5f); lineTo(11f, 9.5f); lineTo(11f, 14.5f); lineTo(6f, 14.5f); close()
        moveTo(13f, 15.5f); lineTo(18f, 15.5f); lineTo(18f, 20.5f); lineTo(13f, 20.5f); close()
    }
    val External = outline("Abrir") {
        moveTo(7f, 17f); lineTo(17f, 7f)
        moveTo(7f, 7f); lineTo(17f, 7f); lineTo(17f, 17f)
    }
    val Shield = outline("Privacidade") {
        moveTo(12f, 3f); lineTo(20f, 6f); lineTo(20f, 11f)
        curveTo(20f, 16.5f, 15.5f, 20f, 12f, 21.5f)
        curveTo(8.5f, 20f, 4f, 16.5f, 4f, 11f); lineTo(4f, 6f); close()
        moveTo(8.5f, 12f); lineTo(11f, 14.5f); lineTo(15.5f, 9.5f)
    }
    val Info = outline("Informacao") {
        moveTo(12f, 3f); curveTo(24f, 3f, 24f, 21f, 12f, 21f)
        curveTo(0f, 21f, 0f, 3f, 12f, 3f); close()
        moveTo(12f, 11f); lineTo(12f, 16f)
        moveTo(12f, 7.5f); lineTo(12f, 7.6f)
    }
}
