package de.scandurra.githubactionswatch.cli

import de.scandurra.githubactionswatch.client.Conclusion
import de.scandurra.githubactionswatch.client.RepoIdentifier
import de.scandurra.githubactionswatch.watch.WatchEvent
import kotlin.time.ExperimentalTime

const val RESET = "\u001B[0m"
const val BOLD = "\u001B[1m"
const val GRAY = "\u001B[90m"
const val GREEN = "\u001B[32m"
const val RED = "\u001B[31m"
const val BLUE = "\u001B[34m"
const val YELLOW = "\u001B[33m"

@OptIn(ExperimentalTime::class)
fun formatWatchEvent(event: WatchEvent, repo: RepoIdentifier): String {
    val localTime = event.occurredAt.toString()
    val ts = "$GRAY[${localTime}]$RESET"

    return when (event) {
        is WatchEvent.RunQueued -> {
            val branchInfo = "$BOLD${event.branch}$RESET @ $GRAY${shortSha(event.commitSha)}$RESET"
            "$ts  ${YELLOW}RUN QUEUED$RESET  $branchInfo (ID: ${event.runId})"
        }

        is WatchEvent.JobStarted ->
            "$ts   ${BLUE}JOB START $RESET  ${event.jobName} ${GRAY}(Run: ${event.identifier.runId})$RESET"

        is WatchEvent.JobFinished -> {
            val color = if (event.conclusion == Conclusion.SUCCESS) GREEN else RED
            "$ts   ${color}JOB FINISH$RESET ${event.conclusion} (Job: ${event.identifier.jobId})"
        }

        is WatchEvent.StepStarted ->
            "$ts     ${GRAY}STEP START$RESET #${event.identifier.stepNumber} ${event.stepName}"

        is WatchEvent.StepFinished -> {
            val color = if (event.conclusion == Conclusion.SUCCESS) GREEN else RED
            "$ts     ${color}STEP ${event.conclusion}$RESET #${event.identifier.stepNumber}"
        }
    }
}

private fun shortSha(sha: String): String = if (sha.length > 7) sha.substring(0, 7) else sha
