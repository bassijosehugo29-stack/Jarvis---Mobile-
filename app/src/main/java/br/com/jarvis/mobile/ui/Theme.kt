package br.com.jarvis.mobile.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Cyan = Color(0xFF39C9FF)
val Gold = Color(0xFFD8B568)
val Background = Color(0xFF05080F)
val Panel = Color(0xFF101A28)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Cyan, onPrimary = Background, secondary = Gold,
        background = Background, surface = Panel, onBackground = Color(0xFFE9F5FF),
        onSurface = Color(0xFFE9F5FF)
    ), content = content)
}
