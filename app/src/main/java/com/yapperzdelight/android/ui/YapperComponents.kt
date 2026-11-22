package com.yapperzdelight.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.yapperzdelight.android.ui.theme.WordmarkStyle
import com.yapperzdelight.android.ui.theme.YapperTheme
import com.yapperzdelight.android.ui.theme.YapperTokens

/** "Yapperz Delight" in the web wordmark's italic serif, with Delight in the accent like the web header. */
@Composable
fun YapperWordmark(modifier: Modifier = Modifier) {
    val c = YapperTheme.colors
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = c.ink)) { append("Yapperz ") }
            withStyle(SpanStyle(color = c.accent)) { append("Delight") }
        },
        style = WordmarkStyle,
        modifier = modifier.semantics { heading() },
    )
}

/** The web .kicker: a small letter spaced eyebrow above a heading. */
@Composable
fun Kicker(text: String, modifier: Modifier = Modifier, color: Color = YapperTheme.colors.muted) {
    Text(text = text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)
}

/** Editorial section heading: kicker plus a serif line, the same pairing the web pages open with. */
@Composable
fun SectionHeading(kicker: String?, title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier = modifier) {
        if (kicker != null) {
            Kicker(kicker)
            Spacer(Modifier.size(6.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = YapperTheme.colors.ink,
            modifier = Modifier.semantics { heading() },
        )
        if (subtitle != null) {
            Spacer(Modifier.size(6.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = YapperTheme.colors.muted)
        }
    }
}

/** The web .card: card surface, 24dp corners, a quiet line border, no shadow. */
@Composable
fun YapperCard(
    modifier: Modifier = Modifier,
    color: Color = YapperTheme.colors.card,
    border: Color = YapperTheme.colors.line,
    contentPadding: PaddingValues = PaddingValues(YapperTokens.Space.xl),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = YapperTokens.Radius.card,
        color = color,
        contentColor = YapperTheme.colors.ink,
        border = BorderStroke(YapperTokens.BorderWidth, border),
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** The web .btn-accent: a magenta pill, extra bold label, 52dp tall. */
@Composable
fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val c = YapperTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = YapperTokens.Radius.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = c.accent,
            contentColor = c.onAccent,
            disabledContainerColor = c.cardSubtle,
            disabledContentColor = c.muted,
        ),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        modifier = modifier.heightIn(min = YapperTokens.ButtonHeight),
    ) {
        PillContent(text = text, icon = icon)
    }
}

/** The web .btn-ghost: card pill with a line border, ink label. */
@Composable
fun GhostPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    contentColor: Color = YapperTheme.colors.ink,
    containerColor: Color = YapperTheme.colors.card,
    borderColor: Color = YapperTheme.colors.lineStrong,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = YapperTokens.Radius.pill,
        border = BorderStroke(YapperTokens.BorderWidth, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = containerColor, contentColor = contentColor),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        modifier = modifier.heightIn(min = YapperTokens.MinTouch),
    ) {
        PillContent(text = text, icon = icon)
    }
}

@Composable
private fun PillContent(text: String, icon: ImageVector?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

enum class ChipTone { Petal, Success, Neutral }

/** A quiet status pill (the web .chip in petal), never a shouting badge. */
@Composable
fun StatusChip(text: String, modifier: Modifier = Modifier, tone: ChipTone = ChipTone.Petal) {
    val c = YapperTheme.colors
    val (bg, fg) = when (tone) {
        ChipTone.Petal -> c.petal to c.accentDeep
        ChipTone.Success -> c.successSoft to c.success
        ChipTone.Neutral -> c.cardSubtle to c.muted
    }
    Surface(shape = YapperTokens.Radius.pill, color = bg, contentColor = fg, modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .defaultMinSize(minHeight = 26.dp)
                .padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}
