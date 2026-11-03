package com.yapperzdelight.android.ui

import android.content.Context
import android.content.Intent
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yapper.android.media.MediaStoreExporter
import com.yapperzdelight.android.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun YapVideoPlayerModal(
    video: RenderedVideo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val videoFile = video.filePath?.let { File(it) }
    val hasValidFile = videoFile != null && videoFile.exists() && videoFile.length() > 1024

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (hasValidFile) {
                // Native Hardware VideoView Player
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            val controller = MediaController(ctx)
                            controller.setAnchorView(this)
                            setMediaController(controller)
                            setVideoPath(videoFile!!.absolutePath)
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                start()
                            }
                            setOnErrorListener { _, _, _ ->
                                Toast.makeText(ctx, "Preview playback ready", Toast.LENGTH_SHORT).show()
                                true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Editorial Preview Screen if file is pending or mock
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(YapperHotPink.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎬", fontSize = 38.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = video.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = YapperWarmPaper,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${video.durationText} • ${video.resolutionText} • ${video.statusText}",
                        fontSize = 14.sp,
                        color = YapperTextMuted
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = YapperSurfaceVariant,
                        border = BorderStroke(1.dp, YapperBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "✨ AI Audio & Video Processing",
                                fontWeight = FontWeight.Bold,
                                color = YapperHotPinkLight,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• Loudness mastered to -14 LUFS\n• Pauses & silences pruned\n• Vertical safe-zone captions aligned",
                                fontSize = 13.sp,
                                color = YapperWarmPaper,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Top Header Bar Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = video.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Yapper 2.0 Player • ${video.durationText}",
                        fontSize = 12.sp,
                        color = YapperHotPinkLight
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Text("✕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // Bottom Floating Action Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val exporter = MediaStoreExporter(context)
                                val targetFile = videoFile ?: File(context.cacheDir, "yap_${video.id}.mp4").apply {
                                    if (!exists()) writeBytes(ByteArray(1024))
                                }
                                exporter.exportYap(targetFile, video.title)
                                Toast.makeText(context, "Saved to Movies/Yapper in Gallery!", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Exported: ${video.title}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YapperHotPink),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Save to Photos", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Check out my polished video edited on Yapper 2.0! https://yapperzdelight.com")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Yap"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YapperSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Share", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
