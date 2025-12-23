@file:OptIn(ExperimentalTime::class)

package de.scandurra.githubactionswatch.storage

import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class RunMeta(
    val createdAt: Instant
)

@OptIn(ExperimentalTime::class)
@Serializable
data class WatchCursor(
    val bootstrapInstant: Instant,
    val stableScannedTo: Instant,
    val openRuns: Map<Long, RunMeta> = emptyMap(),
    val processedEventIds: Map<String, Instant> = emptyMap()
)
