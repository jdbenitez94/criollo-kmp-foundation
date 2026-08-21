# Local configuration

## Detekt

Shared static-analysis policy lives under [`detekt/`](detekt/):

| File | Role |
| ------ | ------ |
| [`detekt/detekt.yml`](detekt/detekt.yml) | Detekt 1 (`io.gitlab.arturbosch.detekt`) |
| [`detekt/detekt-v2.yml`](detekt/detekt-v2.yml) | Detekt 2 (`dev.detekt`, default via `convention.detekt`) |
| [`detekt/baseline.xml`](detekt/baseline.xml) | Shared baseline (project-local suppressions) |

Consumer repos can sync these files with the [`project-conventions`](../docs/project-conventions.md) Gradle plugin.

## OWASP Dependency Check

[`owasp/suppressions.xml`](owasp/suppressions.xml) documents accepted CPE false positives
(Dokka/stdlib mapped to the Kotlin compiler CVE) and a dated suppression for
CVE-2026-53914 on Kotlin 2.4.10 compiler/KGP until 2.4.20 stable. Jackson on Dokka
classpaths is forced to a patched line in `DokkaConventionPlugin` rather than
suppressed.

NVD data: CI caches `OWASP_NVD_DIR`, seeds from the OWASP Builder datafeed, and
uses the API key only for deltas. `failOnError` is off so transient NVD 503s do
not fail the job; `failBuildOnCVSS=7.0` still gates real findings.

## YAML lint

[`.yamllint`](.yamllint) configures YAML lint for GitHub Actions workflows and other repo YAML when you run `yamllint` locally.

## Kover

Coverage aggregation and thresholds are documented in [`kover/README.md`](kover/README.md).

## Gradle build cache encryption (`GRADLE_ENCRYPTION_KEY`)

CI passes an encryption key to [`gradle/actions/setup-gradle`](https://github.com/gradle/actions)
via the GitHub Actions secret `GRADLE_ENCRYPTION_KEY`. **Do not commit this key to the repository.**

### GitHub Actions (required for encrypted remote cache)

1. Generate a 64-character hex key, for example:

   ```bash
   openssl rand -hex 32
   ```

2. Add it as repository secret **`GRADLE_ENCRYPTION_KEY`** (Settings → Secrets and variables → Actions).
3. If the previous key was ever committed, **rotate** the secret and invalidate old cache entries.

### Local development (optional)

To use the same encrypted cache locally, export the key before running Gradle:

```bash
export GRADLE_ENCRYPTION_KEY="<your-64-char-hex-key>"
./gradlew build
```

Or store it only on your machine (never commit):

```bash
# Example: ~/.gradle/gradle.properties (user home, not the repo)
gradle.encryptionKey=<your-64-char-hex-key>
```

The `gradle/actions/setup-gradle` action reads `cache-encryption-key`; local Gradle uses the
`gradle.encryptionKey` property when configured.
