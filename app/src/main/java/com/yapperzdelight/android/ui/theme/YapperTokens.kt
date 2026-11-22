package com.yapperzdelight.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The one Yapperz Delight palette, mirrored 1:1 from the web studio design system
 * (wave4-perfection-1113/src/app/globals.css :root and :root[data-theme="dark"]).
 * Change a value here only together with its twin in globals.css, so Android and Web stay one brand.
 */
object YapperTokens {

    /** Light mode: blush paper, plum ink, couture magenta. The web default. */
    object Light {
        val Background = Color(0xFFFFF5F9) // --blush / --background
        val Card = Color(0xFFFFFFFF) // --cream / --card
        val CardSubtle = Color(0xFFFFF0F6) // --card-subtle / --surface-hover
        val Petal = Color(0xFFFFE1EF) // --petal / --accent-soft
        val Line = Color(0xFFFFE1EF) // --line
        val LineStrong = Color(0xFFF2C6DE)
        val Ink = Color(0xFF2B0A21) // --ink / --foreground
        val Muted = Color(0xFF5F3A52) // --muted
        val Accent = Color(0xFFD6006F) // --pink / --accent
        val AccentHover = Color(0xFFA80057) // --pink-hover
        val OnAccent = Color(0xFFFFFFFF) // --accent-foreground
        val AccentDeep = Color(0xFF8A0048) // --violet (deep magenta, used for text on petal)
        val Neon = Color(0xFFFF4FA3) // --neon

        val Success = Color(0xFF1E6B47)
        val SuccessSoft = Color(0xFFE8F5EE)
        val Warn = Color(0xFFA3401C)
        val WarnSoft = Color(0xFFFDEEE8)
        val Danger = Color(0xFFB42318)
        val DangerSoft = Color(0xFFFDECEC)
    }

    /** Dark mode: deep plum night with a brighter pink so text keeps its contrast. */
    object Dark {
        val Background = Color(0xFF2B0A21)
        val Card = Color(0xFF3A102D)
        val CardSubtle = Color(0xFF4A163A)
        val Petal = Color(0xFF5F1D4A)
        val Line = Color(0xFF5F1D4A)
        val LineStrong = Color(0xFF7A2D60)
        val Ink = Color(0xFFFFF5F9)
        val Muted = Color(0xFFFFD3E6)
        val Accent = Color(0xFFFF4FA3)
        val AccentHover = Color(0xFFFF7AB8)
        val OnAccent = Color(0xFF2B0A21)
        val AccentDeep = Color(0xFFFF9CCB)
        val Neon = Color(0xFFFF4FA3)

        val Success = Color(0xFF8FDDB4)
        val SuccessSoft = Color(0xFF143A28)
        val Warn = Color(0xFFFFB394)
        val WarnSoft = Color(0xFF4A2216)
        val Danger = Color(0xFFFFA49C)
        val DangerSoft = Color(0xFF4A1418)
    }

    /** The deepest plum, for the full screen video player in both modes. */
    val Immersive = Color(0xFF1A0914)
    val OnImmersive = Color(0xFFFFF5F9)

    /** Spacing scale (dp), the same 4 point rhythm the web uses through Tailwind. */
    object Space {
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 20.dp
        val xxl = 24.dp
        val section = 32.dp
        val page = 20.dp
    }

    /** Corner radii: web .card is rounded-3xl (24), fields rounded-xl (12), buttons and chips are pills. */
    object Radius {
        val card = RoundedCornerShape(24.dp)
        val tile = RoundedCornerShape(18.dp)
        val field = RoundedCornerShape(12.dp)
        val thumb = RoundedCornerShape(14.dp)
        val pill = RoundedCornerShape(percent = 50)
    }

    /** Touch targets: 48dp primary actions, never below 44dp (the web button floor). */
    val ButtonHeight = 52.dp
    val MinTouch = 48.dp
    val BorderWidth = 1.5.dp

    /** The live studio this app hands off to for cloud editing. */
    const val STUDIO_ORIGIN = "https://yapperzdelight.com"
}
