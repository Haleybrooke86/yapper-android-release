package com.yapper.android.reconciler

import com.yapper.android.contract.ServerUploadFinalizationProof
import com.yapper.android.contract.YapperLifecycleState
import kotlinx.serialization.Serializable

/**
 * Server Truth Reconciler (Backlog B-1 Resolution).
 * 
 * CORE CONTRACT RULE:
 * Never declare upload failure or force re-upload without inspecting server reality.
 * The client is strictly a projection of authoritative server truth.
 */
class ServerTruthReconciler(private val apiClient: UploadStateApiClient) {

    @Serializable
    data class RemoteUploadStateResponse(
        val jobId: String,
        val complete: List<String>, // Array of confirmed object paths in cloud storage
        val status: String,         // "uploading", "uploaded", "editing", "ready", "failed"
        val totalExpectedBytes: Long,
        val totalReceivedBytes: Long,
        val integrityVerified: Boolean = false, // Target Mobile Contract
        val uploadFinalized: Boolean = false,   // Target Mobile Contract
        val serverConfirmedAtEpochMs: Long = 0L
    )

    sealed class ReconciliationResult {
        data class NotSafe(val missingBytes: Long, val completedParts: List<String>) : ReconciliationResult()
        data class HandoffPending(val jobId: String) : ReconciliationResult()
        data class SafeToLeave(val proof: ServerUploadFinalizationProof) : ReconciliationResult()
        data class RemoteProcessing(val stage: String, val progress: Float) : ReconciliationResult()
        data class TerminalFailure(val reason: String) : ReconciliationResult()
        data class NetworkError(val error: Throwable) : ReconciliationResult()
    }

    /**
     * Queries server reality and reconciles the authoritative lifecycle state.
     */
    suspend fun reconcile(jobId: String, totalExpectedBytes: Long): ReconciliationResult {
        return try {
            val response = apiClient.fetchUploadState(jobId)

            when {
                // Case 1: Job already in server-owned processing
                response.status == "editing" -> {
                    ReconciliationResult.RemoteProcessing(stage = "frame_render", progress = 0.5f)
                }

                // Case 2: Full target contract satisfied (SafeToLeave)
                response.totalReceivedBytes == totalExpectedBytes &&
                response.integrityVerified &&
                response.uploadFinalized -> {
                    val proof = ServerUploadFinalizationProof(
                        jobId = response.jobId,
                        totalExpectedBytes = response.totalExpectedBytes,
                        bytesReceived = response.totalReceivedBytes,
                        checksumsVerified = response.integrityVerified,
                        uploadFinalized = response.uploadFinalized,
                        finalizedAtEpochMs = response.serverConfirmedAtEpochMs
                    )
                    ReconciliationResult.SafeToLeave(proof)
                }

                // Case 3: 100% bytes on server, but finalization handoff in progress
                response.totalReceivedBytes == totalExpectedBytes -> {
                    ReconciliationResult.HandoffPending(response.jobId)
                }

                // Case 4: Partial bytes on server (resume from byte N+1)
                else -> {
                    val missing = totalExpectedBytes - response.totalReceivedBytes
                    ReconciliationResult.NotSafe(missingBytes = missing, completedParts = response.complete)
                }
            }
        } catch (e: Exception) {
            ReconciliationResult.NetworkError(e)
        }
    }

    interface UploadStateApiClient {
        suspend fun fetchUploadState(jobId: String): RemoteUploadStateResponse
    }
}
