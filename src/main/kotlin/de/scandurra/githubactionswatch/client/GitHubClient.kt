package de.scandurra.githubactionswatch.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime

private const val GITHUB_API_BASE = "https://api.github.com"
private const val GITHUB_API_VERSION = "2022-11-28"

@OptIn(ExperimentalTime::class)
class GitHubClient(
    token: String,
    requestTimeoutMillis: Long = 1000
): AutoCloseable {

    private val json = Json { ignoreUnknownKeys = true }
    private val http = HttpClient(CIO) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            this.requestTimeoutMillis = requestTimeoutMillis
        }
        defaultRequest {
            url(GITHUB_API_BASE)
            header(HttpHeaders.Authorization, "Bearer $token")
            /* https://docs.github.com/en/rest/about-the-rest-api/api-versions?apiVersion=2022-11-28#specifying-an-api-version */
            header("X-GitHub-Api-Version", GITHUB_API_VERSION)
        }
    }

    @Serializable
    private data class WorkflowRunsResponse(
        val total_count: Int
    )

    suspend fun getWorkflowRunsCount(owner: String, repo: String): Int = requestWithRateLimitHandling {
        val response: WorkflowRunsResponse = requestWithRateLimitHandling {
            http.get("/repos/$owner/$repo/actions/runs?per_page=1").body()
        }
        response.total_count
    }

    private suspend fun <T> requestWithRateLimitHandling(block: suspend () -> T): T {
        try {
            return block()
        } catch (exception: ClientRequestException) {
            val rateLimitInformation = extractRateLimit(exception)
            if (rateLimitInformation.canceledBecauseOfRateLimit) {
                delay(rateLimitInformation.suggestWaitTime())
                return block()
            }
            throw exception
        }
    }

    override fun close() = http.close()
}
