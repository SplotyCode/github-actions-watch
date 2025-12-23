@file:OptIn(kotlin.time.ExperimentalTime::class)

package de.scandurra.githubactionswatch.cli

import de.scandurra.githubactionswatch.client.Conclusion
import de.scandurra.githubactionswatch.client.RepoIdentifier
import de.scandurra.githubactionswatch.watch.WatchEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class WatchEventFormatterTest {
    private val repo = RepoIdentifier("owner", "repo")
    private val ts: Instant = Instant.parse("2024-01-02T03:04:05Z")

    @Test
    fun format_runQueued() {
        val event = WatchEvent.RunQueued(ts, 42L, "main", "abcdef0123456789")

        val actual = formatWatchEvent(event, repo)
        assertEquals("\u001B[90m[2024-01-02T03:04:05Z]\u001B[0m \u001B[33mRUN_QUEUED\u001B[0m  ID: \u001B[90m42                     \u001B[0m | \u001B[1mmain\u001B[0m @ \u001B[90mabcdef0\u001B[0m", actual)
    }

    @Test
    fun format_jobStarted() {
        val event = WatchEvent.JobStarted(
            occurredAt = ts,
            identifier = WatchEvent.JobIdentifier(runId = 7L, jobId = 77L),
            jobName = "Build"
        )

        val actual = formatWatchEvent(event, repo)
        assertEquals("\u001B[90m[2024-01-02T03:04:05Z]\u001B[0m \u001B[36mJOB_START\u001B[0m   ID: \u001B[90m7/77                   \u001B[0m | Name: Build", actual)
    }

    @Test
    fun format_jobFinished() {
        val e = WatchEvent.JobFinished(
            occurredAt = ts,
            identifier = WatchEvent.JobIdentifier(runId = 7L, jobId = 77L),
            conclusion = Conclusion.SUCCESS
        )

        val actual = formatWatchEvent(e, repo)
        assertEquals("\u001B[90m[2024-01-02T03:04:05Z]\u001B[0m \u001B[32mJOB_SUCCESS\u001B[0m ID: \u001B[90m7/77                   \u001B[0m | Status: SUCCESS", actual)
    }

    @Test
    fun format_stepStarted() {
        val event = WatchEvent.StepStarted(
            occurredAt = ts,
            identifier = WatchEvent.StepIdentifier(
                job = WatchEvent.JobIdentifier(runId = 7L, jobId = 77L),
                stepNumber = 3
            ),
            stepName = "Checkout"
        )

        val actual = formatWatchEvent(event, repo)
        assertEquals("\u001B[90m[2024-01-02T03:04:05Z]\u001B[0m \u001B[90mSTEP_START\u001B[0m  ID: \u001B[90m77#3                   \u001B[0m | Step: Checkout", actual)
    }

    @Test
    fun format_stepFinished() {
        val event = WatchEvent.StepFinished(
            occurredAt = ts,
            identifier = WatchEvent.StepIdentifier(
                job = WatchEvent.JobIdentifier(runId = 7L, jobId = 77L),
                stepNumber = 3
            ),
            conclusion = Conclusion.FAILURE
        )

        val actual = formatWatchEvent(event, repo)
        assertEquals("\u001B[90m[2024-01-02T03:04:05Z]\u001B[0m \u001B[31mSTEP_FAIL\u001B[0m   ID: \u001B[90m77#3                   \u001B[0m | Result: FAILURE", actual)
    }
}
