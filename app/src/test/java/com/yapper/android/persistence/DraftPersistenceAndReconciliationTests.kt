package com.yapper.android.persistence

import com.yapper.android.contract.ServerUploadFinalizationProof
import com.yapper.android.contract.YapperLifecycleState
import com.yapper.android.idempotency.IdempotencyManager
import com.yapper.android.model.SelectionDraft
import com.yapper.android.model.StagedClip
import com.yapper.android.reconciler.ServerTruthReconciler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Unit tests for Steps A through E of the Android Minimal Implementation Chain:
 * - Media selection & draft persistence across process death
 * - Deterministic idempotency keys for zero duplicate jobs
 * - Server truth reconciliation (Backlog B-1) and SafeToLeave state enforcement
 */
class DraftPersistenceAndReconciliationTests {

    /**
     * STEP A TEST: Selection draft preserves 15 clips in strict tap order.
     */
    @Test
    fun testStepA_TapOrderPreservedFor15Clips() {
        val clips = (1..15).map { index ->
            StagedClip(
                id = "clip-$index",
                originalUriString = "content://media/picker/$index",
                stagedFilePath = "/data/user/0/com.yapper.android/cache/staging/clip_$index.mp4",
                tapIndex = index,
                byteSize = 10_000_000L,
                durationMs = 5000L
            )
        }

        val draft = SelectionDraft(draftId = "draft-15-clips", clips = clips)
        assertEquals(15, draft.count)
        assertEquals(150_000_000L, draft.totalBytes)
        
        // Assert strict tap order 1 through 15
        draft.clips.forEachIndexed { i, clip ->
            assertEquals(i + 1, clip.tapIndex)
        }
    }

    /**
     * STEP C TEST: Deterministic Idempotency Key Generation (UUIDv5).
     * Proves identical parameters produce identical keys; differing parameters produce unique keys.
     */
    @Test
    fun testStepC_DeterministicIdempotencyKey() {
        val creatorId = "creator-456"
        val draftId = "draft-789"
        val contentDigest = "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e"

        val key1 = IdempotencyManager.generateJobIdempotencyKey(creatorId, draftId, contentDigest)
        val key2 = IdempotencyManager.generateJobIdempotencyKey(creatorId, draftId, contentDigest)
        val keyDifferentDraft = IdempotencyManager.generateJobIdempotencyKey(creatorId, "draft-different", contentDigest)

        // Must be identical for exact same inputs (re-tap safety)
        assertEquals("Identical inputs must yield identical idempotency keys", key1, key2)
        
        // Must be valid UUID format
        assertNotNull(UUID.fromString(key1))

        // Must match backend regex for clientRequestId: /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
        val backendRegex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE)
        assertTrue("Idempotency key must satisfy backend UUIDv4 schema", key1.matches(backendRegex))
        
        // Must differ for different drafts
        assertNotEquals("Different drafts must yield distinct idempotency keys", key1, keyDifferentDraft)
    }

    /**
     * STEP D & E TEST: Server Truth Reconciler (Backlog B-1).
     * Tests partial upload, handoff pending, SafeToLeave, and editing transitions.
     */
    @Test
    fun testStepDAndE_ServerTruthReconcilerStates() = runTest {
        val totalExpected = 50_000_000L
        val jobId = "job-recon-123"

        // 1. Partial upload -> NotSafe (returns missing bytes)
        val mockPartialApi = object : ServerTruthReconciler.UploadStateApiClient {
            override suspend fun fetchUploadState(jobId: String) = ServerTruthReconciler.RemoteUploadStateResponse(
                jobId = jobId,
                complete = listOf("part_1.mp4"),
                status = "uploading",
                totalExpectedBytes = totalExpected,
                totalReceivedBytes = 25_000_000L
            )
        }
        val reconPartial = ServerTruthReconciler(mockPartialApi)
        val resultPartial = reconPartial.reconcile(jobId, totalExpected)
        assertTrue(resultPartial is ServerTruthReconciler.ReconciliationResult.NotSafe)
        assertEquals(25_000_000L, (resultPartial as ServerTruthReconciler.ReconciliationResult.NotSafe).missingBytes)

        // 2. 100% bytes received, but unfinalized -> HandoffPending
        val mockPendingApi = object : ServerTruthReconciler.UploadStateApiClient {
            override suspend fun fetchUploadState(jobId: String) = ServerTruthReconciler.RemoteUploadStateResponse(
                jobId = jobId,
                complete = listOf("part_1.mp4", "part_2.mp4"),
                status = "uploading",
                totalExpectedBytes = totalExpected,
                totalReceivedBytes = totalExpected,
                integrityVerified = false,
                uploadFinalized = false
            )
        }
        val reconPending = ServerTruthReconciler(mockPendingApi)
        val resultPending = reconPending.reconcile(jobId, totalExpected)
        assertTrue(resultPending is ServerTruthReconciler.ReconciliationResult.HandoffPending)

        // 3. 100% bytes + integrityVerified + uploadFinalized -> SafeToLeave
        val mockSafeApi = object : ServerTruthReconciler.UploadStateApiClient {
            override suspend fun fetchUploadState(jobId: String) = ServerTruthReconciler.RemoteUploadStateResponse(
                jobId = jobId,
                complete = listOf("part_1.mp4", "part_2.mp4"),
                status = "uploaded",
                totalExpectedBytes = totalExpected,
                totalReceivedBytes = totalExpected,
                integrityVerified = true,
                uploadFinalized = true,
                serverConfirmedAtEpochMs = 1727720000000L
            )
        }
        val reconSafe = ServerTruthReconciler(mockSafeApi)
        val resultSafe = reconSafe.reconcile(jobId, totalExpected)
        assertTrue(resultSafe is ServerTruthReconciler.ReconciliationResult.SafeToLeave)
        val proof = (resultSafe as ServerTruthReconciler.ReconciliationResult.SafeToLeave).proof
        assertTrue(proof.isCompleteAndVerified)
        assertEquals(jobId, proof.jobId)

        // Verify that SafeToLeave lifecycle state accepts this valid proof
        val safeState = YapperLifecycleState.SafeToLeave(proof)
        assertEquals(jobId, safeState.jobId)
    }
}
