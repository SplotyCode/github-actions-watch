@file:OptIn(kotlin.time.ExperimentalTime::class)

package de.scandurra.githubactionswatch.watch

import de.scandurra.githubactionswatch.client.Conclusion
import kotlin.time.Instant

sealed interface WatchEvent {
    val occurredAt: Instant

    data class RunQueued(
        override val occurredAt: Instant,
        val runId: Long,
        val branch: String,
        val commitSha: String,
    ) : WatchEvent

    data class JobIdentifier(
        val runId: Long,
        val jobId: Long,
    )

    data class JobStarted(
        override val occurredAt: Instant,
        val identifier: JobIdentifier,
        val jobName: String,
    ) : WatchEvent

    data class JobFinished(
        override val occurredAt: Instant,
        val identifier: JobIdentifier,
        val conclusion: Conclusion,
    ) : WatchEvent

    data class StepIdentifier(
        val job: JobIdentifier,
        val stepNumber: Int,
    )

    data class StepStarted(
        override val occurredAt: Instant,
        val identifier: StepIdentifier,
        val stepName: String,
    ) : WatchEvent

    data class StepFinished(
        override val occurredAt: Instant,
        val identifier: StepIdentifier,
        val conclusion: Conclusion,
    ) : WatchEvent
}
