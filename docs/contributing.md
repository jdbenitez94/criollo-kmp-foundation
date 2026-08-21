# Contributing

## Local Git hooks

Repository hooks are installed when Gradle configures verification (via `installGitHooks`,
a dependency of `qualityCheck`). You can also run it manually:

```bash
./gradlew installGitHooks
```

This sets `core.hooksPath` to [`gradle/hooks/`](../gradle/hooks):

| Hook | What it runs |
| ------ | ---------------- |
| `pre-commit` | `ktlintFormat` (re-stages Kotlin files), `ktlintCheck`, `detekt` (non-blocking) |
| `pre-push` | `qualityCheck jvmLibraryTests` (configuration cache warn) |
| `pre-pr` | **Manual** (Git has no pre-pr event): `preparePullRequest` → `localCloudParity` |

Helpers (after `./gradlew installGitHooks`):

```bash
./bin/pr                         # pre-pr checks, then gh pr create
./bin/pr --draft                 # forwards args to gh
git pr                           # local git alias → ./bin/pr (this clone only)
PRE_PR_COVERAGE=true ./bin/pr    # optional coverage uploads
./gradlew preparePullRequest     # checks only (no gh)
```

Logs: `build/hooks/logs/`.

## Quality gate

```bash
./gradlew qualityCheck jvmLibraryTests
```

## Local cloud parity (optional)

Catch Codacy-style Markdown issues and duplication (jscpd), and optionally upload coverage
before opening a PR. Complexity is Detekt (`./gradlew detekt` / `qualityCheck`).

```bash
# Markdownlint + jscpd (uses .markdownlint.json / .jscpd.json; fails on findings)
./gradlew localCloudParity

# Also run JVM tests + koverXmlReport, then best-effort Codecov/Codacy uploads
./gradlew localCloudParity -PlocalCloudParity.coverage=true
```

Uploads need tokens in `local.properties` (`codecovRepositoryToken` / `codacyApiToken` /
`codacyProjectToken` — see [BADGES-SETUP.md](../BADGES-SETUP.md)). Missing tokens skip uploads
with a log line; upload failures never fail the task.

## Docs site (local)

```bash
python3 -m venv .venv
. .venv/bin/activate
pip install -r requirements-docs.txt
mkdocs serve
./gradlew dokkaGenerateHtml
```

See also [commit conventions](commit-conventions.md) and [adding a module](adding-a-module.md).
