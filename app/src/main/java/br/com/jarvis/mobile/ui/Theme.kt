package br.com.jarvis.mobile.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Cyan = Color(0xFF6EDBFF)
val Gold = Color(0xFFD6BA7A)
val Background = Color(0xFF080B11)
val Panel = Color(0xFF111720)
val Muted = Color(0xFF9BA9BB)
val Outline = Color(0xFF26303D)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Cyan, onPrimary = Background, secondary = Gold,
        background = Background, surface = Panel,
        onBackground = Color(0xFFF0F4FA), onSurface = Color(0xFFF0F4FA),
        onSurfaceVariant = Muted, outline = Outline
    ), content = content)
}
