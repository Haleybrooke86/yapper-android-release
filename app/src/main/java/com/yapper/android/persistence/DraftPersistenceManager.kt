package com.yapper.android.persistence

import android.content.Context
import com.yapper.android.model.SelectionDraft
import com.yapper.android.model.StagedClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Manages durable local persistence of SelectionDrafts and active job context.
 * 
 * CORE ARCHITECTURAL INVARIANT:
 * Survives both system-initiated process death (LMK) AND user swipe-away from Recents.
 * Uses atomic file transactions (.tmp -> .json rename) to prevent partial write corruption.
 */
class DraftPersistenceManager(private val context: Context) {

    private val json = Json { 
        prettyPrint = false 
        ignoreUnknownKeys = true 
        encodeDefaults = true 
    }

    private val draftsDir: File by lazy {
        File(context.filesDir, "drafts").apply {
            if (!exists()) mkdirs()
        }
    }

    private val prefs by lazy {
        context.getSharedPreferences("yapper_persistence_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_ACTIVE_DRAFT_ID = "active_draft_id"
        private const val KEY_ACTIVE_JOB_ID = "active_job_id"
    }

    /**
     * Atomically saves a selection draft to private internal storage.
     */
    suspend fun saveDraft(draft: SelectionDraft): Unit = withContext(Dispatchers.IO) {
        val targetFile = File(draftsDir, "draft_${draft.draftId}.json")
        val tempFile = File(draftsDir, "draft_${draft.draftId}.tmp")

        try {
            val jsonString = json.encodeToString(draft)
            FileOutputStream(tempFile).use { out ->
                out.write(jsonString.toByteArray(Charsets.UTF_8))
                out.flush()
            }

            if (!tempFile.renameTo(targetFile)) {
                throw IOException("Atomic rename failed from ${tempFile.name} to ${targetFile.name}")
            }

            // Persist as the active draft ID
            prefs.edit().putString(KEY_ACTIVE_DRAFT_ID, draft.draftId).apply()
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }

    /**
     * Restores the active draft if one exists, verifying staged file existence.
     */
    suspend fun loadActiveDraft(): SelectionDraft? = withContext(Dispatchers.IO) {
        val activeDraftId = prefs.getString(KEY_ACTIVE_DRAFT_ID, null) ?: return@withContext null
        loadDraft(activeDraftId)
    }

    /**
     * Loads a specific draft by ID and verifies physical integrity of staged clips.
     */
    suspend fun loadDraft(draftId: String): SelectionDraft? = withContext(Dispatchers.IO) {
        val file = File(draftsDir, "draft_${draftId}.json")
        if (!file.exists()) return@withContext null

        try {
            val jsonString = file.readText(Charsets.UTF_8)
            val draft = json.decodeFromString<SelectionDraft>(jsonString)

            // Verify physical files still exist in cacheDir
            val verifiedClips = draft.clips.map { clip ->
                val stagedFile = File(clip.stagedFilePath)
                if (!stagedFile.exists() || stagedFile.length() != clip.byteSize) {
                    // Staged file was evicted by OS cache cleanup
                    clip.copy(byteSize = 0L)
                } else {
                    clip
                }
            }

            draft.copy(clips = verifiedClips, isFullyPrepared = verifiedClips.all { it.isStagedValid })
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Tracks the server-authoritative active job ID across process death and reboots.
     */
    fun setActiveJobId(jobId: String?) {
        if (jobId != null) {
            prefs.edit().putString(KEY_ACTIVE_JOB_ID, jobId).apply()
        } else {
            prefs.edit().remove(KEY_ACTIVE_JOB_ID).apply()
        }
    }

    fun getActiveJobId(): String? = prefs.getString(KEY_ACTIVE_JOB_ID, null)

    /**
     * Cleans up draft files and deletes associated staged media.
     */
    suspend fun deleteDraft(draftId: String): Unit = withContext(Dispatchers.IO) {
        val draft = loadDraft(draftId)
        draft?.clips?.forEach { clip ->
            File(clip.stagedFilePath).delete()
        }
        File(draftsDir, "draft_${draftId}.json").delete()

        if (prefs.getString(KEY_ACTIVE_DRAFT_ID, null) == draftId) {
            prefs.edit().remove(KEY_ACTIVE_DRAFT_ID).apply()
        }
    }
}
