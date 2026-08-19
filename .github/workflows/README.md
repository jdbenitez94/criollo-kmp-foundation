# GitHub Actions — criollo-kmp-foundation

CI for the Criollo KMP Foundation monorepo.

## Workflow: `ci.yml`

| Job | Purpose |
|-----|---------|
| `build` | `qualityCheck` + `jvmLibraryTests` + `koverXmlReport`; Codecov + Codacy coverage. Runs **in parallel** with security so an NVD timeout cannot skip tests. Static analysis on Codacy is the GitHub App check, not a CLI upload. |
| `security` | OWASP `dependencyCheckAnalyze` (`failBuildOnCVSS=7.0`). Dedicated Actions cache for the NVD DB (`OWASP_NVD_DIR`); skipped on Dependabot and fork PRs (no secrets). |

Required secrets: `CODECOV_TOKEN`, `CODACY_API_TOKEN` (or `CODACY_PROJECT_TOKEN`),
`GRADLE_ENCRYPTION_KEY`, `NVD_API_KEY` (see [`BADGES-SETUP.md`](../../BADGES-SETUP.md)).

Triggers: push and pull_request to **`main`** and **`dev`** (docs-only / markdown-only
changes are skipped via `paths-ignore`), matching Saveable.

### Gradle cache policy

| Ref | Cache |
|-----|--------|
| Push to `main` / `dev` | Read + **write** (seeds shared cache) |
| PRs into `main` / `dev`, `feature/*`, other branches, tags | **Read-only** |

NVD data is **not** stored in Gradle User Home. The security job uses a separate
`actions/cache` keyed by UTC day (`owasp-nvd-<os>-<date>`) and **saves on PRs**, so the
first daily download is slow and later runs reuse it.

GitHub Actions only restores Gradle caches from the **same branch**, the **PR base branch**, or the
**default branch**. Prefer opening feature work as PRs into `dev` so jobs reuse the `dev`
cache; after release squash, `main` also writes a cache line.

## Release automation

| Workflow | Trigger | Role |
|----------|---------|------|
| [`release-please.yml`](release-please.yml) | push to `main` | Conventional-commit release PR; on merge → tag `vX.Y.Z` + GitHub Release + Maven publish |
| [`release.yml`](release.yml) | push tag `v*` | Manual tag escape hatch → Maven publish (+ GitHub Release if missing) |
| [`publish-maven-central.yml`](publish-maven-central.yml) | `workflow_call` | Shared quality + Portal publish + finalize |

`dev` never tags or publishes. Version sources: `version.txt`, `ProjectConfig.version` (`x-release-please-version`), `.release-please-manifest.json`.

See [`docs/publishing.md`](../../docs/publishing.md).

Other workflows:

- [`codeql.yml`](codeql.yml) — Kotlin/Java static security analysis
- [`docs.yml`](docs.yml) — required **Docs** check (lychee + MkDocs + Dokka); deploys GitHub Pages from `main`

## Aggregated Gradle tasks

| Task | CI job |
|------|--------|
| `qualityCheck` | `ci.yml`, publish workflow (also runs `installGitHooks`) |
| `jvmLibraryTests` | `ci.yml`, publish workflow |
| `installGitHooks` | local DX / dependency of `qualityCheck` |
| `formatAndCheck` | local DX (`ktlintFormat` then `qualityCheck`) |
| `publishToMavenLocal` | root aggregator for `bom`, library modules, and `project-conventions` |

## Action pinning

Third-party actions use immutable commit SHAs with a version comment for humans, e.g. `actions/checkout@<sha> # v4`. Dependabot (`github-actions` ecosystem) opens weekly PRs to refresh pins.

## Secrets (release)

| Secret | Description |
|--------|-------------|
| `OSSRH_USERNAME` / `OSSRH_PASSWORD` | Central Publisher Portal **user token** (not legacy OSSRH) |
| `SIGNING_KEY_ID` / `SIGNING_KEY` / `SIGNING_PASSWORD` | In-memory PGP signing for publications |
| `CODECOV_TOKEN` | Codecov upload (required by `ci.yml`) |
| `CODACY_API_TOKEN` | Codacy account API token (preferred; analysis + coverage) |
| `CODACY_PROJECT_TOKEN` | Codacy project token (fallback if `CODACY_API_TOKEN` is unset) |
| `GRADLE_ENCRYPTION_KEY` | Encrypts Gradle configuration-cache entries in Actions cache (required for warm CC) |
| `NVD_API_KEY` | NIST NVD API key for OWASP Dependency Check (required; avoids multi-minute rate limits) |

## Variables (release)

| Variable | Description |
|----------|-------------|
| `MAVEN_CENTRAL_NAMESPACE` | Portal namespace for finalize step (e.g. `io.github.jdbenitez94`) |

Publish URL is the Portal OSSRH staging API
(`ossrh-staging-api.central.sonatype.com`). After Gradle upload, publish calls
`POST /manual/upload/defaultRepository/{namespace}` so the deployment is visible at
https://central.sonatype.com/publishing.

## Composite action: `setup-gradle-ci`

Injects CI-friendly settings into [`gradle.properties`](../../gradle.properties) (daemon off, reduced workers, memory cap). See [`action.yml`](../actions/setup-gradle-ci/action.yml).

## Composite action: `setup-android-sdk`

Uses the Ubuntu runner `ANDROID_HOME` (no third-party actions). Symlinks
`platforms/android-37` → `android-37.0` when the image only ships the dotted
layout, so AGP `compileSdk = 37` resolves. See [`action.yml`](../actions/setup-android-sdk/action.yml).

## Local parity

```bash
./gradlew installGitHooks
./gradlew qualityCheck jvmLibraryTests
```

See [`docs/contributing.md`](../../docs/contributing.md).
