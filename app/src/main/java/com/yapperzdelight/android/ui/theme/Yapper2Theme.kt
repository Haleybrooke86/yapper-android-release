package com.yapperzdelight.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val YapperInkBackground = Color(0xFF151113)
val YapperSurface = Color(0xFF20191D)
val YapperSurfaceVariant = Color(0xFF2C2228)
val YapperBorder = Color(0xFF45353F)
val YapperHotPink = Color(0xFFFF2E7E)
val YapperHotPinkLight = Color(0xFFFF8FC7)
val YapperViolet = Color(0xFF9B6BE0)
val YapperWarmPaper = Color(0xFFF7F0E7)
val YapperTextMuted = Color(0xFFB5A7AF)
val YapperSuccess = Color(0xFF4ADE80)

private val DarkColorScheme = darkColorScheme(
    primary = YapperHotPink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5C102E),
    onPrimaryContainer = YapperHotPinkLight,
    secondary = YapperViolet,
    onSecondary = Color.White,
    background = YapperInkBackground,
    onBackground = YapperWarmPaper,
    surface = YapperSurface,
    onSurface = YapperWarmPaper,
    surfaceVariant = YapperSurfaceVariant,
    onSurfaceVariant = YapperTextMuted,
    outline = YapperBorder
)

@Composable
fun Yapper2Theme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
