package com.yapperzdelight.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Brand colours Material 3 has no slot for, read through [YapperTheme.colors]. */
@Immutable
data class YapperColors(
    val background: Color,
    val card: Color,
    val cardSubtle: Color,
    val petal: Color,
    val line: Color,
    val lineStrong: Color,
    val ink: Color,
    val muted: Color,
    val accent: Color,
    val accentHover: Color,
    val onAccent: Color,
    val accentDeep: Color,
    val neon: Color,
    val success: Color,
    val successSoft: Color,
    val warn: Color,
    val warnSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val isDark: Boolean,
)

private val LightYapperColors = with(YapperTokens.Light) {
    YapperColors(
        background = Background, card = Card, cardSubtle = CardSubtle, petal = Petal, line = Line,
        lineStrong = LineStrong, ink = Ink, muted = Muted, accent = Accent, accentHover = AccentHover,
        onAccent = OnAccent, accentDeep = AccentDeep, neon = Neon, success = Success, successSoft = SuccessSoft,
        warn = Warn, warnSoft = WarnSoft, danger = Danger, dangerSoft = DangerSoft, isDark = false,
    )
}

private val DarkYapperColors = with(YapperTokens.Dark) {
    YapperColors(
        background = Background, card = Card, cardSubtle = CardSubtle, petal = Petal, line = Line,
        lineStrong = LineStrong, ink = Ink, muted = Muted, accent = Accent, accentHover = AccentHover,
        onAccent = OnAccent, accentDeep = AccentDeep, neon = Neon, success = Success, successSoft = SuccessSoft,
        warn = Warn, warnSoft = WarnSoft, danger = Danger, dangerSoft = DangerSoft, isDark = true,
    )
}

private fun schemeFor(c: YapperColors): ColorScheme {
    val base = if (c.isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = c.accent,
        onPrimary = c.onAccent,
        primaryContainer = c.petal,
        onPrimaryContainer = c.accentDeep,
        inversePrimary = c.neon,
        secondary = c.ink,
        onSecondary = c.background,
        secondaryContainer = c.petal,
        onSecondaryContainer = c.ink,
        tertiary = c.neon,
        onTertiary = YapperTokens.Light.Ink,
        tertiaryContainer = c.cardSubtle,
        onTertiaryContainer = c.ink,
        background = c.background,
        onBackground = c.ink,
        surface = c.background,
        onSurface = c.ink,
        surfaceVariant = c.cardSubtle,
        onSurfaceVariant = c.muted,
        surfaceTint = c.accent,
        surfaceBright = c.card,
        surfaceDim = c.cardSubtle,
        surfaceContainerLowest = c.card,
        surfaceContainerLow = c.card,
        surfaceContainer = c.card,
        surfaceContainerHigh = c.cardSubtle,
        surfaceContainerHighest = c.petal,
        inverseSurface = c.ink,
        inverseOnSurface = c.background,
        outline = c.lineStrong,
        outlineVariant = c.line,
        error = c.danger,
        onError = if (c.isDark) c.dangerSoft else Color.White,
        errorContainer = c.dangerSoft,
        onErrorContainer = c.danger,
        scrim = YapperTokens.Immersive,
    )
}

private val LocalYapperColors = staticCompositionLocalOf { LightYapperColors }

object YapperTheme {
    val colors: YapperColors
        @Composable
        @ReadOnlyComposable
        get() = LocalYapperColors.current
}

/**
 * The Yapperz Delight theme. Follows the phone's light or dark setting, exactly like the web studio's
 * light default and dark toggle. No dynamic (wallpaper) colour: the brand is the colour.
 */
@Composable
fun Yapper2Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkYapperColors else LightYapperColors
    CompositionLocalProvider(LocalYapperColors provides colors) {
        MaterialTheme(
            colorScheme = schemeFor(colors),
            typography = YapperTypography,
            content = content,
        )
    }
}
