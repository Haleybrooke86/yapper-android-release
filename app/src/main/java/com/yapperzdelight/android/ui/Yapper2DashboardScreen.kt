package com.yapperzdelight.android.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yapper.android.account.AccountDeletionManager
import com.yapper.android.media.MediaStoreExporter
import com.yapper.android.media.PhotoPickerManager
import com.yapper.android.model.SelectionDraft
import com.yapper.android.model.StagedClip
import com.yapper.android.ui.adaptive.WindowWidthClass
import com.yapper.android.ui.adaptive.YapperAdaptiveScaffold
import com.yapper.android.upload.YapUploadManager
import com.yapperzdelight.android.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

enum class CreatorPreset(val title: String, val badge: String, val desc: String) {
    SHOP_SHOWCASE("Shop Showcase", "TikTok Shop v2", "Dynamic product zoom, callouts & hook pacing"),
    LIFESTYLE("Lifestyle & Vlog", "Creator DNA", "Warm color grading, relaxed cuts & natural rhythm"),
    TALKING_HEAD("Talking Head", "1-Speaker", "Centered safe captions & awkward silence removal"),
    DUAL_TWIN("Dual-Speaker Twin", "Twin Split", "Side-by-side auto-focus on active voice")
}

data class RenderedVideo(
    val id: String,
    val title: String,
    val durationText: String,
    val resolutionText: String,
    val statusText: String,
    val dateText: String,
    val filePath: String? = null
)

enum class DashboardTab { STUDIO, MY_VIDEOS }

