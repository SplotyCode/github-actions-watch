package de.scandurra.githubactionswatch.storage

import de.scandurra.githubactionswatch.client.RepoIdentifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class FileWatchStateStore(
    private val rootDir: Path,
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true }
) : WatchCursorStore {
    private fun fileFor(repo: RepoIdentifier): Path =
        rootDir.resolve("${repo.owner}__${repo.repo}.json")

    override suspend fun load(repoId: RepoIdentifier): WatchCursor? = withContext(Dispatchers.IO) {
        val file = fileFor(repoId)
        if (!file.exists()) return@withContext null
        json.decodeFromString(WatchCursor.serializer(), file.readText())
    }

    override suspend fun save(repoId: RepoIdentifier, cursor: WatchCursor) = withContext(Dispatchers.IO) {
        rootDir.createDirectories()
        val file = fileFor(repoId)
        file.writeText(json.encodeToString(WatchCursor.serializer(), cursor))
    }
}
