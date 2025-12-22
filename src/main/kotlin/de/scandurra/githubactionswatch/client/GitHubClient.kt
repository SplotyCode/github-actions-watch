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
): AutoCloseable {

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

    fun listAllWorkflowRuns(
        repoId: RepoIdentifier,
        startPage: Int = 1,
        perPage: Int = 100,
        created: TimeRange? = null,
        maxPages: Int = 50,
    ): Flow<WorkflowRun> = flow {
        var page = startPage
        while (page <= maxPages) {
            val response: WorkflowRunsResponse = requestWithRateLimitHandling {
                http.get("/repos/${repoId.owner}/${repoId.repo}/actions/runs") {
                    parameter("per_page", perPage)
                    parameter("page", page)
                    created?.let { parameter("created", it.toQueryParam()) }
                }.body()
            }

            if (response.workflowRuns.isEmpty()) return@flow
            response.workflowRuns.forEach { emit(it) }
            if (response.workflowRuns.size < perPage) return@flow
            page++
        }
    }

    suspend fun listJobsForWorkflowRun(
        repoId: RepoIdentifier,
        runId: Long,
        page: Int = 1,
        perPage: Int = 100,
    ): WorkflowJobsResponse = requestWithRateLimitHandling {
        http.get("/repos/${repoId.owner}/${repoId.repo}/actions/runs/$runId/jobs") {
            parameter("per_page", perPage)
            parameter("page", page)
        }.body()
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
