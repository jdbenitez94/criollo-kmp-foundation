# Status and coverage badge setup

Manual steps for third-party badge integrations used in `README.md`.
CI **requires** Codecov and Codacy secrets. Quality fails if `CODECOV_TOKEN`
is missing or the Codecov upload errors. Codacy coverage stays best-effort.

Store tokens in gitignored `local.properties` and sync to GitHub Actions secrets
(never commit tokens).

| `local.properties` key | GitHub Actions secret |
| ------------------------------------------------- | ------------------------ |
| `codecovRepositoryToken` (or `codecovApiToken`) | `CODECOV_TOKEN` |
| `codacyApiToken` (or `codacyToken`) | `CODACY_API_TOKEN` (preferred) |
| `codacyProjectToken` | `CODACY_PROJECT_TOKEN` (fallback) |
| `gradleEncryptionKey` | `GRADLE_ENCRYPTION_KEY` |
| `nvdApiKey` | `NVD_API_KEY` |
| `devSyncToken` | `DEV_SYNC_TOKEN` (`dev` tip sync; owner PAT) |

Sync example (values never printed):

```bash
awk -F= '/^codecovRepositoryToken=/{print substr($0,index($0,"=")+1)}' local.properties \
  | gh secret set CODECOV_TOKEN -R jdbenitez94/criollo-kmp-foundation
awk -F= '/^codacyApiToken=/{print substr($0,index($0,"=")+1)}' local.properties \
  | gh secret set CODACY_API_TOKEN -R jdbenitez94/criollo-kmp-foundation
awk -F= '/^devSyncToken=/{print substr($0,index($0,"=")+1)}' local.properties \
  | gh secret set DEV_SYNC_TOKEN -R jdbenitez94/criollo-kmp-foundation
```

---

## 1. Code coverage (Codecov) — required

1. Sign in to [Codecov](https://codecov.io/) with GitHub.
2. Add **`jdbenitez94/criollo-kmp-foundation`** and open **Setup Repo**.
3. Copy the **Repository Upload Token**.
4. Put it in `local.properties` as `codecovRepositoryToken=…` and sync `CODECOV_TOKEN`.
5. CI uploads Kover XML as coverage and Gradle `TEST-*.xml` as Test Analytics
   (`report_type: test_results` on the same SHA-pinned `codecov-action` v7).
   Do not use floating `codecov-action@v5` or deprecated `test-results-action@v1`.
   Missing token or upload errors fail Quality.
6. Repo policy lives in [`codecov.yml`](codecov.yml) (project/patch target **70%**,
   aligned with Kover verify; ignores `build-logic` / `project-conventions` / docs).
   Validate with: `curl --data-binary @codecov.yml https://codecov.io/validate`

---

## 2. Code quality (Codacy) — required

1. Sign in to [Codacy](https://www.codacy.com/) with GitHub.
2. **Add organization** for the GitHub user `jdbenitez94`, then **Add Repository**
   **`criollo-kmp-foundation`**. Skip Segments if GitHub Custom Properties fail
   (personal GitHub accounts do not have them).
3. Project API token: Codacy repo → Settings → Integrations → Project API Token.
   Put it in `local.properties` as `codacyProjectToken=…` and sync `CODACY_PROJECT_TOKEN`.
   Optional: account API token as `CODACY_API_TOKEN` (CI prefers it when set).
4. Leave **Run analysis on your build server** **off**. The GitHub App already
   posts *Codacy Static Code Analysis* on PRs. Enabling that flag makes Codacy
   wait for a CLI upload we do not send.
5. CI uploads Kover XML via `codacy/codacy-coverage-reporter-action` with
   `language: Kotlin` and `force-coverage-parser: jacoco` (Kover’s Jacoco-format
   report is otherwise treated as Java and the coverage badge stays at 0%).

### Local parity (optional)

```bash
./gradlew localCloudParity
./gradlew localCloudParity -PlocalCloudParity.coverage=true
```

Markdownlint uses [`.markdownlint.json`](.markdownlint.json). Coverage mode runs
`jvmLibraryTests` + `koverXmlReport`, then best-effort Codecov/Codacy CLI uploads when
tokens are present in `local.properties`.

Grade badge (use Codacy-hosted URL; Shields `codacy/grade` often lags new repos):

`https://app.codacy.com/project/badge/Grade/09897325adbd4047ab7fc603b46c5a97`

(dashboard: <https://app.codacy.com/gh/jdbenitez94/criollo-kmp-foundation/dashboard>).

Coverage badge (optional in README):

`https://app.codacy.com/project/badge/Coverage/09897325adbd4047ab7fc603b46c5a97`

---

## 3. NVD API key (OWASP Dependency Check) — required

Without an NVD key, `dependencyCheckAnalyze` is rate-limited and can take a very long time.

1. Request a free key: <https://nvd.nist.gov/developers/request-an-api-key>
2. Confirm the email and copy the key.  
3. Put it in `local.properties` as `nvdApiKey=…` and sync `NVD_API_KEY`:

```bash
awk -F= '/^nvdApiKey=/{print substr($0,index($0,"=")+1)}' local.properties \
  | gh secret set NVD_API_KEY -R jdbenitez94/criollo-kmp-foundation
```

CI fails the security job if `NVD_API_KEY` is missing on maintainer PRs and pushes.
Dependabot and fork PRs skip that job (GitHub withholds Actions secrets). NVD data is
cached in Actions under `OWASP_NVD_DIR` (not Gradle User Home), including on PRs.

---

## 4. Automated badges (no extra accounts)

- **GitHub Actions CI** — `.github/workflows/ci.yml`
- **GitHub Release** — `img.shields.io/github/v/release/...`
- **Kotlin** — static badge; keep in sync with `gradle/libs.versions.toml` (`kotlin` version)
- **Kotlin Multiplatform** — static Shields.io badge
- **Maven Central** — dynamic Shields badge for latest `coroutines` under
  `io.github.jdbenitez94.criollo.kmp.foundation` (use a static `vX.Y.Z` badge only
  before the first Central index)
- **Codacy grade / coverage** — Codacy-hosted badge URLs (see section 2)
- **CodeQL** — `img.shields.io/github/actions/workflow/status/.../codeql.yml`
- **Docs (GitHub Pages)** — `img.shields.io/github/deployments/.../github-pages`
- **OWASP Dependency Check** — static badge; enforced in CI via
  `./gradlew dependencyCheckAnalyze` (`failBuildOnCVSS=7.0`)
- **MIT License** — static legal badge
