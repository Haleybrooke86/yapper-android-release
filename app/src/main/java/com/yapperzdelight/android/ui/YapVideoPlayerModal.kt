package com.yapperzdelight.android.ui

import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.widget.Toast
import com.yapperzdelight.android.data.LocalYap
import com.yapperzdelight.android.ui.theme.YapperIcons
import com.yapperzdelight.android.ui.theme.YapperTheme
import com.yapperzdelight.android.ui.theme.YapperTokens

/**
 * Full screen player for a clip kept on this phone. Plays the real file in a 9:16 frame on deep plum,
 * with Save and Share under the thumb. If the file cannot play, it says so; it never pretends.
 */
@Composable
fun YapVideoPlayerModal(
    video: LocalYap,
    onDismiss: () -> Unit,
    onSave: (LocalYap) -> Unit,
) {
    val context = LocalContext.current
    val c = YapperTheme.colors
    var playbackFailed by remember(video.id) { mutableStateOf(!video.exists) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(YapperTokens.Immersive)
                .safeDrawingPadding(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = YapperTokens.OnImmersive),
                        modifier = Modifier.size(YapperTokens.MinTouch),
                    ) {
                        Icon(YapperIcons.Close, contentDescription = "Close the player")
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                        Text(video.title, style = MaterialTheme.typography.titleMedium, color = YapperTokens.OnImmersive)
                        Text(
                            "${formatDuration(video.durationMs)} · Original clip, not edited yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = YapperTokens.OnImmersive.copy(alpha = 0.72f),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (playbackFailed) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(YapperIcons.Film, contentDescription = null, tint = c.neon, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "This clip will not play on this phone.",
                                style = MaterialTheme.typography.titleMedium,
                                color = YapperTokens.OnImmersive,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "It may have been moved or deleted. You can still edit the original in your online studio.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = YapperTokens.OnImmersive.copy(alpha = 0.72f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.widthIn(max = 320.dp),
                            )
                        }
                    } else {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    val controller = MediaController(ctx)
                                    controller.setAnchorView(this)
                                    setMediaController(controller)
                                    setVideoPath(video.filePath)
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = true
                                        start()
                                    }
                                    setOnErrorListener { _, _, _ ->
                                        playbackFailed = true
                                        true
                                    }
                                }
                            },
                            onRelease = { it.stopPlayback() },
                            modifier = Modifier
                                .aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                                .clip(YapperTokens.Radius.tile)
                                .background(Color.Black),
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PrimaryPillButton(
                        text = "Save to Photos",
                        icon = YapperIcons.Save,
                        onClick = { onSave(video) },
                        enabled = video.exists,
                        modifier = Modifier.weight(1f),
                    )
                    GhostPillButton(
                        text = "Share",
                        icon = YapperIcons.Share,
                        onClick = {
                            if (!shareVideoFile(context, video.file, video.title)) {
                                Toast.makeText(context, "This video is no longer on your phone, so it cannot be shared.", Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = video.exists,
                        contentColor = YapperTokens.OnImmersive,
                        containerColor = Color.Transparent,
                        borderColor = YapperTokens.OnImmersive.copy(alpha = 0.4f),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** 0:42 style durations, the way the web yap cards show them. */
fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val total = (ms + 500) / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
