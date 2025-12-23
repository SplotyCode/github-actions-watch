package de.scandurra.githubactionswatch.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class WorkflowJobsResponse(
    @SerialName("total_count") val totalCount: Int,
    val jobs: List<WorkflowJob>,
)

@Serializable
enum class JobStatus {
    @SerialName("queued") QUEUED,
    @SerialName("in_progress") IN_PROGRESS,
    @SerialName("completed") COMPLETED,
}

@Serializable
enum class StepStatus {
    @SerialName("queued") QUEUED,
    @SerialName("in_progress") IN_PROGRESS,
    @SerialName("completed") COMPLETED,
}

@OptIn(ExperimentalTime::class)
@Serializable
data class WorkflowJob(
    val id: Long,
    val name: String,
    val status: JobStatus,
    val conclusion: Conclusion,

    @SerialName("started_at")
    val startedAt: Instant,

    @SerialName("completed_at")
    val completedAt: Instant? = null,

    val steps: List<WorkflowStep> = emptyList(),
)

@OptIn(ExperimentalTime::class)
@Serializable
data class WorkflowStep(
    val name: String,
    val number: Int,

    val status: StepStatus,
    val conclusion: Conclusion? = null,

    @SerialName("started_at")
    val startedAt: Instant? = null,

    @SerialName("completed_at")
    val completedAt: Instant? = null,
)