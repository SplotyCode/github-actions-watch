@file:OptIn(ExperimentalTime::class)

package de.scandurra.githubactionswatch.client

import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class RateLimitInformation(
    val remaining: Long,
    val resetAt: Instant?,
    val canceledBecauseOfRateLimit: Boolean
) {
    fun suggestWaitTime(clock: Clock = Clock.System): Duration {
        if (resetAt == null) return 20.milliseconds
        return (resetAt - clock.now()).coerceAtLeast(1.seconds)
    }
}

fun extractRateLimit(exception: ClientRequestException): RateLimitInformation {
    val response = exception.response
    /* https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api?apiVersion=2022-11-28 */
    val remaining = response.headers["X-RateLimit-Remaining"]?.toLongOrNull()
        ?: throw IllegalStateException("X-RateLimit-Remaining header missing from response", exception)
    val resetEpoch = response.headers["X-RateLimit-Reset"]?.toLongOrNull()
    val statuscodeCouldMeanRateLimited = response.status == HttpStatusCode.Forbidden
        || response.status == HttpStatusCode.TooManyRequests
    return RateLimitInformation(
        remaining,
        resetEpoch?.let { Instant.fromEpochSeconds(it) },
        statuscodeCouldMeanRateLimited && remaining == 0L
    )
}

suspend fun <T> requestWithRateLimitHandling(block: suspend () -> T): T {
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