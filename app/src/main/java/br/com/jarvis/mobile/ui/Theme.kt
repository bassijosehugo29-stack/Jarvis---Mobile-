package br.com.jarvis.mobile.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Cyan = Color(0xFF53E8FF)
val Gold = Color(0xFFD6BA7A)
val Background = Color(0xFF03090F)
val Panel = Color(0xFF081B27)
val Muted = Color(0xFF91ADB9)
val Outline = Color(0xFF194250)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Cyan, onPrimary = Background, secondary = Gold,
        background = Background, surface = Panel,
        onBackground = Color(0xFFF0F4FA), onSurface = Color(0xFFF0F4FA),
        onSurfaceVariant = Muted, outline = Outline
    ), content = content)
}
