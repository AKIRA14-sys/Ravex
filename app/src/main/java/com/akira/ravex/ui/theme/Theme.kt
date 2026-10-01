package com.akira.ravex.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val RavexBlack = Color(0xFF08080C)
val RavexSurface = Color(0xFF10121A)
val RavexSurfaceVariant = Color(0xFF1A1D28)
val RavexRed = Color(0xFFFF2A55)
val RavexCyan = Color(0xFF00F0FF)
val RavexGold = Color(0xFFFFD700)
val RavexGreen = Color(0xFF39FF14)
val RavexTextWhite = Color(0xFFF0F4F8)
val RavexTextMuted = Color(0xFF8A94A6)

private val DarkColorScheme = darkColorScheme(
    primary = RavexRed,
    secondary = RavexCyan,
    tertiary = RavexGold,
    background = RavexBlack,
    surface = RavexSurface,
    surfaceVariant = RavexSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = RavexTextWhite,
    onSurface = RavexTextWhite
)

@Composable
fun AkiraRavexTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
