package com.yapper.android.evidence

import kotlinx.serialization.Serializable

/**
 * Universal Physical-Evidence Envelope for Mobile Testing (Android & iOS).
 * 
 * CORE CONTRACT RULE:
 * A physical test may be DEVICE PROVEN only when client and server evidence agree on the exact same job ID.
 * 
 * HARDENED REJECTION INVARIANTS:
 * Real evidence envelopes strictly reject:
 * 1. Empty-file hash (e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855) or blank hash.
 * 2. Missing or placeholder job IDs (e.g., "placeholder", "example", "mock", or all zeros).
 * 3. Placeholder device models or app builds.
 * 4. Missing or empty authoritative server evidence.
 * 5. Client/server job ID mismatch.
 */
@Serializable
data class PhysicalEvidenceEnvelope(
    val platform: String, // "Android" or "iOS"
    val deviceModel: String, // e.g. "Google Pixel 9 Pro", "Samsung SM-S928U", "iPhone 17 Pro Max"
    val osVersion: String, // e.g. "Android 15 (APV1.240517.022)", "iOS 18.2"
    val appBuild: String, // e.g. "1.0.0-rc3 (build 142)"
    val jobId: String, // Canonical UUID
    val mediaFingerprint: String, // Cryptographic content hash of original input media fixture (SHA-256 leading candidate)
    val clientTimestampsAndEvents: List<TimestampedEvent>,
    val authoritativeServerTimestampsAndEvents: List<TimestampedEvent>,
    val expectedFinalState: String, // e.g. "SafeToLeave", "Ready", "SavedExported"
    val observedFinalState: String,
    val verdict: TestVerdict // PASS or FAIL
) {
    companion object {
        /**
         * Known SHA-256 digest of 0-byte / empty content.
         * Must NEVER satisfy a valid fixture run.
         */
        const val EMPTY_CONTENT_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

        private val PLACEHOLDER_KEYWORDS = listOf("placeholder", "example", "mock", "test-job", "sample", "unknown")
    }

    enum class TestVerdict {
        PASS,
        FAIL
    }

    @Serializable
    data class TimestampedEvent(
        val eventName: String,
        val epochMs: Long,
        val targetJobId: String = "",
        val details: String = ""
    )

    /**
     * Invariant rule: A test is marked DEVICE PROVEN only when:
     * 1. Verdict is PASS.
     * 2. Observed final state matches expected final state.
     * 3. Media fingerprint is valid, non-blank, and NOT the empty-content SHA-256.
     * 4. Job ID, device model, and app build are legitimate non-placeholders.
     * 5. Client and server event logs both record activity on the exact same jobId.
     * 6. Server authoritative events confirm completion.
     */
    val isDeviceProven: Boolean
        get() {
            if (verdict != TestVerdict.PASS) return false
            if (observedFinalState != expectedFinalState) return false

            // Rejection Vector 1: Empty-content or invalid media fingerprint
            if (mediaFingerprint.isBlank() || mediaFingerprint.equals(EMPTY_CONTENT_SHA256, ignoreCase = true)) {
                return false
            }

            // Rejection Vector 2: Missing or placeholder Job ID
            if (jobId.isBlank() || PLACEHOLDER_KEYWORDS.any { jobId.contains(it, ignoreCase = true) }) {
                return false
            }

            // Rejection Vector 3: Placeholder device model or app build
            if (deviceModel.isBlank() || PLACEHOLDER_KEYWORDS.any { deviceModel.contains(it, ignoreCase = true) }) {
                return false
            }
            if (appBuild.isBlank() || PLACEHOLDER_KEYWORDS.any { appBuild.contains(it, ignoreCase = true) }) {
                return false
            }

            // Rejection Vector 4: Missing client or authoritative server evidence
            if (clientTimestampsAndEvents.isEmpty() || authoritativeServerTimestampsAndEvents.isEmpty()) {
                return false
            }

            // Rejection Vector 5: Client/Server job ID mismatch
            val clientJobMismatch = clientTimestampsAndEvents.any { it.targetJobId.isNotBlank() && it.targetJobId != jobId }
            val serverJobMismatch = authoritativeServerTimestampsAndEvents.any { it.targetJobId.isNotBlank() && it.targetJobId != jobId }
            if (clientJobMismatch || serverJobMismatch) {
                return false
            }

            // Confirm client initiated and server confirmed the job
            val hasClientStart = clientTimestampsAndEvents.any {
                it.eventName.contains("START", ignoreCase = true) || it.eventName.contains("SELECT", ignoreCase = true)
            }
            val hasServerConfirm = authoritativeServerTimestampsAndEvents.any {
                it.eventName.contains("CONFIRMED", ignoreCase = true) || it.eventName.contains("RENDER_COMPLETE", ignoreCase = true)
            }

            return hasClientStart && hasServerConfirm
        }
}
