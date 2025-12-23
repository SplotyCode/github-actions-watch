package de.scandurra.githubactionswatch.client

import kotlinx.coroutines.flow.Flow

interface GitHubActionsClient : AutoCloseable {
    fun listAllWorkflowRuns(
        repoId: RepoIdentifier,
        startPage: Int = 1,
        perPage: Int = 100,
        created: TimeRange? = null,
        maxPages: Int = 50,
    ): Flow<WorkflowRun>

    fun listJobsForWorkflowRun(
        repoId: RepoIdentifier,
        runId: Long,
        startPage: Int = 1,
        perPage: Int = 100,
        maxPages: Int = 50,
    ): Flow<WorkflowJob>
}
