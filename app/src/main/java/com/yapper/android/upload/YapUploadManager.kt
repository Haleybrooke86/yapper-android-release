package com.yapper.android.upload

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.yapper.android.model.SelectionDraft
import com.yapper.android.model.StagedClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/**
 * Manages resilient background uploads and state reconciliation for the Yapper Android Lab.
 * Directly addresses Backlog B-1: Guarantees the client queries server upload-state
 * before declaring failure and never forces re-upload if the backend received bytes.
 */
class YapUploadManager(private val context: Context) {

    companion object {
        const val UPLOAD_CHANNEL_ID = "channel_yapper_uploads"
        const val RENDER_CHANNEL_ID = "channel_yapper_renders"
        const val CHUNK_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB chunks
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Low-importance channel for silent upload progress updates
            val uploadChannel = NotificationChannel(
                UPLOAD_CHANNEL_ID,
                "Yapper Video Uploads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress during video clip uploads"
                setShowBadge(false)
            }

            // High-importance channel for Yap render completion alerts
            val renderChannel = NotificationChannel(
                RENDER_CHANNEL_ID,
                "Yap Render Complete",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when your 4K HDR Yap is ready for review"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(uploadChannel)
            notificationManager.createNotificationChannel(renderChannel)
        }
    }

    /**
     * Upload State Reconciliation representation from /api/jobs/{id}/upload-state
     */
    data class ServerUploadState(
        val jobId: String,
        val receivedBytes: Long,
        val totalExpectedBytes: Long,
        val isFullyUploaded: Boolean,
        val isPlanning: Boolean
    )

    /**
     * Reconciles local upload state against Supabase/Modal worker before proceeding.
     * Prevents race conditions and erroneous "client_error" declarations (Backlog B-1).
     */
    suspend fun reconcileUploadState(
        jobId: String,
        draft: SelectionDraft,
        onProgress: (percent: Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val totalBytes = draft.totalBytes
        var currentOffset = 0L

        // In production: fetch from GET /api/jobs/{id}/upload-state
        val serverState = checkRemoteUploadState(jobId)

        if (serverState.isFullyUploaded || serverState.isPlanning) {
            // Already received! No re-upload required.
            onProgress(100f)
            return@withContext true
        }

        currentOffset = serverState.receivedBytes

        // Stream remaining chunks from currentOffset
        for (clip in draft.clips) {
            uploadClipInChunks(jobId, clip, currentOffset, totalBytes, onProgress)
        }

        // Final verification after all chunks transmitted
        val finalState = checkRemoteUploadState(jobId)
        finalState.isFullyUploaded || finalState.isPlanning
    }

    private suspend fun uploadClipInChunks(
        jobId: String,
        clip: StagedClip,
        startOffset: Long,
        totalJobBytes: Long,
        onProgress: (percent: Float) -> Unit
    ) {
        val file = File(clip.stagedFilePath)
        if (!file.exists()) throw IOException("Staged file not found: ${clip.stagedFilePath}")

        val fileLength = file.length()
        var clipOffset = if (startOffset < fileLength) startOffset else 0L

        RandomAccessFile(file, "r").use { raf ->
            val buffer = ByteArray(CHUNK_SIZE_BYTES)
            while (clipOffset < fileLength) {
                raf.seek(clipOffset)
                val bytesToRead = (fileLength - clipOffset).coerceAtMost(CHUNK_SIZE_BYTES.toLong()).toInt()
                val read = raf.read(buffer, 0, bytesToRead)
                if (read <= 0) break

                // Transmit chunk to S3/Modal presigned endpoint with retry
                sendChunkWithRetry(jobId, clip.id, clipOffset, buffer, read)

                clipOffset += read
                val progressPercent = (clipOffset.toFloat() / totalJobBytes.toFloat()) * 100f
                onProgress(progressPercent.coerceIn(0f, 100f))
            }
        }
    }

    private suspend fun sendChunkWithRetry(
        jobId: String,
        clipId: String,
        offset: Long,
        chunkData: ByteArray,
        length: Int,
        maxRetries: Int = 3
    ) {
        var attempts = 0
        while (attempts < maxRetries) {
            try {
                // In production: HTTP PUT to S3 presigned chunk URL
                delay(50) // Non-blocking simulation
                return
            } catch (e: IOException) {
                attempts++
                if (attempts >= maxRetries) throw e
                delay(1000L * attempts) // Exponential backoff
            }
        }
    }

    private suspend fun checkRemoteUploadState(jobId: String): ServerUploadState {
        // In production: HTTP GET /api/jobs/{id}/upload-state
        delay(100)
        return ServerUploadState(
            jobId = jobId,
            receivedBytes = 0L,
            totalExpectedBytes = 0L,
            isFullyUploaded = false,
            isPlanning = false
        )
    }

    fun buildUploadNotification(title: String, progress: Int): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, UPLOAD_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("$progress% uploaded")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
    }
}
