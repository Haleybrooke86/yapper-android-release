package com.yapperzdelight.android.ui

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.yapper.android.media.MediaStoreExporter
import com.yapperzdelight.android.data.LocalYap
import com.yapperzdelight.android.ui.theme.YapperTokens
import kotlinx.coroutines.launch
import java.io.File

/** Opens a page of the live studio in the browser. Returns false when no browser can open it. */
fun openStudio(context: Context, path: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(YapperTokens.STUDIO_ORIGIN + path))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return runCatching { context.startActivity(intent); true }.getOrDefault(false)
}

/**
 * Shares the real video file (video/mp4 through FileProvider), never just a link pretending to be the video.
 * Returns false when the file is missing so the caller can say so honestly.
 */
fun shareVideoFile(context: Context, file: File, title: String): Boolean {
    if (!file.exists() || file.length() == 0L) return false
    return runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, title)
            clipData = ClipData.newRawUri(title, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(send, "Share your video").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
        true
    }.getOrDefault(false)
}

/**
 * Save to Photos with honest results only. Android 9 and older need the storage permission first,
 * so it is asked for right when she taps Save, and a refusal is explained, not hidden.
 */
@Composable
fun rememberSaveToPhotos(): (LocalYap) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<LocalYap?>(null) }

    fun export(yap: LocalYap) {
        if (!yap.exists) {
            Toast.makeText(context, "This video is no longer on your phone, so there is nothing to save.", Toast.LENGTH_LONG).show()
            return
        }
        scope.launch {
            val ok = runCatching { MediaStoreExporter(context).exportYap(yap.file, "Yapperz Delight ${yap.title}") }.isSuccess
            val message = if (ok) {
                "Saved to your Photos, in Movies/Yapper."
            } else {
                "That did not save. Check your phone has free space, then try again."
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val yap = pending
        pending = null
        if (yap == null) return@rememberLauncherForActivityResult
        if (granted) {
            export(yap)
        } else {
            Toast.makeText(context, "To save to Photos, allow storage access for Yapperz Delight in Settings.", Toast.LENGTH_LONG).show()
        }
    }

    return remember(context) {
        { yap: LocalYap ->
            val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            if (needsPermission) {
                pending = yap
                permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                export(yap)
            }
        }
    }
}
