package com.yapperzdelight.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yapperzdelight.android.ui.theme.YapperIcons
import com.yapperzdelight.android.ui.theme.YapperTheme
import com.yapperzdelight.android.ui.theme.YapperTokens

data class TutorialStep(
    val icon: ImageVector,
    val title: String,
    val description: String,
)

/** The 1 minute tour. Opt in only (the help button), and every name in it matches a real button on screen. */
val YapperTutorialSteps = listOf(
    TutorialStep(
        icon = YapperIcons.NewYap,
        title = "Pick your look",
        description = "At the top of New yap, choose Storytime, Lifestyle, TikTok Shop or Just edit it. You get three quick tips for filming that kind of video.",
    ),
    TutorialStep(
        icon = YapperIcons.Plus,
        title = "Add your clips",
        description = "Tap Choose clips and pick up to 15 from your gallery. They stay on this phone until you decide what to do next.",
    ),
    TutorialStep(
        icon = YapperIcons.OpenExternal,
        title = "Edit, keep or share",
        description = "Tap Edit in my studio to make your yap at yapperzdelight.com, or Keep on this phone to find your clips in My yaps, ready to watch, save or share.",
    ),
)

@Composable
fun YapperTutorialWalkthroughDialog(onDismiss: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    val step = YapperTutorialSteps[index]
    val isLast = index == YapperTutorialSteps.lastIndex
    val c = YapperTheme.colors

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        YapperCard(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .widthIn(max = 440.dp)
                .semantics { contentDescription = "Tour, step ${index + 1} of ${YapperTutorialSteps.size}: ${step.title}" },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Kicker("Step ${index + 1} of ${YapperTutorialSteps.size}")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss, modifier = Modifier.height(YapperTokens.MinTouch)) {
                    Text("Skip", style = MaterialTheme.typography.labelMedium, color = c.muted)
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(c.petal, YapperTokens.Radius.tile),
                contentAlignment = Alignment.Center,
            ) {
                Icon(step.icon, contentDescription = null, tint = c.accentDeep, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(step.title, style = MaterialTheme.typography.headlineSmall, color = c.ink)
            Spacer(Modifier.height(8.dp))
            Text(step.description, style = MaterialTheme.typography.bodyMedium, color = c.muted)
            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 20.dp)) {
                YapperTutorialSteps.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(width = if (i == index) 22.dp else 8.dp, height = 8.dp)
                            .background(if (i == index) c.accent else c.line, YapperTokens.Radius.pill),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                if (index > 0) {
                    GhostPillButton(text = "Back", onClick = { index-- }, modifier = Modifier.weight(1f))
                }
                PrimaryPillButton(
                    text = if (isLast) "Start" else "Next",
                    onClick = { if (isLast) onDismiss() else index++ },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
