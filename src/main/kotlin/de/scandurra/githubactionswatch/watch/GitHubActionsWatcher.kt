@file:OptIn(ExperimentalTime::class)

package de.scandurra.githubactionswatch.watch

import de.scandurra.githubactionswatch.client.*
import de.scandurra.githubactionswatch.storage.RunMeta
import de.scandurra.githubactionswatch.storage.WatchCursor
import de.scandurra.githubactionswatch.storage.WatchCursorStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class WatchEventContext(
    val event: WatchEvent,
    val branch: String?,
    val commitSha: String?,
)

class GitHubActionsWatcher(
    private val client: GitHubActionsClient,
    private val store: WatchCursorStore,
    private val safetyDelay: Duration = 5.minutes,
    private val overlap: Duration = 2.minutes,
    private val dedupeRetention: Duration = 30.minutes,
) {
    fun watch(repo: RepoIdentifier, pollInterval: Duration = 10.seconds): Flow<WatchEvent> =
        watchWithContext(repo, pollInterval).let { ctxFlow ->
            flow { ctxFlow.collect { emit(it.event) } }
        }

    fun watchWithContext(repo: RepoIdentifier, pollInterval: Duration = 10.seconds): Flow<WatchEventContext> = flow {
        var state = store.load(repo) ?: bootstrap(repo)

        while (true) {
            val now = Clock.System.now()
            val (events, nextState) = pollOnce(repo, state, now)
            state = nextState
            store.save(repo, state)

            events.sortedBy { it.event.occurredAt }.forEach { emit(it) }
            delay(pollInterval)
        }
    }

    private suspend fun bootstrap(repo: RepoIdentifier): WatchCursor {
        val now = Clock.System.now()
        val initialStable = now.minus(safetyDelay)
        return WatchCursor(
            bootstrapInstant = now,
            stableScannedTo = initialStable,
        ).also { store.save(repo, it) }
    }

    private suspend fun pollOnce(
        repo: RepoIdentifier,
        state: WatchCursor,
        now: Instant,
    ): Pair<List<WatchEventContext>, WatchCursor> {

        val queryFrom = maxOf(state.bootstrapInstant, state.stableScannedTo.minus(overlap))

        val runs = client.listAllWorkflowRuns(
            repoId = repo,
            created = TimeRange(fromInclusive = queryFrom, toInclusive = now),
        ).toList()

        val openRuns = state.openRuns.toMutableMap()
        val processed = state.processedEventIds.toMutableMap()
        val out = mutableListOf<WatchEventContext>()

        for (run in runs) {
            val meta = RunMeta(
                createdAt = run.createdAt,
                branch = run.headBranch,
                sha = run.headSha,
                name = run.name,
            )
            openRuns.putIfAbsent(run.id, meta)

            val queuedEvent = WatchEvent.RunQueued(run.createdAt, run.id, run.headBranch, run.headSha)
            emitIfNew(queuedEvent, processed, out, run.headBranch, run.headSha)
        }

        val runIdsToPoll = openRuns.keys.toList()
        val stillOpen = mutableMapOf<Long, RunMeta>()

        for (runId in runIdsToPoll) {
            val meta = openRuns[runId] ?: continue

            val jobs = client.listJobsForWorkflowRun(repo, runId).toList()

            var anyJobIncomplete = false
            var latestCompletion: Instant? = null

            for (job in jobs) {
                job.startedAt.let {
                    val event = WatchEvent.JobStarted(
                        occurredAt = it,
                        identifier = WatchEvent.JobIdentifier(runId, job.id),
                        jobName = job.name,
                    )
                    emitIfNew(event, processed, out, meta.branch, meta.sha)
                }

                if (job.status == JobStatus.COMPLETED && job.completedAt != null) {
                    val e = WatchEvent.JobFinished(
                        job.completedAt, WatchEvent.JobIdentifier(runId, job.id), job.conclusion
                    )
                    emitIfNew(e, processed, out, meta.branch, meta.sha)
                    latestCompletion = maxInstant(latestCompletion, job.completedAt)
                } else {
                    anyJobIncomplete = true
                }

                // Steps
                for (step in job.steps) {
                    step.startedAt?.let {
                        val event = WatchEvent.StepStarted(
                            it, WatchEvent.StepIdentifier(
                                job = WatchEvent.JobIdentifier(runId, job.id),
                                stepNumber = step.number,
                            ), step.name
                        )
                        emitIfNew(event, processed, out, meta.branch, meta.sha)
                    }
                    if (step.status == StepStatus.COMPLETED && step.completedAt != null) {
                        val event = WatchEvent.StepFinished(
                            step.completedAt, WatchEvent.StepIdentifier(
                                job = WatchEvent.JobIdentifier(runId, job.id),
                                stepNumber = step.number,
                            ), step.conclusion
                                ?: throw IllegalStateException("No conclusion for step ${step.number}")
                        )
                        emitIfNew(event, processed, out, meta.branch, meta.sha)
                        latestCompletion = maxInstant(latestCompletion, step.completedAt)
                    }
                }
            }

            if (anyJobIncomplete) {
                stillOpen[runId] = meta
            }
        }

        val oldestOpenCreated = stillOpen.values.minOfOrNull { it.createdAt }
        val candidate = minOf(
            now.minus(safetyDelay),
            oldestOpenCreated?.minus(safetyDelay) ?: now.minus(safetyDelay),
        )
        val newStable = maxOf(state.stableScannedTo, candidate)

        val pruneBefore = maxOf(state.bootstrapInstant, newStable.minus(dedupeRetention))
        val prunedProcessed = processed.filterValues { it >= pruneBefore }

        val nextState = state.copy(
            stableScannedTo = newStable,
            openRuns = stillOpen,
            processedEventIds = prunedProcessed,
        )
        return out to nextState
    }

    @OptIn(ExperimentalTime::class)
    private fun emitIfNew(
        event: WatchEvent,
        processed: MutableMap<String, Instant>,
        out: MutableList<WatchEventContext>,
        branch: String?,
        sha: String?,
    ) {
        val id = eventId(event)
        if (processed.containsKey(id)) return
        processed[id] = event.occurredAt
        out += WatchEventContext(event, branch, sha)
    }

    private fun eventId(e: WatchEvent): String = when (e) {
        is WatchEvent.RunQueued ->
            "runQueued:${e.runId}"
        is WatchEvent.JobStarted ->
            "jobStarted:${e.identifier.runId}:${e.identifier.jobId}"
        is WatchEvent.JobFinished ->
            "jobFinished:${e.identifier.runId}:${e.identifier.jobId}"
        is WatchEvent.StepStarted ->
            "stepStarted:${e.identifier.job.runId}:${e.identifier.job.jobId}:${e.identifier.stepNumber}"
        is WatchEvent.StepFinished ->
            "stepFinished:${e.identifier.job.runId}:${e.identifier.job.jobId}:${e.identifier.stepNumber}"
    }

    private fun maxInstant(a: Instant?, b: Instant?): Instant? = when {
        a == null -> b
        b == null -> a
        else -> if (a > b) a else b
    }
}
