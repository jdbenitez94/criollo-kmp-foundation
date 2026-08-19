# Status and coverage badge setup

Manual steps for third-party badge integrations used in `README.md`.
CI **requires** Codecov and Codacy secrets to attempt uploads; coverage
providers must not fail the Quality job if the SaaS repo is not onboarded yet.

Store tokens in gitignored `local.properties` and sync to GitHub Actions secrets
(never commit tokens).

| `local.properties` key | GitHub Actions secret |
|------------------------|------------------------|
| `codecovToken` | `CODECOV_TOKEN` |
| `codacyApiToken` (or `codacyToken`) | `CODACY_API_TOKEN` (preferred) |
| `codacyProjectToken` | `CODACY_PROJECT_TOKEN` (fallback) |
| `gradleEncryptionKey` | `GRADLE_ENCRYPTION_KEY` |
| `nvdApiKey` | `NVD_API_KEY` |

Sync example (values never printed):

```bash
awk -F= '/^codecovToken=/{print substr($0,index($0,"=")+1)}' local.properties \
  | gh secret set CODECOV_TOKEN -R jdbenitez94/criollo-kmp-foundation
awk -F= '/^codacyApiToken=/{print substr($0,index($0,"=")+1)}' local.properties \
  | gh secret set CODACY_API_TOKEN -R jdbenitez94/criollo-kmp-foundation
```

---

## 1. Code coverage (Codecov) — required

1. Sign in to [Codecov](https://codecov.io/) with GitHub.
2. Add **`jdbenitez94/criollo-kmp-foundation`** and open **Setup Repo**.
3. Copy the **Repository Upload Token**.
4. Put it in `local.properties` as `codecovToken=…` and sync `CODECOV_TOKEN` (table above).
5. CI uploads `**/build/reports/kover/report.xml` after `koverXmlReport`.
   Upload errors (`Repository not found`) do not fail Quality.

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
5. CI uploads Kover XML via `codacy/codacy-coverage-reporter-action`.

Grade badge: Shields.io `codacy/grade/github/jdbenitez94/criollo-kmp-foundation`
(dashboard: https://app.codacy.com/gh/jdbenitez94/criollo-kmp-foundation/dashboard).

---

## 3. NVD API key (OWASP Dependency Check) — required

Without an NVD key, `dependencyCheckAnalyze` is rate-limited and can take a very long time.

1. Request a free key: https://nvd.nist.gov/developers/request-an-api-key  
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

* **GitHub Actions CI** — `.github/workflows/ci.yml`
* **Kotlin Multiplatform** — static Shields.io badge
* **Maven Central** — latest `coroutines` under
  `io.github.jdbenitez94.criollo.kmp.foundation` (after first publish)
* **OWASP Dependency Check** — static badge; enforced in CI via
  `./gradlew dependencyCheckAnalyze` (`failBuildOnCVSS=7.0`)
* **MIT License** — static legal badge
