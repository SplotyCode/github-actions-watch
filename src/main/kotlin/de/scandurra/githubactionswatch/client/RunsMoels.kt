package de.scandurra.githubactionswatch.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
enum class RunStatus {
    @SerialName("queued") QUEUED,
    @SerialName("in_progress") IN_PROGRESS,
    @SerialName("completed") COMPLETED,
    @SerialName("requested") REQUESTED,
    @SerialName("waiting") WAITING,
    @SerialName("pending") PENDING,
}

@Serializable
enum class Conclusion {
    @SerialName("success") SUCCESS,
    @SerialName("failure") FAILURE,
    @SerialName("cancelled") CANCELLED,
    @SerialName("skipped") SKIPPED,
    @SerialName("neutral") NEUTRAL,
    @SerialName("timed_out") TIMED_OUT,
    @SerialName("action_required") ACTION_REQUIRED,
    @SerialName("stale") STALE,
}

@Serializable
data class WorkflowRunsResponse(
    @SerialName("total_count") val totalCount: Int,
    @SerialName("workflow_runs") val workflowRuns: List<WorkflowRun>,
)

@OptIn(ExperimentalTime::class)
@Serializable
data class WorkflowRun(
    val id: Long,
    val name: String? = null,

    @SerialName("head_branch") val headBranch: String,
    @SerialName("head_sha") val headSha: String,

    val status: RunStatus,
    val conclusion: Conclusion? = null,

    @SerialName("created_at")
    @Serializable(with = IsoInstantSerializer::class)
    val createdAt: Instant,

    @SerialName("run_started_at")
    @Serializable(with = IsoInstantSerializer::class)
    val runStartedAt: Instant? = null,

    @SerialName("updated_at")
    @Serializable(with = IsoInstantSerializer::class)
    val updatedAt: Instant,
)