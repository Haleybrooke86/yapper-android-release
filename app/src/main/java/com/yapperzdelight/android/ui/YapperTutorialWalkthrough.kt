package com.yapperzdelight.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yapperzdelight.android.ui.theme.*

data class TutorialStep(
    val stepNumber: Int,
    val iconEmoji: String,
    val title: String,
    val location: String,
    val badge: String,
    val description: String,
    val tip: String
)

val YapperTutorialSteps = listOf(
    TutorialStep(
        stepNumber = 1,
        iconEmoji = "🎥",
        title = "Upload Raw Takes",
        location = "Top of Screen • Step 1",
        badge = "Up to 15 Clips",
        description = "Tap 'Select Raw Takes' at the very top of your screen. Pick 1 to 15 video clips from your gallery. 4K HDR takes stream smoothly with zero phone memory lag.",
        tip = "💡 Tap 'Clear' anytime to swap out your chosen clips."
    ),
    TutorialStep(
        stepNumber = 2,
        iconEmoji = "🎨",
        title = "Choose Creator Look",
        location = "Middle Carousel • Step 2",
        badge = "AI Styling",
        description = "Swipe through the creator styles: 'Talking Head' for centered safe captions & silence removal, 'Lifestyle & Vlog' for aesthetic cuts, or 'TikTok Shop' for high-converting hook pacing.",
        tip = "💡 Tap on any look to highlight and select it."
    ),
    TutorialStep(
        stepNumber = 3,
        iconEmoji = "⚡",
        title = "Render in Background",
        location = "Pink Action Button",
        badge = "Background Service",
        description = "Tap the hot pink 'Render 2.0 Polished Yap' button. You'll see a green notification confirming your video is sent to the render engine. You can safely switch apps while it works!",
        tip = "💡 You don't have to keep the screen open while it edits."
    ),
    TutorialStep(
        stepNumber = 4,
        iconEmoji = "📲",
        title = "Find & Export Videos",
        location = "Scroll Down to Bottom",
        badge = "Save to Photos",
        description = "Scroll down to 'My Videos' at the bottom of the screen! When your video shows the green 'Ready' badge, tap 'Save to Photos' to save it right into your phone's Gallery, or tap 'Share' to post directly.",
        tip = "👇 Remember: Always scroll down to find your completed videos!"
    )
)

@Composable
fun YapperTutorialWalkthroughDialog(
    onDismiss: () -> Unit
) {
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = YapperTutorialSteps[currentStepIndex]
    val isLastStep = currentStepIndex == YapperTutorialSteps.lastIndex

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = YapperSurface),
            border = BorderStroke(1.5.dp, YapperHotPink),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(YapperHotPink.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = step.location.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = YapperHotPinkLight
                        )
                    }

                    Text(
                        text = "Step ${step.stepNumber} of ${YapperTutorialSteps.size}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = YapperTextMuted
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Icon / Emoji Circle
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(YapperSurfaceVariant, CircleShape)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = step.iconEmoji,
                        fontSize = 32.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title & Badge
                Text(
                    text = step.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = YapperWarmPaper,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = step.badge,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = YapperViolet
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Description
                Text(
                    text = step.description,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = YapperWarmPaper.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Pro Tip Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = YapperSurfaceVariant,
                    border = BorderStroke(1.dp, YapperBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = step.tip,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = YapperHotPinkLight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Dot Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    YapperTutorialSteps.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == currentStepIndex) 10.dp else 8.dp)
                                .background(
                                    color = if (index == currentStepIndex) YapperHotPink else YapperBorder,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentStepIndex > 0) {
                        OutlinedButton(
                            onClick = { currentStepIndex-- },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, YapperBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Back", color = YapperWarmPaper, fontSize = 14.sp)
                        }
                    } else {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Skip", color = YapperTextMuted, fontSize = 14.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (isLastStep) {
                                onDismiss()
                            } else {
                                currentStepIndex++
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = YapperHotPink),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (isLastStep) "Got It! Let's Yap 🎉" else "Next Step ➔",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
