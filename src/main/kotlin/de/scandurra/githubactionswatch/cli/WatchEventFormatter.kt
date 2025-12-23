package de.scandurra.githubactionswatch.cli

import de.scandurra.githubactionswatch.client.RepoIdentifier
import de.scandurra.githubactionswatch.watch.WatchEvent
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun formatWatchEvent(event: WatchEvent, repo: RepoIdentifier): String {
    val ts = event.occurredAt.toString()
    val repoStr = "${repo.owner}/${repo.repo}"

    return when (event) {
        is WatchEvent.RunQueued ->
            "$ts | $repoStr | RUN ${event.runId} QUEUED branch=${event.branch} sha=${shortSha(event.commitSha)}"

        is WatchEvent.JobStarted ->
            "$ts | $repoStr | JOB ${event.identifier.runId}/${event.identifier.jobId} START name=\"${event.jobName}\""

        is WatchEvent.JobFinished ->
            "$ts | $repoStr | JOB ${event.identifier.runId}/${event.identifier.jobId} FINISH conclusion=${event.conclusion}"

        is WatchEvent.StepStarted ->
            "$ts | $repoStr | STEP ${event.identifier.job.runId}/${event.identifier.job.jobId}#${event.identifier.stepNumber} START name=\"${event.stepName}\""

        is WatchEvent.StepFinished ->
            "$ts | $repoStr | STEP ${event.identifier.job.runId}/${event.identifier.job.jobId}#${event.identifier.stepNumber} FINISH conclusion=${event.conclusion}"
    }
}

private fun shortSha(sha: String): String = if (sha.length > 7) sha.substring(0, 7) else sha
