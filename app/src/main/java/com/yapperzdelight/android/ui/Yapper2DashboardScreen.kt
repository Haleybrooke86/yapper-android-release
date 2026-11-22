package com.yapperzdelight.android.ui

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yapper.android.account.AccountDeletionManager
import com.yapper.android.media.PhotoPickerManager
import com.yapper.android.model.SelectionDraft
import com.yapperzdelight.android.data.LocalYap
import com.yapperzdelight.android.data.LocalYapLibrary
import com.yapperzdelight.android.ui.theme.YapperIcons
import com.yapperzdelight.android.ui.theme.YapperTheme
import com.yapperzdelight.android.ui.theme.YapperTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * The four looks are the web studio's real journeys (src/lib/journeys.ts), with the same names and lines,
 * so a look picked here opens the matching studio setup at yapperzdelight.com/app/new.
 */
enum class CreatorLook(
    val title: String,
    val line: String,
    val studioQuery: String,
    val cues: List<String>,
) {
    STORYTIME(
        "Storytime",
        "Keep them listening.",
        "journey=storytime",
        listOf(
            "Say your best line first, then tell it like you would to a friend.",
            "Phone at eye level, about an arm's length away.",
            "Pause whenever you need to. The gaps get trimmed.",
        ),
    ),
    LIFESTYLE(
        "Lifestyle",
        "Make the everyday a moment.",
        "journey=lifestyle",
        listOf(
            "Face a window for soft, even light.",
            "Grab a few quiet moments without talking.",
            "Keep each clip short and let it breathe.",
        ),
    ),
    TIKTOK_SHOP(
        "TikTok Shop",
        "Your story. Product as the payoff.",
        "journey=tiktok_shop",
        listOf(
            "Hold the product close to the lens for your first line.",
            "Show the problem it fixes, then show it working.",
            "Skip prices and links. Talk about how it feels to use.",
        ),
    ),
    JUST_EDIT(
        "Just edit it",
        "No format. Give me the mess.",
        "fresh=1",
        listOf(
            "Good light and a steady phone do most of the work.",
            "Talk naturally. Keep the stumbles, the good parts get found.",
            "Up to 15 clips, in any order you like.",
        ),
    ),
}

enum class DashboardTab(val label: String, val icon: ImageVector) {
    NEW_YAP("New yap", YapperIcons.NewYap),
    MY_YAPS("My yaps", YapperIcons.MyYaps),
}

private const val PREFS = "yapper_creator_prefs"
private const val KEY_TOUR_SEEN = "has_seen_v3_tour"

