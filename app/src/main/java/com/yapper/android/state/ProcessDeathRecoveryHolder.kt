package com.yapper.android.state

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yapper.android.contract.YapperLifecycleState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State Holder designed specifically to survive Android OS Process Death.
 * Conforms to the Android Developers Architecture Guide:
 * If the process is terminated by the Low Memory Killer (LMK) and revived hours later,
 * it queries the server truth instead of relying on stale local memory.
 */
class ProcessDeathRecoveryHolder(
    private val savedStateHandle: SavedStateHandle,
    private val serverApiClient: RemoteJobApiClient
) : ViewModel() {

    companion object {
        private const val KEY_ACTIVE_JOB_ID = "yapper_active_job_id"
        private const val KEY_DRAFT_ID = "yapper_active_draft_id"
    }

    private val _lifecycleState = MutableStateFlow<YapperLifecycleState>(YapperLifecycleState.Selecting())
    val lifecycleState: StateFlow<YapperLifecycleState> = _lifecycleState.asStateFlow()

    init {
        // 1. Inspect SavedStateHandle for surviving keys after process death
        val survivingJobId: String? = savedStateHandle[KEY_ACTIVE_JOB_ID]

        if (survivingJobId != null) {
            // Process was resurrected! Query server-authoritative truth.
            resurrectJobState(survivingJobId)
        }
    }

    /**
     * Preserves active job ID into SavedStateHandle before backgrounding.
     */
    fun trackActiveJob(jobId: String, draftId: String) {
        savedStateHandle[KEY_ACTIVE_JOB_ID] = jobId
        savedStateHandle[KEY_DRAFT_ID] = draftId
    }

    /**
     * The Resurrection Routine: Ask the server what happened while we were dead.
     */
    private fun resurrectJobState(jobId: String) {
        viewModelScope.launch {
            _lifecycleState.value = YapperLifecycleState.RecoverableFailure(
                jobId = jobId,
                failureReason = "Reconnecting to server state..."
            )

            try {
                // In production: HTTP GET /api/jobs/{jobId}/state
                val serverState = serverApiClient.fetchServerJobState(jobId)
                _lifecycleState.value = serverState
            } catch (e: Exception) {
                _lifecycleState.value = YapperLifecycleState.RecoverableFailure(
                    jobId = jobId,
                    failureReason = "Unable to fetch server state: ${e.localizedMessage}. Tap to retry.",
                    canRetry = true
                )
            }
        }
    }

    fun clearActiveJob() {
        savedStateHandle.remove<String>(KEY_ACTIVE_JOB_ID)
        savedStateHandle.remove<String>(KEY_DRAFT_ID)
        _lifecycleState.value = YapperLifecycleState.Selecting()
    }

    interface RemoteJobApiClient {
        suspend fun fetchServerJobState(jobId: String): YapperLifecycleState
    }
}
