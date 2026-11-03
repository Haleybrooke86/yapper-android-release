package com.yapper.android.media

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.yapper.android.model.SelectionDraft
import com.yapper.android.model.StagedClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * Manages photo picker ingest and bounded-memory streaming staging for the Yapper Android Lab.
 * Conforms to Scoped Storage and Android 13+ Photo Picker contracts.
 */
class PhotoPickerManager(private val context: Context) {

    private val stagingDir: File by lazy {
        File(context.cacheDir, "staging").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Takes an ordered list of selected URIs from PickMultipleVisualMedia,
     * streams each into durable staging storage with bounded-memory streaming (64KB chunks),
     * extracts media metadata, and returns a verified SelectionDraft.
     */
    suspend fun stageSelectedUris(
        uris: List<Uri>,
        onProgress: (currentIndex: Int, totalCount: Int, percent: Float) -> Unit = { _, _, _ -> }
    ): SelectionDraft = withContext(Dispatchers.IO) {
        val stagedClips = mutableListOf<StagedClip>()
        val totalCount = uris.size.coerceAtMost(SelectionDraft.MAX_FIXTURE_ITEMS)

        uris.take(totalCount).forEachIndexed { index, uri ->
            val clipId = UUID.randomUUID().toString()
            val targetFile = File(stagingDir, "yap_${clipId}.mp4")
            val tempFile = File(stagingDir, "yap_${clipId}.tmp")

            try {
                // 1. Buffered streaming copy: 64KB buffer prevents OOM on multi-GB 4K clips
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(tempFile).use { outputStream ->
                        val buffer = ByteArray(64 * 1024)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }
                        outputStream.flush()
                    }
                } ?: throw IOException("Cannot open input stream for URI: $uri")

                // 2. Atomic rename from .tmp to final .mp4
                if (!tempFile.renameTo(targetFile)) {
                    throw IOException("Failed to rename staged file: ${tempFile.absolutePath}")
                }

                // 3. Extract media metadata without loading frames into memory
                val metadata = extractVideoMetadata(targetFile)

                val clip = StagedClip(
                    id = clipId,
                    originalUriString = uri.toString(),
                    stagedFilePath = targetFile.absolutePath,
                    tapIndex = index + 1,
                    byteSize = targetFile.length(),
                    durationMs = metadata.durationMs,
                    width = metadata.width,
                    height = metadata.height,
                    isHdr = metadata.isHdr,
                    mimeType = metadata.mimeType
                )
                stagedClips.add(clip)

                onProgress(index + 1, totalCount, ((index + 1).toFloat() / totalCount.toFloat()) * 100f)
            } catch (e: Exception) {
                tempFile.delete()
                targetFile.delete()
                throw e
            }
        }

        SelectionDraft(
            clips = stagedClips,
            isFullyPrepared = stagedClips.size == totalCount && stagedClips.all { it.isStagedValid }
        )
    }

    /**
     * Inspects video tracks to detect 10-bit color profile and HDR metadata (HLG, HDR10).
     */
    private fun extractVideoMetadata(file: File): VideoMetadata {
        val retriever = MediaMetadataRetriever()
        var durationMs = 0L
        var width = 0
        var height = 0
        var mimeType = "video/mp4"
        var isHdr = false

        try {
            retriever.setDataSource(file.absolutePath)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "video/mp4"

            // Check HDR profile using MediaExtractor
            val extractor = MediaExtractor()
            extractor.setDataSource(file.absolutePath)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val trackMime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (trackMime.startsWith("video/")) {
                    if (format.containsKey(MediaFormat.KEY_COLOR_STANDARD)) {
                        val colorStandard = format.getInteger(MediaFormat.KEY_COLOR_STANDARD)
                        if (colorStandard == MediaFormat.COLOR_STANDARD_BT2020) {
                            isHdr = true
                        }
                    }
                    if (format.containsKey(MediaFormat.KEY_COLOR_TRANSFER)) {
                        val transfer = format.getInteger(MediaFormat.KEY_COLOR_TRANSFER)
                        if (transfer == MediaFormat.COLOR_TRANSFER_HLG || transfer == MediaFormat.COLOR_TRANSFER_ST2084) {
                            isHdr = true
                        }
                    }
                    break
                }
            }
            extractor.release()
        } catch (_: Exception) {
            // Fallback gracefully on retriever errors
        } finally {
            retriever.release()
        }

        return VideoMetadata(durationMs, width, height, mimeType, isHdr)
    }

    /**
     * Purges obsolete or abandoned staged files.
     */
    fun clearStaging() {
        stagingDir.listFiles()?.forEach { it.delete() }
    }

    data class VideoMetadata(
        val durationMs: Long,
        val width: Int,
        val height: Int,
        val mimeType: String,
        val isHdr: Boolean
    )
}
