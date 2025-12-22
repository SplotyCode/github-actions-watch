package de.scandurra.githubactionswatch.storage

import de.scandurra.githubactionswatch.client.RepoIdentifier

interface WatchCursorStore {
    suspend fun load(repoId: RepoIdentifier): WatchCursor?
    suspend fun save(repoId: RepoIdentifier, cursor: WatchCursor)
}
