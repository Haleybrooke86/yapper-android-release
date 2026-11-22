package com.yapperzdelight.android.ui

import android.media.MediaPlayer
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
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
    val haptic = LocalHapticFeedback.current
    var playbackFailed by remember(video.id) { mutableStateOf(!video.exists) }
    var videoView by remember(video.id) { mutableStateOf<VideoView?>(null) }
    var player by remember(video.id) { mutableStateOf<MediaPlayer?>(null) }
    var prepared by remember(video.id) { mutableStateOf(false) }
    var playing by remember(video.id) { mutableStateOf(false) }
    var ended by remember(video.id) { mutableStateOf(false) }
    var muted by remember(video.id) { mutableStateOf(false) }
    var scrubbing by remember(video.id) { mutableStateOf(false) }
    var positionMs by remember(video.id) { mutableStateOf(0) }
    var lengthMs by remember(video.id) { mutableStateOf(video.durationMs.toInt()) }

    LaunchedEffect(videoView, prepared, scrubbing) {
        val view = videoView ?: return@LaunchedEffect
        while (prepared && !scrubbing) {
            positionMs = view.currentPosition
            playing = view.isPlaying
            delay(150)
        }
    }

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
                                    setVideoPath(video.filePath)
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = false
                                        player = mp
                                        lengthMs = duration.coerceAtLeast(1)
                                        prepared = true
                                        playing = true
                                        start()
                                    }
                                    setOnCompletionListener {
                                        ended = true
                                        playing = false
                                        positionMs = lengthMs
                                    }
                                    setOnErrorListener { _, _, _ ->
                                        playbackFailed = true
                                        true
                                    }
                                    videoView = this
                                }
                            },
                            onRelease = {
                                it.stopPlayback()
                                prepared = false
                                videoView = null
                                player = null
                            },
                            modifier = Modifier
                                .aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                                .clip(YapperTokens.Radius.tile)
                                .background(Color.Black)
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = if (playing) "Pause" else "Play",
                                ) {
                                    val view = videoView ?: return@clickable
                                    if (!prepared) return@clickable
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (view.isPlaying) {
                                        view.pause()
                                        playing = false
                                    } else {
                                        if (ended) { view.seekTo(0); ended = false }
                                        view.start()
                                        playing = true
                                    }
                                },
                        )
                    }
                }

                if (!playbackFailed) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = {
                                val view = videoView ?: return@IconButton
                                if (!prepared) return@IconButton
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                when {
                                    ended -> { view.seekTo(0); view.start(); ended = false; playing = true }
                                    view.isPlaying -> { view.pause(); playing = false }
                                    else -> { view.start(); playing = true }
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = YapperTokens.OnImmersive),
                            modifier = Modifier.size(YapperTokens.MinTouch),
                        ) {
                            Icon(
                                imageVector = when {
                                    ended -> YapperIcons.Replay
                                    playing -> YapperIcons.Pause
                                    else -> YapperIcons.Play
                                },
                                contentDescription = when {
                                    ended -> "Replay"
                                    playing -> "Pause"
                                    else -> "Play"
                                },
                            )
                        }
                        Text(
                            formatDuration(positionMs.toLong()),
                            style = MaterialTheme.typography.labelMedium,
                            color = YapperTokens.OnImmersive.copy(alpha = 0.8f),
                        )
                        Slider(
                            value = positionMs.coerceIn(0, lengthMs).toFloat(),
                            onValueChange = {
                                scrubbing = true
                                positionMs = it.toInt()
                                videoView?.seekTo(it.toInt())
                            },
                            onValueChangeFinished = {
                                scrubbing = false
                                ended = false
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            valueRange = 0f..lengthMs.coerceAtLeast(1).toFloat(),
                            enabled = prepared,
                            colors = SliderDefaults.colors(
                                thumbColor = c.accent,
                                activeTrackColor = c.accent,
                                inactiveTrackColor = YapperTokens.OnImmersive.copy(alpha = 0.25f),
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .semantics { contentDescription = "Scrub through the video" },
                        )
                        Text(
                            formatDuration(lengthMs.toLong()),
                            style = MaterialTheme.typography.labelMedium,
                            color = YapperTokens.OnImmersive.copy(alpha = 0.8f),
                        )
                        IconButton(
                            onClick = {
                                muted = !muted
                                val v = if (muted) 0f else 1f
                                player?.setVolume(v, v)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = YapperTokens.OnImmersive),
                            modifier = Modifier.size(YapperTokens.MinTouch),
                        ) {
                            Icon(
                                if (muted) YapperIcons.VolumeOff else YapperIcons.VolumeOn,
                                contentDescription = if (muted) "Unmute" else "Mute",
                            )
                        }
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
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSave(video)
                        },
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
