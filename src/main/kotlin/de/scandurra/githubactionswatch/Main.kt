package de.scandurra.githubactionswatch

import de.scandurra.githubactionswatch.cli.parseCli
import de.scandurra.githubactionswatch.cli.formatWatchEvent
import de.scandurra.githubactionswatch.client.GitHubClient
import de.scandurra.githubactionswatch.storage.FileWatchStateStore
import de.scandurra.githubactionswatch.watch.GitHubActionsWatcher
import kotlin.time.Duration.Companion.seconds
import java.nio.file.Path

suspend fun main(rawArgs: Array<String>) {
    val args = parseCli(rawArgs)
    GitHubClient(args.token, args.requestTimeoutMs.toLong()).use { client ->
        val store = FileWatchStateStore(Path.of(args.stateDir))
        val watcher = GitHubActionsWatcher(client, store)

        watcher.watch(args.repo, args.pollSeconds.seconds).collect { event ->
            println(formatWatchEvent(event, args.repo))
        }
    }
}
