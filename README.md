# GitHub Actions Watch

This is a Kotlin CLI tool that monitors GitHub Actions workflow activity for a repository and prints one line per event to stdout. It reports:

- New workflow runs being queued
- Jobs starting and finishing (with conclusion)
- Steps within jobs starting and finishing (success/failure)
- Branch names, commit SHAs, and timestamps
- Resumes from where it left off using a small on-disk state per repository

![screenshot](docs/img.png)

## State and resuming

State is stored in one json per repository (`.github-actions-watch/Owner__Repo.json` by default).
It remembers how far we’ve safely looked, which runs are still in flight (so they’ll be re-checked), and what events from the active runs we’ve already printed so you don’t see repeats.
Each poll intentionally looks a little bit back and avoids trusting the very latest moment, this was necessary to avoid missing events.

## Build and usage

### Requirements

- JDK 23 or newer
- GitHub Personal Access Token

### Run
```bash
./gradlew run --args "--repo owner/repo --token $GITHUB_TOKEN"
```

### Usage

github-actions-watch --repo owner/repo [options]

Options

- -r, --repo <owner/repo>    Required repository identifier
- -t, --token <token>        If omitted, the GITHUB_TOKEN env var is used
- --poll-seconds <int>       Polling interval in seconds (default: 10)
- --state-dir <path>         Directory for state files (default: .github-actions-watch)
- --request-timeout-ms <n>   GitHub API request timeout in ms (default: 5000)

Environment

- GITHUB_TOKEN  Used if --token is not provided

## License

MIT. See [LICENSE](LICENSE).