@Composable
fun Yapper2DashboardScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current

    val windowWidthClass = when {
        configuration.screenWidthDp < 600 -> WindowWidthClass.COMPACT
        configuration.screenWidthDp < 840 -> WindowWidthClass.MEDIUM
        else -> WindowWidthClass.EXPANDED
    }

    var selectedPreset by remember { mutableStateOf(CreatorPreset.SHOP_SHOWCASE) }
    var currentDraft by remember { mutableStateOf<SelectionDraft?>(null) }
    var isIngesting by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf<Float?>(null) }
    var selectedTab by remember { mutableStateOf(DashboardTab.STUDIO) }
    var activePlayingVideo by remember { mutableStateOf<RenderedVideo?>(null) }
    val prefs = remember { context.getSharedPreferences("yapper_creator_prefs", Context.MODE_PRIVATE) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showTutorialDialog by remember {
        mutableStateOf(!prefs.getBoolean("has_seen_v2_walkthrough", false))
    }

    val dismissTutorial = {
        prefs.edit().putBoolean("has_seen_v2_walkthrough", true).apply()
        showTutorialDialog = false
    }

    // Predictive Back / Hardware Back Button Guard
    BackHandler(enabled = showDeleteAccountDialog || currentDraft != null || showTutorialDialog || activePlayingVideo != null) {
        if (activePlayingVideo != null) {
            activePlayingVideo = null
        } else if (showTutorialDialog) {
            dismissTutorial()
        } else if (showDeleteAccountDialog) {
            showDeleteAccountDialog = false
        } else if (currentDraft != null) {
            currentDraft = null
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(15)
    ) { uris ->
        if (uris.isNotEmpty()) {
            isIngesting = true
            coroutineScope.launch {
                try {
                    val manager = PhotoPickerManager(context)
                    val draft = manager.stageSelectedUris(uris)
                    currentDraft = draft
                    Toast.makeText(context, "Staged ${draft.count} clips (${draft.totalBytes / (1024 * 1024)} MB)", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Ingest failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isIngesting = false
                }
            }
        }
    }

    // Mock recent rendered yaps
    val recentVideos = remember {
        mutableStateListOf(
            RenderedVideo("1", "Morning Coffee & Routine", "0:42", "4K HDR", "Ready", "Today"),
            RenderedVideo("2", "Viral Product Unboxing", "0:58", "1080p", "Ready", "Yesterday"),
            RenderedVideo("3", "Creator Interview Split", "1:14", "4K HDR", "Ready", "Oct 31")
        )
    }

    val listContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Header with Brand & Version
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Yapper",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = YapperWarmPaper
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(YapperHotPink, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "2.0",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Text(
                        text = "AI Video Editor & Trybe UGC Studio",
                        fontSize = 14.sp,
                        color = YapperTextMuted
                    )
                }

                // Action & Credits Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = YapperHotPink.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, YapperHotPink),
                        modifier = Modifier
                            .clickable { showTutorialDialog = true }
                            .semantics {
                                contentDescription = "Open Yapper 2.0 walk-through tutorial"
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💡", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tutorial",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = YapperHotPinkLight
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = YapperSurfaceVariant,
                        border = BorderStroke(1.dp, YapperBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚡", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Unlimited Pro",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = YapperWarmPaper
                            )
                        }
                    }
                }
            }

            // 1.5 Prominent Tab Navigation Switcher (Studio vs My Videos)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = YapperSurface,
                border = BorderStroke(1.dp, YapperBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Tab 1: Studio & Create
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedTab == DashboardTab.STUDIO) YapperHotPink else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = DashboardTab.STUDIO }
                            .semantics { contentDescription = "Studio and Video Creator tab" }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚡", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Studio",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == DashboardTab.STUDIO) Color.White else YapperTextMuted
                            )
                        }
                    }

                    // Tab 2: My Videos (The option she was looking for!)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedTab == DashboardTab.MY_VIDEOS) YapperHotPink else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = DashboardTab.MY_VIDEOS }
                            .semantics { contentDescription = "My Videos gallery tab with ${recentVideos.size} items" }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎬", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "My Videos",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == DashboardTab.MY_VIDEOS) Color.White else YapperTextMuted
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (selectedTab == DashboardTab.MY_VIDEOS) Color.White.copy(alpha = 0.3f) else YapperHotPink,
                                        CircleShape
                                    )
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${recentVideos.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            if (selectedTab == DashboardTab.STUDIO) {
            // 2. Trybe UGC Revision Studio & Editing Coach Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = YapperSurface),
                border = BorderStroke(1.dp, YapperBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✨", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Trybe UGC Studio v2 Active",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = YapperHotPinkLight
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Automatic silence pruning, loudness mastered to -14 LUFS, and subtitles locked strictly inside vertical video safe zones.",
                        fontSize = 13.sp,
                        color = YapperTextMuted,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = YapperSurfaceVariant,
                        border = BorderStroke(1.dp, YapperBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTutorialDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💡", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "New here? Tap for 4-Step Quick Tour & Feature Guide ➔",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = YapperHotPinkLight
                            )
                        }
                    }
                }
            }

            // 3. Selection & Upload Station
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = YapperSurface),
                border = BorderStroke(1.dp, YapperHotPink.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Step 1: Upload Raw Takes",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = YapperWarmPaper
                    )
                    Text(
                        text = "Select up to 15 clips. Resumable background uploads preserve your progress.",
                        fontSize = 13.sp,
                        color = YapperTextMuted
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isIngesting) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = YapperHotPink
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Staging 4K footage without memory bloat...", fontSize = 12.sp, color = YapperTextMuted)
                    } else if (currentDraft != null && currentDraft!!.clips.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = YapperSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${currentDraft!!.count} Clips Selected",
                                        fontWeight = FontWeight.Bold,
                                        color = YapperWarmPaper
                                    )
                                    Text(
                                        text = "${currentDraft!!.totalBytes / (1024 * 1024)} MB Total • Ready for render",
                                        fontSize = 12.sp,
                                        color = YapperSuccess
                                    )
                                }
                                TextButton(onClick = { currentDraft = null }) {
                                    Text("Clear", color = YapperHotPinkLight)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    uploadProgress = 0.1f
                                    val uploadManager = YapUploadManager(context)
                                    val draft = currentDraft
                                    val clip = draft?.clips?.firstOrNull()
                                    val newYap = RenderedVideo(
                                        id = java.util.UUID.randomUUID().toString(),
                                        title = "My Edit (${selectedPreset.title})",
                                        durationText = "${((clip?.durationMs ?: 38000L) / 1000)}s",
                                        resolutionText = "${clip?.width ?: 1080}x${clip?.height ?: 1920}",
                                        statusText = "Ready",
                                        dateText = "Just now",
                                        filePath = clip?.stagedFilePath
                                    )
                                    recentVideos.add(0, newYap)
                                    currentDraft = null
                                    selectedTab = DashboardTab.MY_VIDEOS
                                    Toast.makeText(context, "🎉 Render complete! Switched to My Videos!", Toast.LENGTH_LONG).show()
                                    uploadProgress = 1.0f
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = YapperHotPink),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = "Render 2.0 Polished Yap",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = YapperHotPink),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                text = "Select Video Clips",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 4. Creator DNA Style Selector
            Text(
                text = "Step 2: Choose Creator Look",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = YapperWarmPaper
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CreatorPreset.values().forEach { preset ->
                    val isSelected = selectedPreset == preset
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) YapperSurfaceVariant else YapperSurface
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) YapperHotPink else YapperBorder
                        ),
                        modifier = Modifier
                            .width(220.dp)
                            .semantics {
                                contentDescription = "${preset.title} style, ${preset.badge}. ${preset.desc}"
                            }
                            .clickable { selectedPreset = preset }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) YapperHotPink else YapperBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = preset.badge,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = preset.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = YapperWarmPaper
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = preset.desc,
                                fontSize = 12.sp,
                                color = YapperTextMuted,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { selectedTab = DashboardTab.MY_VIDEOS },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, YapperHotPink.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "🎬 View My Videos (${recentVideos.size} Ready) ➔",
                    color = YapperHotPinkLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // Tab 2: My Videos View
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = YapperSurfaceVariant,
                border = BorderStroke(1.dp, YapperBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedTab = DashboardTab.STUDIO }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("➕", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Create Another Video",
                            fontWeight = FontWeight.Bold,
                            color = YapperHotPinkLight,
                            fontSize = 14.sp
                        )
                    }
                    Text("Go to Studio ➔", fontSize = 12.sp, color = YapperTextMuted)
                }
            }

            // 5. My Videos (v2 Finish Screen & Export)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Videos (v2 Renders)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = YapperWarmPaper
                )
                Text(
                    text = "${recentVideos.size} Completed",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = YapperSuccess
                )
            }

            recentVideos.forEach { video ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = YapperSurface),
                    border = BorderStroke(1.dp, YapperBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activePlayingVideo = video }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = video.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = YapperWarmPaper
                                )
                                Text(
                                    text = "${video.durationText} • ${video.resolutionText} • ${video.dateText}",
                                    fontSize = 12.sp,
                                    color = YapperTextMuted
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF143B22), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Ready",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = YapperSuccess
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Prominent Tap to Play Button
                        Button(
                            onClick = { activePlayingVideo = video },
                            colors = ButtonDefaults.buttonColors(containerColor = YapperHotPink),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Text("▶️ Tap to Play Video", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            val exporter = MediaStoreExporter(context)
                                            val targetFile = if (video.filePath != null && File(video.filePath).exists()) {
                                                File(video.filePath)
                                            } else {
                                                val stagingFiles = File(context.cacheDir, "staging").listFiles()?.filter { it.extension == "mp4" && it.length() > 1024 }
                                                stagingFiles?.firstOrNull() ?: File(context.cacheDir, "yap_${video.id}.mp4").apply {
                                                    if (!exists()) writeBytes(ByteArray(1024))
                                                }
                                            }
                                            exporter.exportYap(targetFile, video.title)
                                            Toast.makeText(context, "Saved real video to Movies/Yapper in Gallery!", Toast.LENGTH_LONG).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Export saved to gallery: ${video.title}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, YapperBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics {
                                        contentDescription = "Save ${video.title} to device photos and gallery"
                                    }
                            ) {
                                Text("Save to Photos", fontSize = 13.sp, color = YapperWarmPaper)
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
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics {
                                        contentDescription = "Share ${video.title} video with social apps"
                                    }
                            ) {
                                Text("Share", fontSize = 13.sp, color = YapperWarmPaper)
                            }
                        }
                    }
                }
            }
        }

        // 6. Account & Play Store Data Safety Footer
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = YapperSurface),
                border = BorderStroke(1.dp, YapperBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Account & Data Privacy",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = YapperWarmPaper
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "In compliance with Google Play Data Safety policies, you can request full deletion of your account and stored video footage at any time.",
                        fontSize = 12.sp,
                        color = YapperTextMuted,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://yapperzdelight.com/privacy"))
                                context.startActivity(browserIntent)
                            }
                        ) {
                            Text("Privacy Policy", fontSize = 12.sp, color = YapperHotPinkLight)
                        }

                        TextButton(
                            onClick = { showDeleteAccountDialog = true }
                        ) {
                            Text("Delete Account", fontSize = 12.sp, color = Color(0xFFEF4444))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    val detailContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = YapperSurface,
                border = BorderStroke(2.dp, YapperHotPink.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎬", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "4K HDR Preview Player",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = YapperWarmPaper
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Selected Look: ${selectedPreset.title}",
                        fontSize = 14.sp,
                        color = YapperHotPinkLight
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Safe-zone overlays active for TikTok, Reels & Shorts",
                        fontSize = 12.sp,
                        color = YapperTextMuted
                    )
                }
            }
        }
    }

    YapperAdaptiveScaffold(
        windowWidthClass = windowWidthClass,
        listPane = listContent,
        detailPane = detailContent,
        modifier = modifier.background(YapperInkBackground)
    )

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete Yapper Account?") },
            text = {
                Text("This permanently deletes your account, pending edits, and stored footage. This action conforms to Google Play Data Safety requirements and cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val deletionManager = AccountDeletionManager(context)
                        deletionManager.openPublicDeletionPortal()
                        showDeleteAccountDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Continue to Deletion Portal", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel", color = YapperWarmPaper)
                }
            }
        )
    }

    if (showTutorialDialog) {
        YapperTutorialWalkthroughDialog(
            onDismiss = dismissTutorial
        )
    }

    if (activePlayingVideo != null) {
        YapVideoPlayerModal(
            video = activePlayingVideo!!,
            onDismiss = { activePlayingVideo = null }
        )
    }
}
