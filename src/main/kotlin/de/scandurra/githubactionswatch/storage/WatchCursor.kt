@file:OptIn(ExperimentalTime::class)

package de.scandurra.githubactionswatch.storage

import de.scandurra.githubactionswatch.client.IsoInstantSerializer
import de.scandurra.githubactionswatch.client.RepoIdentifier
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class RunMeta(
    @Serializable(with = IsoInstantSerializer::class)
    val createdAt: Instant,
    val branch: String,
    val sha: String,
    val name: String? = null,
)

@OptIn(ExperimentalTime::class)
@Serializable
data class WatchCursor(
    @Serializable(with = IsoInstantSerializer::class)
    val bootstrapInstant: Instant,
    @Serializable(with = IsoInstantSerializer::class)
    val stableScannedTo: Instant,
    val openRuns: Map<Long, RunMeta> = emptyMap(),
    val processedEventIds: Map<String, @Serializable(with = IsoInstantSerializer::class) Instant> = emptyMap()
)
