package com.yapper.android.media

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Handles exporting finished, rendered Yaps back to the creator's device gallery.
 * Conforms to Scoped Storage guidelines with zero permissions required on Android 10+.
 */
class MediaStoreExporter(private val context: Context) {

    /**
     * Exports a rendered MP4 file to Movies/Yapper.
     * Marks IS_PENDING = 1 during write to prevent media players from reading incomplete files.
     */
    suspend fun exportYap(sourceFile: File, displayName: String): Uri = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "$displayName.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Yapper")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val itemUri = resolver.insert(collectionUri, contentValues)
            ?: throw IOException("Failed to create MediaStore video entry")

        try {
            resolver.openOutputStream(itemUri)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
                outputStream.flush()
            } ?: throw IOException("Failed to open output stream for MediaStore URI: $itemUri")

            // Atomic commit: Clear IS_PENDING
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)
            }

            // Force index scan so Google Photos and Samsung Gallery display it immediately
            MediaScannerConnection.scanFile(
                context,
                arrayOf(sourceFile.absolutePath),
                arrayOf("video/mp4"),
                null
            )

            itemUri
        } catch (e: Exception) {
            // Cleanup incomplete entry on failure
            resolver.delete(itemUri, null, null)
            throw e
        }
    }
}