@Composable
fun Yapper2DashboardScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val c = YapperTheme.colors
    val snackbar = remember { SnackbarHostState() }
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val library = remember { LocalYapLibrary(context) }
    val saveToPhotos = rememberSaveToPhotos()

    var lookName by rememberSaveable { mutableStateOf(CreatorLook.STORYTIME.name) }
    val look = CreatorLook.valueOf(lookName)
    var tabName by rememberSaveable { mutableStateOf(DashboardTab.NEW_YAP.name) }
    val tab = DashboardTab.valueOf(tabName)
    var draft by remember { mutableStateOf<SelectionDraft?>(null) }
    var ingesting by remember { mutableStateOf(false) }
    var ingestProgress by remember { mutableFloatStateOf(0f) }
    var ingestLabel by remember { mutableStateOf("") }
    val kept = remember { mutableStateListOf<LocalYap>() }
    var playing by remember { mutableStateOf<LocalYap?>(null) }
    var confirmRemove by remember { mutableStateOf<LocalYap?>(null) }
    var showDeleteAccount by remember { mutableStateOf(false) }
    var showTour by remember { mutableStateOf(false) }
    var tourSeen by remember { mutableStateOf(prefs.getBoolean(KEY_TOUR_SEEN, false)) }

    fun openTour() {
        showTour = true
        if (!tourSeen) {
            prefs.edit().putBoolean(KEY_TOUR_SEEN, true).apply()
            tourSeen = true
        }
    }

    fun say(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    LaunchedEffect(Unit) {
        kept.clear()
        kept.addAll(library.load())
    }

    BackHandler(enabled = tab == DashboardTab.MY_YAPS) { tabName = DashboardTab.NEW_YAP.name }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(SelectionDraft.MAX_FIXTURE_ITEMS)) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        ingesting = true
        ingestProgress = 0f
        ingestLabel = "Getting your clips ready"
        scope.launch {
            try {
                draft = PhotoPickerManager(context).stageSelectedUris(uris) { done, total, percent ->
                    ingestProgress = percent / 100f
                    ingestLabel = "Getting clip $done of $total ready"
                }
            } catch (_: Exception) {
                say("Some clips could not be added. Try picking them again.")
            } finally {
                ingesting = false
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = c.background,
        contentColor = c.ink,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column {
                HorizontalDivider(color = c.line, thickness = 1.dp)
                NavigationBar(containerColor = c.card, tonalElevation = 0.dp) {
                    DashboardTab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tabName = t.name },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(t.label, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = c.accent,
                                selectedTextColor = c.accent,
                                indicatorColor = c.petal,
                                unselectedIconColor = c.muted,
                                unselectedTextColor = c.muted,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = YapperTokens.Space.page, vertical = YapperTokens.Space.lg),
            ) {
                Header(onHelp = ::openTour)

                when (tab) {
                    DashboardTab.NEW_YAP -> StudioTab(
                        look = look,
                        onLook = { lookName = it.name },
                        showTourHint = !tourSeen,
                        onTour = ::openTour,
                        draft = draft,
                        ingesting = ingesting,
                        ingestProgress = ingestProgress,
                        ingestLabel = ingestLabel,
                        onChooseClips = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        },
                        onClear = {
                            PhotoPickerManager(context).clearStaging()
                            draft = null
                        },
                        onEditInStudio = {
                            if (!openStudio(context, "/app/new?${look.studioQuery}")) {
                                say("No browser found. Visit yapperzdelight.com to edit your yap.")
                            }
                        },
                        onKeep = {
                            val current = draft ?: return@StudioTab
                            scope.launch {
                                val next = library.keep(current, look.title)
                                kept.clear()
                                kept.addAll(next)
                                draft = null
                                tabName = DashboardTab.MY_YAPS.name
                                say("Kept on this phone. These are your original clips, ready to edit any time.")
                            }
                        },
                        onPrivacy = {
                            if (!openStudio(context, "/privacy")) say("No browser found. Visit yapperzdelight.com/privacy.")
                        },
                        onDeleteAccount = { showDeleteAccount = true },
                    )

                    DashboardTab.MY_YAPS -> LibraryTab(
                        items = kept,
                        onPlay = { playing = it },
                        onSave = saveToPhotos,
                        onShare = { yap ->
                            if (!shareVideoFile(context, yap.file, yap.title)) {
                                say("This clip is no longer on your phone, so it cannot be shared.")
                            }
                        },
                        onRemove = { confirmRemove = it },
                        onOpenStudioYaps = {
                            if (!openStudio(context, "/app/yaps")) say("No browser found. Visit yapperzdelight.com to see your yaps.")
                        },
                        onAddClips = { tabName = DashboardTab.NEW_YAP.name },
                    )
                }
                Spacer(Modifier.height(YapperTokens.Space.xxl))
            }
        }
    }

    confirmRemove?.let { yap ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            containerColor = c.card,
            shape = YapperTokens.Radius.card,
            title = { Text("Remove this clip?", style = MaterialTheme.typography.headlineSmall, color = c.ink) },
            text = {
                Text(
                    "It is removed from Yapperz Delight on this phone. The original stays in your gallery.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.muted,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmRemove = null
                        scope.launch {
                            val next = library.remove(yap)
                            kept.clear()
                            kept.addAll(next)
                        }
                    },
                    shape = YapperTokens.Radius.pill,
                    colors = ButtonDefaults.buttonColors(containerColor = c.danger, contentColor = if (c.isDark) c.dangerSoft else Color.White),
                ) { Text("Remove", style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = null }) {
                    Text("Keep it", style = MaterialTheme.typography.labelLarge, color = c.ink)
                }
            },
        )
    }

    if (showDeleteAccount) {
        AlertDialog(
            onDismissRequest = { showDeleteAccount = false },
            containerColor = c.card,
            shape = YapperTokens.Radius.card,
            title = { Text("Delete your account?", style = MaterialTheme.typography.headlineSmall, color = c.ink) },
            text = {
                Text(
                    "We will open the account deletion page, where you can permanently delete your account and every video you have uploaded. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.muted,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccount = false
                        AccountDeletionManager(context).openPublicDeletionPortal()
                    },
                    shape = YapperTokens.Radius.pill,
                    colors = ButtonDefaults.buttonColors(containerColor = c.danger, contentColor = if (c.isDark) c.dangerSoft else Color.White),
                ) { Text("Continue", style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccount = false }) {
                    Text("Cancel", style = MaterialTheme.typography.labelLarge, color = c.ink)
                }
            },
        )
    }

    if (showTour) YapperTutorialWalkthroughDialog(onDismiss = { showTour = false })

    playing?.let { yap ->
        YapVideoPlayerModal(video = yap, onDismiss = { playing = null }, onSave = saveToPhotos)
    }
}

