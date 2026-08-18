# build-logic

Composite Gradle build that publishes convention plugins for the Criollo KMP Foundation
monorepo: a root `build-logic` settings build with a `convention` subproject that compiles
and registers all plugins.

## Layout

```text
build-logic/
├── settings.gradle.kts      # include("convention"), version catalog from ../gradle/libs.versions.toml
├── gradle.properties        # memory / configuration-cache for the included build
└── convention/
    ├── build.gradle.kts     # kotlin-dsl + gradlePlugin { ... }
    └── src/main/kotlin/
        ├── *Plugin.kt       # convention plugins (default package, stable implementationClass names)
        └── io/github/jdbenitez94/criollo/kmp/foundation/buildlogic/
            ├── BuildLogicExtensions.kt   # libs + libsVersion()
            ├── SemVer.kt                 # validateSemVer()
            ├── ProjectConfig.kt          # identity, artifacts, publishing metadata
            └── KlibModuleNaming.kt       # unique klib module names + duplicate-name strategy
```

The main repo resolves these plugins via `pluginManagement { includeBuild("build-logic") }`
(and a second `includeBuild("build-logic")` for buildscript classpath) in `settings.gradle.kts`.

## Plugin naming

| Prefix         | Purpose                                                                            | Examples                                        |
|----------------|------------------------------------------------------------------------------------|-------------------------------------------------|
| `convention.*` | Reusable monorepo engineering (formatting, static analysis, coverage, root wiring) | `convention.ktlint`, `convention.kover.library` |
| `criollo.*`    | Product policy for this foundation (KMP library targets, Maven publish)            | `criollo.kmp-library`, `criollo.maven-publish`  |

Catalog aliases in [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) use kebab-case
(e.g. `convention-ktlint`), which Gradle exposes as dot-nested properties
(e.g. `libs.plugins.convention.ktlint`).

## Registered plugins

| Plugin ID                      | Catalog alias                    | Implementation                         |
|--------------------------------|----------------------------------|----------------------------------------|
| `convention.root`              | `convention-root`                | `RootPlugin`                           |
| `convention.kover.library`     | `convention-kover-library`       | `KoverLibraryConventionPlugin`         |
| `convention.kover.aggregation` | `convention-kover-aggregation`   | `KoverAggregationConventionPlugin`     |
| `convention.ktlint`            | `convention-ktlint`              | `KtLintConventionPlugin`               |
| `convention.detekt`            | `convention-detekt`              | `DetektConventionPlugin`               |
| `convention.dokka`             | `convention-dokka`               | `DokkaConventionPlugin`                |
| `criollo.kmp-library`          | `criollo-kmp-library`            | `CriolloKmpLibraryConventionPlugin`    |
| `criollo.maven-publish`        | `criollo-maven-publish`          | `MavenPublishConventionPlugin`         |

## Adding a new convention plugin

1. Create `convention/src/main/kotlin/MyConventionPlugin.kt` implementing `Plugin<Project>`.
2. Register it in `convention/build.gradle.kts` under `gradlePlugin { plugins { register(...) } }`.
3. Add an alias in `gradle/libs.versions.toml` (`convention.*` vs `criollo.*`).
4. Run `./gradlew :build-logic:convention:build` to compile and validate plugin metadata.

## Utilities

- **`libsVersion("alias")`** — reads a version from `gradle/libs.versions.toml`.
- **`ProjectConfig`** — monorepo identity and publishing metadata.
- **`validateSemVer(version)`** — fail-fast SemVer check (used by `RootPlugin` on `ProjectConfig.version`).

```kotlin
import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.ProjectConfig
import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.libsVersion
import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.validateSemVer
```

## Detekt dual-stack (v1 + v2)

`convention.detekt` defaults to **Detekt 2** (`dev.detekt`) with [`config/detekt/detekt-v2.yml`](../config/detekt/detekt-v2.yml).
If a project already applies `io.gitlab.arturbosch.detekt`, the convention configures **Detekt 1** with
[`config/detekt/detekt.yml`](../config/detekt/detekt.yml) instead.

Both share [`config/detekt/baseline.xml`](../config/detekt/baseline.xml).

- Detekt 1 still wires `detekt-formatting` (legacy path).
- Detekt 2 leaves formatting to `convention.ktlint` (no ktlint-wrapper in Detekt).
- KtLint engine version follows the Detekt line (`ktlint-engine` vs `ktlint-engine-legacy`).

## Verify locally

```bash
./gradlew :build-logic:convention:build
./gradlew qualityCheck jvmLibraryTests --configuration-cache --configuration-cache-problems=warn
```
