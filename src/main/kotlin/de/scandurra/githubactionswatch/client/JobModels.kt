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


@OptIn(ExperimentalTime::class)
@Serializable
data class WorkflowJob(
    val id: Long,
    val name: String,
    val status: RunStatus,
    val conclusion: Conclusion?,

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

    val status: RunStatus,
    val conclusion: Conclusion? = null,

    @SerialName("started_at")
    val startedAt: Instant? = null,

    @SerialName("completed_at")
    val completedAt: Instant? = null,
)