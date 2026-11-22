package com.yapperzdelight.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.yapperzdelight.android.R

/**
 * The web studio type pairing, bundled (OFL, see assets/licenses):
 * Instrument Serif for editorial display lines and the wordmark, Figtree for everything you read and tap.
 */
val Figtree = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold),
    Font(R.font.figtree_extrabold, FontWeight.ExtraBold),
)

val InstrumentSerif = FontFamily(
    Font(R.font.instrument_serif_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

private val serifDisplay = TextStyle(
    fontFamily = InstrumentSerif,
    fontWeight = FontWeight.Normal,
    letterSpacing = (-0.02).em,
)

private val sans = TextStyle(fontFamily = Figtree)

/**
 * Sized for creators 30 to 65: body copy never drops below 13sp and buttons read at 15sp extra bold,
 * matching the web .btn and .kicker classes.
 */
val YapperTypography = Typography(
    displayLarge = serifDisplay.copy(fontSize = 52.sp, lineHeight = 54.sp),
    displayMedium = serifDisplay.copy(fontSize = 44.sp, lineHeight = 46.sp),
    displaySmall = serifDisplay.copy(fontSize = 38.sp, lineHeight = 40.sp),
    headlineLarge = serifDisplay.copy(fontSize = 34.sp, lineHeight = 36.sp),
    headlineMedium = serifDisplay.copy(fontSize = 30.sp, lineHeight = 33.sp),
    headlineSmall = serifDisplay.copy(fontSize = 26.sp, lineHeight = 29.sp),
    titleLarge = sans.copy(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.01).em),
    titleMedium = sans.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 23.sp),
    titleSmall = sans.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = sans.copy(fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = sans.copy(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = sans.copy(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = sans.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.005).em),
    labelMedium = sans.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 17.sp),
    // The web .kicker: small, letter spaced, upper case, muted.
    labelSmall = sans.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.08.em),
)

/** The wordmark style, the same italic serif as the web .wordmark class. */
val WordmarkStyle = TextStyle(
    fontFamily = InstrumentSerif,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Normal,
    fontSize = 30.sp,
    lineHeight = 32.sp,
    letterSpacing = (-0.01).em,
)
