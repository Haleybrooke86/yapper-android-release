package com.yapper.android.contract

import kotlinx.serialization.Serializable

/**
 * CONTRACT CLASSIFICATION & COMPATIBILITY AUDIT:
 * 
 * 1. CURRENT SERVER CONTRACT (Today's Backend Reality):
 *    - Expected cloud storage object path exists.
 *    - Stored object byte size == expected byte count.
 *    - POST /uploaded triggers atomic verification, records uploaded_at,
 *      and hands the job into server-owned Editing transition (uploading -> uploaded -> Editing).
 *    - GET /upload-state returns: complete[] + job status + deletion state.
 *    - DOES NOT EXPOSE: explicit `checksumsVerified` or `uploadFinalized` API fields.
 * 
 * 2. TARGET MOBILE CONTRACT (Promise Integrity Standard):
 *    - All required source objects verified.
 *    - Algorithm-neutral cryptographic content-integrity verification: The authoritative
 *      server must prove that durable source content matches the client-finalized content
 *      before SafeToLeave becomes available.
 *    - SHA-256 is recorded as the leading candidate implementation, not yet a permanent product-contract requirement.
 *    - Server upload session finalization and handoff confirmed.
 *    - Explicitly codified in ServerUploadFinalizationProof below.
 * 
 * ARCHITECTURAL DIRECTIVE:
 * We DO NOT weaken Android to "bytes match = safe".
 * We DO NOT synthesize or forge these fields client-side.
 * Instead, this target contract is classified as:
 * TARGET MOBILE CONTRACT (UNPROVEN / UNAVAILABLE ON TODAY'S SERVER).
 * 
 * SEQUENCE & GATING CONSTRAINT:
 * Do NOT implement the shared backend lane until Delivery Quality PR #322
 * clears human audiovisual acceptance. Only then will a single shared Mobile Upload Contract lane
 * add authoritative integrity and finalization evidence for Web + iOS + Android together.
 */
@Serializable
data class ServerUploadFinalizationProof(
    val jobId: String,
    val totalExpectedBytes: Long,
    val bytesReceived: Long,
    val checksumsVerified: Boolean, // TARGET MOBILE CONTRACT: Algorithm-neutral integrity verification (SHA-256 leading candidate)
    val uploadFinalized: Boolean,   // TARGET MOBILE CONTRACT (Currently unexposed by backend)
    val finalizedAtEpochMs: Long
) {
    /**
     * Algorithm-neutral alias for checksumsVerified.
     */
    val integrityVerified: Boolean get() = checksumsVerified

    val isCompleteAndVerified: Boolean
        get() = totalExpectedBytes > 0 &&
                bytesReceived == totalExpectedBytes &&
                integrityVerified &&
                uploadFinalized
}

/**
 * The 9 Canonical Yapper Lifecycle States defined by the Mobile Core Contract.
 * The Android client is strictly a projection of server-authoritative truth.
 * Under no circumstances does Android invent local or optimistic states.
 */
@Serializable
sealed class YapperLifecycleState {

    /**
     * 1. Selecting: Creator picks up to 15 video clips via system Photo Picker.
     * Tap order is numbered, recorded, and strictly preserved.
     */
    @Serializable
    data class Selecting(val selectedCount: Int = 0) : YapperLifecycleState()

    /**
     * 2. Uploading: Bounded-memory streaming of 5MB chunks via WorkManager dataSync service.
     */
    @Serializable
    data class Uploading(
        val jobId: String,
        val progressPercent: Float,
        val bytesTransferred: Long,
        val totalBytes: Long
    ) : YapperLifecycleState()

    /**
     * 3. Upload Confirmed: Server verifies receipt of all chunk bytes, integrity checksums,
     * and records session finalization.
     */
    @Serializable
    data class UploadConfirmed(
        val proof: ServerUploadFinalizationProof
    ) : YapperLifecycleState() {
        val is100PercentConfirmed: Boolean get() = proof.isCompleteAndVerified
    }

    /**
     * 4. Safe To Leave: The Creator Peace-of-Mind Guarantee.
     * 
     * PROMISE INTEGRITY CONTRACT:
     * Reachable ONLY after authoritative server evidence explicitly proves:
     * 1. All required source bytes received (bytesReceived == totalExpectedBytes)
     * 2. Algorithm-neutral cryptographic content-integrity verification: The authoritative
     *    server must prove that durable source content matches the client-finalized content
     *    before SafeToLeave becomes available (integrityVerified == true).
     *    (SHA-256 is recorded as the leading candidate implementation, not yet a permanent product-contract requirement).
     * 3. Upload finalization confirmed (uploadFinalized == true).
     * 
     * Invariant: Complete byte count with failed/unverified cryptographic content-integrity CANNOT enter SafeToLeave.
     * 
     * Note: In current production, this state is UNAVAILABLE / UNPROVEN because today's
     * backend only verifies byte count and path existence without exposing cryptographic integrity verification fields.
     */
    @Serializable
    data class SafeToLeave(
        val proof: ServerUploadFinalizationProof
    ) : YapperLifecycleState() {
        val jobId: String get() = proof.jobId

        init {
            require(proof.bytesReceived == proof.totalExpectedBytes && proof.totalExpectedBytes > 0) {
                "PROMISE INTEGRITY VIOLATION: SafeToLeave requires all source bytes received (received=${proof.bytesReceived}, expected=${proof.totalExpectedBytes})"
            }
            require(proof.integrityVerified) {
                "PROMISE INTEGRITY VIOLATION: SafeToLeave requires complete and valid cryptographic content-integrity verification"
            }
            require(proof.uploadFinalized) {
                "PROMISE INTEGRITY VIOLATION: SafeToLeave requires upload finalization to be confirmed by server"
            }
        }
    }

    /**
     * 5. Editing: Modal GPU engine (yap-worker) is actively processing scene cuts,
     * Claude planning, face tracking, dynamic zooms, and 4K HDR frame generation.
     */
    @Serializable
    data class Editing(
        val jobId: String,
        val stageName: String,
        val stageProgress: Float
    ) : YapperLifecycleState()

    /**
     * 6. Needs Attention: Fired only if creator review is required (e.g. crop check or social proof verification).
     */
    @Serializable
    data class NeedsAttention(
        val jobId: String,
        val reason: String,
        val actionRequired: String
    ) : YapperLifecycleState()

    /**
     * 7. Ready: 4K HDR Yap render complete, QC passed, signed stream URL available for review.
     */
    @Serializable
    data class Ready(
        val jobId: String,
        val previewUrl: String,
        val durationSeconds: Float,
        val isHdr: Boolean
    ) : YapperLifecycleState()

    /**
     * 8. Saved / Exported: Losslessly written to Movies/Yapper via MediaStore with IS_PENDING atomic commit.
     */
    @Serializable
    data class SavedExported(val jobId: String, val galleryUriString: String) : YapperLifecycleState()

    /**
     * 9. Recoverable Failure: Transient network or container interruption.
     * Auto-resumes without creator data loss or repeated billing.
     */
    @Serializable
    data class RecoverableFailure(
        val jobId: String?,
        val failureReason: String,
        val canRetry: Boolean = true,
        val serverAcknowledgedBytes: Long = 0L
    ) : YapperLifecycleState()
}
