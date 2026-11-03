package com.yapper.android.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SelectionDraftTest {

    @Test
    fun testTapOrderPreservedAndFixtureLimitRespected() {
        val clips = (1..15).map { index ->
            StagedClip(
                id = "clip-$index",
                originalUriString = "content://media/external/video/media/$index",
                stagedFilePath = "/tmp/fake_staged_$index.mp4",
                tapIndex = index,
                byteSize = 10_000_000L,
                durationMs = 5000L,
                width = 3840,
                height = 2160,
                isHdr = true
            )
        }

        val draft = SelectionDraft(
            clips = clips,
            isFullyPrepared = true
        )

        assertEquals(15, draft.count)
        assertEquals(150_000_000L, draft.totalBytes)
        assertEquals(75_000L, draft.totalDurationMs)

        // Verify exact tap ordering is sequential 1 to 15
        draft.clips.forEachIndexed { idx, clip ->
            assertEquals(idx + 1, clip.tapIndex)
            assertEquals("clip-${idx + 1}", clip.id)
            assertTrue(clip.isHdr)
        }
    }

    @Test
    fun testExceedingMaxFixtureItemsIsInvalid() {
        val clips = (1..16).map { index ->
            StagedClip(
                id = "clip-$index",
                originalUriString = "content://media/external/video/media/$index",
                stagedFilePath = "/tmp/fake_staged_$index.mp4",
                tapIndex = index,
                byteSize = 1000L
            )
        }

        val draft = SelectionDraft(clips = clips)
        assertFalse("Drafts exceeding 15 fixture items must be rejected", draft.isValid())
    }

    @Test
    fun testEmptyDraftIsInvalid() {
        val draft = SelectionDraft(clips = emptyList())
        assertFalse(draft.isValid())
    }
}
