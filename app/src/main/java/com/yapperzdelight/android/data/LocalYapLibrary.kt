package com.yapperzdelight.android.data

import android.content.Context
import com.yapper.android.model.SelectionDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * A clip the creator chose to keep on this phone. It is her original footage, not an edit:
 * editing happens in the online studio, and the UI always says so.
 */
@Serializable
data class LocalYap(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val filePath: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val byteSize: Long,
    val lookTitle: String,
    val savedAtEpochMs: Long = System.currentTimeMillis(),
) {
    val file: File get() = File(filePath)
    val exists: Boolean get() = file.exists() && file.length() > 0
}

/**
 * The honest local library behind "My yaps". Kept clips move out of the evictable cache into app files,
 * so they are still there tomorrow, and nothing is ever listed that is not a real file on this phone.
 */
class LocalYapLibrary(private val context: Context) {

    private val dir: File by lazy { File(context.filesDir, "yaps").apply { if (!exists()) mkdirs() } }
    private val index: File get() = File(dir, "index.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    suspend fun load(): List<LocalYap> = withContext(Dispatchers.IO) {
        readIndex().filter { it.exists }.sortedByDescending { it.savedAtEpochMs }
    }

    /** Moves every staged clip of [draft] into the library and returns the updated list. */
    suspend fun keep(draft: SelectionDraft, lookTitle: String): List<LocalYap> = withContext(Dispatchers.IO) {
        val current = readIndex().filter { it.exists }.toMutableList()
        draft.clips.sortedBy { it.tapIndex }.forEach { clip ->
            val source = File(clip.stagedFilePath)
            if (!source.exists() || source.length() == 0L) return@forEach
            val target = File(dir, "clip_${clip.id}.mp4")
            val moved = source.renameTo(target) || runCatching {
                source.copyTo(target, overwrite = true)
                source.delete()
                true
            }.getOrDefault(false)
            if (!moved) return@forEach
            current.add(
                LocalYap(
                    id = clip.id,
                    title = "Clip ${clip.tapIndex}",
                    filePath = target.absolutePath,
                    durationMs = clip.durationMs,
                    width = clip.width,
                    height = clip.height,
                    byteSize = target.length(),
                    lookTitle = lookTitle,
                ),
            )
        }
        writeIndex(current)
        current.sortedByDescending { it.savedAtEpochMs }
    }

    suspend fun remove(yap: LocalYap): List<LocalYap> = withContext(Dispatchers.IO) {
        yap.file.delete()
        val next = readIndex().filter { it.id != yap.id && it.exists }
        writeIndex(next)
        next.sortedByDescending { it.savedAtEpochMs }
    }

    private fun readIndex(): List<LocalYap> =
        runCatching { json.decodeFromString<List<LocalYap>>(index.readText()) }.getOrDefault(emptyList())

    private fun writeIndex(items: List<LocalYap>) {
        val tmp = File(dir, "index.json.tmp")
        tmp.writeText(json.encodeToString(items))
        if (!tmp.renameTo(index)) {
            index.writeText(tmp.readText())
            tmp.delete()
        }
    }
}
