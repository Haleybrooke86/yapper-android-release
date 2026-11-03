package com.yapper.android.model

import kotlinx.serialization.Serializable
import java.io.File
import java.util.UUID

/**
 * Represents a single video clip selected by the creator from the Android Photo Picker.
 * Preserves exact tap order, original metadata, and staged file path with zero RAM bloat.
 */
@Serializable
data class StagedClip(
    val id: String = UUID.randomUUID().toString(),
    val originalUriString: String,
    val stagedFilePath: String,
    val tapIndex: Int,
    val byteSize: Long,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val isHdr: Boolean = false,
    val mimeType: String = "video/mp4"
) {
    val stagedFile: File get() = File(stagedFilePath)
    val isStagedValid: Boolean get() = stagedFile.exists() && stagedFile.length() == byteSize && byteSize > 0
}

/**
 * Immutable draft state representing up to 15 ordered clips ready for planning or cloud upload.
 * Provides atomic serialization and recovery from interruptions or crashes.
 */
@Serializable
data class SelectionDraft(
    val draftId: String = UUID.randomUUID().toString(),
    val clips: List<StagedClip> = emptyList(),
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val isFullyPrepared: Boolean = false
) {
    val totalBytes: Long get() = clips.sumOf { it.byteSize }
    val totalDurationMs: Long get() = clips.sumOf { it.durationMs }
    val count: Int get() = clips.size

    fun isValid(): Boolean = clips.isNotEmpty() && clips.size <= MAX_FIXTURE_ITEMS && clips.all { it.isStagedValid }

    companion object {
        const val MAX_FIXTURE_ITEMS = 15
    }
}