@Composable
private fun Header(onHelp: () -> Unit) {
    val c = YapperTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        YapperWordmark(modifier = Modifier.weight(1f))
        IconButton(onClick = onHelp, modifier = Modifier.size(YapperTokens.MinTouch)) {
            Icon(YapperIcons.Help, contentDescription = "Take the 1 minute tour", tint = c.muted)
        }
    }
}

@Composable
private fun StudioTab(
    look: CreatorLook,
    onLook: (CreatorLook) -> Unit,
    showTourHint: Boolean,
    onTour: () -> Unit,
    draft: SelectionDraft?,
    ingesting: Boolean,
    ingestProgress: Float,
    ingestLabel: String,
    onChooseClips: () -> Unit,
    onClear: () -> Unit,
    onEditInStudio: () -> Unit,
    onKeep: () -> Unit,
    onPrivacy: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    val c = YapperTheme.colors
    Text("Give me the mess. I'll find the yap.", style = MaterialTheme.typography.bodyMedium, color = c.muted)
    if (showTourHint) {
        Text(
            "New here? Take the 1 minute tour",
            style = MaterialTheme.typography.labelMedium,
            color = c.accent,
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onTour)
                .heightIn(min = YapperTokens.MinTouch)
                .padding(vertical = 14.dp),
        )
    }

    Spacer(Modifier.height(if (showTourHint) 12.dp else 28.dp))
    SectionHeading(kicker = "Step 1", title = "Pick your look")
    Spacer(Modifier.height(14.dp))
    CreatorLook.entries.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            row.forEach { item ->
                LookTile(look = item, selected = item == look, onClick = { onLook(item) }, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    YapperCard(color = c.cardSubtle, contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)) {
        Kicker("Before you film")
        Spacer(Modifier.height(10.dp))
        look.cues.forEachIndexed { i, cue ->
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = if (i == 0) 0.dp else 10.dp)) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(6.dp)
                        .background(c.accent, CircleShape),
                )
                Spacer(Modifier.width(12.dp))
                Text(cue, style = MaterialTheme.typography.bodyMedium, color = c.ink)
            }
        }
    }

    Spacer(Modifier.height(32.dp))
    SectionHeading(
        kicker = "Step 2",
        title = "Add your clips",
        subtitle = "Pick up to 15 clips from your gallery. They stay on this phone until you choose what happens next.",
    )
    Spacer(Modifier.height(16.dp))

    when {
        ingesting -> YapperCard {
            LinearProgressIndicator(
                progress = { ingestProgress.coerceIn(0f, 1f) },
                color = c.accent,
                trackColor = c.petal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(YapperTokens.Radius.pill),
            )
            Spacer(Modifier.height(10.dp))
            Text("$ingestLabel\u2026", style = MaterialTheme.typography.bodySmall, color = c.muted)
        }

        draft != null && draft.clips.isNotEmpty() -> YapperCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(c.petal, YapperTokens.Radius.thumb),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(YapperIcons.Film, contentDescription = null, tint = c.accentDeep)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (draft.count == 1) "1 clip ready" else "${draft.count} clips ready",
                        style = MaterialTheme.typography.titleMedium,
                        color = c.ink,
                    )
                    Text(
                        "${formatDuration(draft.totalDurationMs)} · ${formatSize(draft.totalBytes)} · ${look.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.muted,
                    )
                }
                TextButton(onClick = onClear, modifier = Modifier.heightIn(min = YapperTokens.MinTouch)) {
                    Text("Clear", style = MaterialTheme.typography.labelMedium, color = c.accent)
                }
            }
            Spacer(Modifier.height(18.dp))
            PrimaryPillButton(
                text = "Edit in my studio",
                icon = YapperIcons.OpenExternal,
                onClick = onEditInStudio,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Opens yapperzdelight.com, where your yap is edited. Add these same clips there.",
                style = MaterialTheme.typography.bodySmall,
                color = c.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            GhostPillButton(text = "Keep on this phone", onClick = onKeep, modifier = Modifier.fillMaxWidth())
        }

        else -> PrimaryPillButton(
            text = "Choose clips",
            icon = YapperIcons.Plus,
            onClick = onChooseClips,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Spacer(Modifier.height(36.dp))
    YapperCard {
        Kicker("Your account")
        Spacer(Modifier.height(8.dp))
        Text("Your videos, your call", style = MaterialTheme.typography.titleSmall, color = c.ink)
        Spacer(Modifier.height(4.dp))
        Text(
            "You can delete your account and every video you have uploaded, any time.",
            style = MaterialTheme.typography.bodySmall,
            color = c.muted,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onPrivacy, modifier = Modifier.heightIn(min = YapperTokens.MinTouch)) {
                Text("Privacy policy", style = MaterialTheme.typography.labelMedium, color = c.accent)
            }
            TextButton(onClick = onDeleteAccount, modifier = Modifier.heightIn(min = YapperTokens.MinTouch)) {
                Text("Delete account", style = MaterialTheme.typography.labelMedium, color = c.danger)
            }
        }
    }
}

