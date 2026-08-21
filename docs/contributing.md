# Contributing

## Branch model (`dev` ↔ `main`, same tip)

| Branch | Role |
| -------- | ------ |
| `dev` | Integration + Central Portal **SNAPSHOT** publishes |
| `main` | Releases via release-please → Maven Central |

**Invariant:** after every push to `main`, CI resets `origin/dev` to that exact commit
([`sync-dev-to-main.yml`](../.github/workflows/sync-dev-to-main.yml)). Tips match; do not
rebase `dev` onto squash commits by hand.

Suggested loop:

1. Open feature PRs into **`dev`** (squash merge).
2. Promote with PR(s) **`dev` → `main`** (squash; prefer small PRs over one megapr).
3. Automation sets **`dev` = `main` tip**. Continue from there.

Do not leave long-lived unique history on `dev` after a promotion—the sync will discard it.

### Ruleset bypass + classic protection + `DEV_SYNC_TOKEN` (one-time)

The sync workflow force-updates `dev`.

1. **Ruleset** (strict): applies to **`main` only** (PR + required checks + no
   force-push). Owner User bypass kept for emergencies.
2. **Classic branch protection** on **`dev`**: no required PR/checks (so tip sync
   can force-update), **no deletions**, linear history, **force push only for the
   owner** (`bypassForcePushAllowances`). Feature work should still land via PRs into
   `dev` by convention; `main` remains the hard gate.
3. Create a PAT (or reuse `gh` login token with `repo` scope) and put it in
   gitignored `local.properties` as `devSyncToken=…`, then sync:

```bash
awk -F= '/^devSyncToken=/{print substr($0,index($0,"=")+1)}' local.properties \
  | gh secret set DEV_SYNC_TOKEN -R jdbenitez94/criollo-kmp-foundation
```

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
