@file:OptIn(ExperimentalTime::class)

package de.scandurra.githubactionswatch.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.call.body
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val GITHUB_API_BASE = "https://api.github.com"
private const val GITHUB_API_VERSION = "2022-11-28"

class GitHubClient(
    token: String,
    requestTimeoutMillis: Long = 1000
): GitHubActionsClient {

    private val json = Json { ignoreUnknownKeys = true }
    private val http = HttpClient(CIO) {
        expectSuccess = true
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            this.requestTimeoutMillis = requestTimeoutMillis
        }
        /* does not only cache control but also etag handling via If-None-Match */
        install(HttpCache) {
            isShared = false
        }
        defaultRequest {
            url(GITHUB_API_BASE)
            header(HttpHeaders.Authorization, "Bearer $token")
            /* https://docs.github.com/en/rest/about-the-rest-api/api-versions?apiVersion=2022-11-28#specifying-an-api-version */
            header("X-GitHub-Api-Version", GITHUB_API_VERSION)
        }
    }

    override fun listAllWorkflowRuns(
        repoId: RepoIdentifier,
        startPage: Int,
        perPage: Int,
        created: TimeRange?,
        maxPages: Int,
    ): Flow<WorkflowRun> = pagedFlow(startPage, perPage, maxPages) { page, perPage ->
        val response: WorkflowRunsResponse = requestWithRateLimitHandling {
            http.get("/repos/${repoId.owner}/${repoId.repo}/actions/runs") {
                parameter("per_page", perPage)
                parameter("page", page)
                created?.let { parameter("created", it.toQueryParam()) }
            }.body()
        }
        response.workflowRuns
    }

    override fun listJobsForWorkflowRun(
        repoId: RepoIdentifier,
        runId: Long,
        startPage: Int,
        perPage: Int,
        maxPages: Int,
    ): Flow<WorkflowJob> = pagedFlow(startPage, perPage, maxPages) { page, perPage ->
        val response: WorkflowJobsResponse = requestWithRateLimitHandling {
            http.get("/repos/${repoId.owner}/${repoId.repo}/actions/runs/$runId/jobs") {
                parameter("per_page", perPage)
                parameter("page", page)
            }.body()
        }
        response.jobs
    }

    override fun close() = http.close()
}

data class RepoIdentifier(
    val owner: String,
    val repo: String,
)

data class TimeRange(
    val fromInclusive: Instant? = null,
    val toInclusive: Instant? = null,
)  {
    fun toQueryParam(): String {
        val from = fromInclusive?.toIsoString()
        val to = toInclusive?.toIsoString()
        return when {
            from != null && to != null -> "$from..$to"
            from != null -> ">=$from"
            to != null -> "<=$to"
            else -> error("Require fromInclusive and/or toInclusive")
        }
    }
}

private fun Instant.toIsoString(): String = this.toString()

private fun <T> pagedFlow(
    startPage: Int = 1,
    perPage: Int = 100,
    maxPages: Int = 50,
    fetchPage: suspend (page: Int, perPage: Int) -> List<T>
): Flow<T> = flow {
    var page = startPage
    while (page <= maxPages) {
        val items = fetchPage(page, perPage)
        if (items.isEmpty()) return@flow
        items.forEach { emit(it) }
        if (items.size < perPage) return@flow
        page++
    }
}