@Composable
private fun LookTile(look: CreatorLook, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = YapperTheme.colors
    Surface(
        shape = YapperTokens.Radius.tile,
        color = if (selected) c.petal else c.card,
        border = BorderStroke(if (selected) 2.dp else YapperTokens.BorderWidth, if (selected) c.accent else c.line),
        modifier = modifier
            .heightIn(min = 112.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(look.title, style = MaterialTheme.typography.titleSmall, color = c.ink, modifier = Modifier.weight(1f))
                if (selected) {
                    Icon(YapperIcons.Check, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(look.line, style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
    }
}

@Composable
private fun LibraryTab(
    items: List<LocalYap>,
    onPlay: (LocalYap) -> Unit,
    onSave: (LocalYap) -> Unit,
    onShare: (LocalYap) -> Unit,
    onRemove: (LocalYap) -> Unit,
    onOpenStudioYaps: () -> Unit,
    onAddClips: () -> Unit,
) {
    val c = YapperTheme.colors
    Spacer(Modifier.height(20.dp))
    SectionHeading(
        kicker = "On this phone",
        title = "My yaps",
        subtitle = "Clips you kept on this phone. Your finished edits live in your online studio.",
    )
    Spacer(Modifier.height(14.dp))
    GhostPillButton(text = "See my finished yaps", icon = YapperIcons.OpenExternal, onClick = onOpenStudioYaps)
    Spacer(Modifier.height(20.dp))

    if (items.isEmpty()) {
        YapperCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(28.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(c.petal, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(YapperIcons.MyYaps, contentDescription = null, tint = c.accentDeep, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Nothing kept yet", style = MaterialTheme.typography.headlineSmall, color = c.ink)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Clips you keep on this phone show up here, ready to watch, save or share.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.muted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                PrimaryPillButton(text = "Add clips", icon = YapperIcons.Plus, onClick = onAddClips)
            }
        }
        return
    }

    items.forEachIndexed { i, yap ->
        if (i > 0) Spacer(Modifier.height(12.dp))
        LibraryItem(yap = yap, onPlay = onPlay, onSave = onSave, onShare = onShare, onRemove = onRemove)
    }
}

@Composable
private fun LibraryItem(
    yap: LocalYap,
    onPlay: (LocalYap) -> Unit,
    onSave: (LocalYap) -> Unit,
    onShare: (LocalYap) -> Unit,
    onRemove: (LocalYap) -> Unit,
) {
    val c = YapperTheme.colors
    var frame by remember(yap.id) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(yap.id) { frame = loadFrame(yap) }

    YapperCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Row {
            Box(
                modifier = Modifier
                    .width(84.dp)
                    .height(150.dp)
                    .clip(YapperTokens.Radius.thumb)
                    .background(YapperTokens.Immersive)
                    .clickable(role = Role.Button, onClick = { onPlay(yap) })
                    .semantics { contentDescription = "Play ${yap.title}" },
                contentAlignment = Alignment.Center,
            ) {
                frame?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(c.accent.copy(alpha = 0.94f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(YapperIcons.Play, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(18.dp))
                }
                Text(
                    formatDuration(yap.durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.62f), YapperTokens.Radius.pill)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                Text(yap.title, style = MaterialTheme.typography.titleMedium, color = c.ink)
                Spacer(Modifier.height(6.dp))
                StatusChip(text = "Original, not edited")
                Spacer(Modifier.height(8.dp))
                Text(
                    "${yap.lookTitle} · ${dateLabel(yap.savedAtEpochMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.muted,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = { onSave(yap) }, modifier = Modifier.size(YapperTokens.MinTouch)) {
                        Icon(YapperIcons.Save, contentDescription = "Save ${yap.title} to Photos", tint = c.ink)
                    }
                    IconButton(onClick = { onShare(yap) }, modifier = Modifier.size(YapperTokens.MinTouch)) {
                        Icon(YapperIcons.Share, contentDescription = "Share ${yap.title}", tint = c.ink)
                    }
                    IconButton(onClick = { onRemove(yap) }, modifier = Modifier.size(YapperTokens.MinTouch)) {
                        Icon(YapperIcons.Trash, contentDescription = "Remove ${yap.title} from this phone", tint = c.muted)
                    }
                }
            }
        }
    }
}

private suspend fun loadFrame(yap: LocalYap): Bitmap? = withContext(Dispatchers.IO) {
    if (!yap.exists) return@withContext null
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(yap.filePath)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            retriever.getScaledFrameAtTime(500_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 270, 480)
        } else {
            retriever.getFrameAtTime(500_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }
    } catch (_: Exception) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}

private fun formatSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) "%.1f GB".format(mb / 1024.0) else "%.0f MB".format(mb.coerceAtLeast(1.0))
}

private fun dateLabel(epochMs: Long): String {
    val then = Calendar.getInstance().apply { timeInMillis = epochMs }
    val now = Calendar.getInstance()
    fun sameDay(a: Calendar, b: Calendar) = a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    if (sameDay(then, now)) return "Today"
    now.add(Calendar.DAY_OF_YEAR, -1)
    if (sameDay(then, now)) return "Yesterday"
    return SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMs))
}
