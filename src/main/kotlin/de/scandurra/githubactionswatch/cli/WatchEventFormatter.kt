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
const val CYAN = "\u001B[36m"
const val YELLOW = "\u001B[33m"

@OptIn(ExperimentalTime::class)
fun formatWatchEvent(event: WatchEvent, repo: RepoIdentifier): String {
    val localTime = event.occurredAt.toString()
    val ts = "$GRAY[${localTime}]$RESET"

    return when (event) {
        is WatchEvent.RunQueued -> {
            val type = "${YELLOW}RUN_QUEUED$RESET ".padEnd(20)
            val id = event.runId.toString().padEnd(23)
            val info = "${BOLD}${event.branch}$RESET @ ${GRAY}${shortSha(event.commitSha)}$RESET"
            "$ts $type ID: $GRAY$id$RESET | $info"
        }

        is WatchEvent.JobStarted -> {
            val type = "${CYAN}JOB_START$RESET  ".padEnd(20)
            val id = "${event.identifier.runId}/${event.identifier.jobId}".padEnd(23)
            "$ts $type ID: $GRAY$id$RESET | Name: ${event.jobName}"
        }

        is WatchEvent.JobFinished -> {
            val type = if (event.conclusion == Conclusion.SUCCESS) "${GREEN}JOB_SUCCESS$RESET" else "${RED}JOB_FAILED$RESET"
            val id = "${event.identifier.runId}/${event.identifier.jobId}".padEnd(23)
            "$ts ${type.padEnd(20)} ID: $GRAY$id$RESET | Status: ${event.conclusion}"
        }

        is WatchEvent.StepStarted -> {
            val type = "${GRAY}STEP_START$RESET".padEnd(20)
            val id = "${event.identifier.job.jobId}#${event.identifier.stepNumber}".padEnd(23)
            "$ts $type ID: $GRAY$id$RESET | Step: ${event.stepName}"
        }

        is WatchEvent.StepFinished -> {
            val type = if (event.conclusion == Conclusion.SUCCESS) "${GREEN}STEP_OK$RESET" else "${RED}STEP_FAIL$RESET"
            val id = "${event.identifier.job.jobId}#${event.identifier.stepNumber}".padEnd(23)
            "$ts ${type.padEnd(20)} ID: $GRAY$id$RESET | Result: ${event.conclusion}"
        }
    }
}

private fun shortSha(sha: String): String = if (sha.length > 7) sha.substring(0, 7) else sha
