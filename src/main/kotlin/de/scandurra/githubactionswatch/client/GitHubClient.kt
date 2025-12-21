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
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

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
        val response: WorkflowRunsResponse = http.get("/repos/$owner/$repo/actions/runs?per_page=1").body()
        response.total_count
    }

    private suspend fun <T> requestWithRateLimitHandling(block: suspend () -> T): T {
        try {
            return block()
        } catch (exception: ClientRequestException) {
            val rateLimitInformation = extractRatelimit(exception)
            if (rateLimitInformation.canceledBecauseOfRateLimit && rateLimitInformation.resetAt != null) {
                delay(rateLimitInformation.suggestWaitTime())
                return block()
            }
            throw exception
        }
    }

    private fun extractRatelimit(e: ClientRequestException): RateLimitInformation {
        /* https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api?apiVersion=2022-11-28 */
        val remaining = e.response.headers["X-RateLimit-Remaining"]?.toLongOrNull()
            ?: throw IllegalStateException("X-RateLimit-Remaining header missing from response", e)
        val resetEpoch = e.response.headers["X-RateLimit-Reset"]?.toLongOrNull()
        val statuscodeCouldMeanRateLimited = e.response.status.value == 403 || e.response.status.value == 429
        return RateLimitInformation(
            remaining,
            resetEpoch?.let { Instant.fromEpochSeconds(it) },
            statuscodeCouldMeanRateLimited && remaining == 0L
        )
    }

    private data class RateLimitInformation(
        val remaining: Long,
        val resetAt: Instant?,
        val canceledBecauseOfRateLimit: Boolean
    ) {
        fun suggestWaitTime(clock: Clock = Clock.System): Duration {
            if (resetAt == null) return 20.milliseconds
            return (clock.now() - resetAt).coerceAtLeast(1.seconds)
        }
    }

    override fun close() = http.close()
}
