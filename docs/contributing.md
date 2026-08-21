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

### Hard ruleset + Deploy Key bypass (one-time)

`main` and `dev` share the **Protect main and `dev`** ruleset (PR required, Docs + Codacy
checks, no force-push, no deletion, linear history). Humans stay fully gated.

Sync uses a **write Deploy Key** (`dev-tip-sync`) with ruleset **DeployKey** bypass
(always). Classic branch protection on `dev` stays **off**: it blocks Deploy Key pushes
even when the ruleset would bypass. The ruleset alone is the hard gate for `dev`.

Owner **User** bypass remains for emergencies.

Setup:

1. Generate an ed25519 key pair (no passphrase).
2. Add the **public** key as a repo Deploy Key with **Allow write access** (title
   `dev-tip-sync`).
3. Store the **private** key as Actions secret **`DEV_SYNC_SSH_KEY`**.
4. Optional local mirror (gitignored): keep the private key under `.tmp/dev-sync` and
   note `devSyncSshKeyPath=.tmp/dev-sync` in `local.properties`.

```bash
gh secret set DEV_SYNC_SSH_KEY -R jdbenitez94/criollo-kmp-foundation < /path/to/dev-sync
```

(`DEV_SYNC_TOKEN` / `devSyncToken` is unused for tip sync; prefer the Deploy Key.)

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
