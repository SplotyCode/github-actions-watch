package de.scandurra.githubactionswatch.cli

import de.scandurra.githubactionswatch.client.RepoIdentifier
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required

data class CliConfig(
    val repo: RepoIdentifier,
    val token: String,
    val pollSeconds: Int,
    val stateDir: String,
    val requestTimeoutMs: Int,
)

fun parseCli(args: Array<String>): CliConfig {
    val parser = ArgParser("github-actions-watch")

    val repoArg by parser.option(
        ArgType.String,
        shortName = "r",
        fullName = "repo",
        description = "Repository identifier in the form owner/repo"
    ).required()

    val tokenArg by parser.option(
        ArgType.String,
        shortName = "t",
        fullName = "token",
        description = "GitHub token. If not set, the GITHUB_TOKEN environment variable is used"
    )

    val pollSeconds by parser.option(
        ArgType.Int,
        fullName = "poll-seconds",
        description = "Polling interval in seconds"
    ).default(10)

    val stateDir by parser.option(
        ArgType.String,
        fullName = "state-dir",
        description = "Directory to store watcher cursor state files"
    ).default(".github-actions-watch")

    val requestTimeoutMs by parser.option(
        ArgType.Int,
        fullName = "request-timeout-ms",
        description = "GitHub API request timeout in milliseconds"
    ).default(5_000)

    parser.parse(args)

    val token = tokenArg ?: System.getenv("GITHUB_TOKEN")
    require(!token.isNullOrBlank()) {
        "GitHub token not provided. Use --token or set GITHUB_TOKEN env var."
    }

    val repo = parseRepo(repoArg)

    return CliConfig(
        repo = repo,
        token = token,
        pollSeconds = pollSeconds,
        stateDir = stateDir,
        requestTimeoutMs = requestTimeoutMs,
    )
}

private fun parseRepo(input: String): RepoIdentifier {
    val parts = input.split('/')
    require(parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
        "Invalid repo format '$input'. Expected owner/repo"
    }
    return RepoIdentifier(parts[0], parts[1])
}
