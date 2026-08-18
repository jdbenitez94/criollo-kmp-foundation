# Project conventions plugin

Optional Gradle plugin that syncs shared style configuration into a consumer repository:

| File | Role |
|------|------|
| `.editorconfig` | KtLint / IDE indent, imports, trailing commas, line length 180 |
| `config/detekt/detekt.yml` | Detekt 1 policy (incl. `detekt-formatting`) |
| `config/detekt/detekt-v2.yml` | Detekt 2 policy |

Source of truth is this monorepo’s root `.editorconfig` and `config/detekt/*` (packaged into the
plugin JAR at publish time). Per-project `baseline.xml` is **not** shipped — baselines stay local.

## Coordinates

- Plugin id: `io.github.jdbenitez94.criollo.kmp.foundation.project-conventions`
- Artifact: `io.github.jdbenitez94.criollo.kmp.foundation:project-conventions:<version>`

## Consumer setup

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

// root build.gradle.kts
plugins {
    id("io.github.jdbenitez94.criollo.kmp.foundation.project-conventions") version "0.1.0"
}
```

```bash
./gradlew syncCriolloProjectConventions
./gradlew checkCriolloProjectConventions
```

### Extension

```kotlin
criolloProjectConventions {
    // targetRoot.convention(layout.projectDirectory)
    includeEditorConfig.set(true)
    includeDetektV1.set(true)
    includeDetektV2.set(true)
    // Opt in so `check` fails on drift after the first sync:
    attachCheckToLifecycle.set(true)
}
```

## Local dogfood (this repo)

```bash
./gradlew :project-conventions:publishToMavenLocal
./gradlew :project-conventions:test
```

## Scope notes

- `MatchingDeclarationName` is **off** (KMP `Type.android.kt` / `Type.wasmJs.kt` file names).
- No product-specific Detekt rule sets — only the shared foundation policy files.
- No shared `baseline.xml` sync (always project-local).
