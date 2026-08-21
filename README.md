[![CI](https://github.com/jdbenitez94/criollo-kmp-foundation/actions/workflows/ci.yml/badge.svg?branch=main&event=push)](https://github.com/jdbenitez94/criollo-kmp-foundation/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/jdbenitez94/criollo-kmp-foundation)](https://github.com/jdbenitez94/criollo-kmp-foundation/releases)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/docs/releases.html)
[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin_Multiplatform-Platform_Support-%237F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/docs/multiplatform.html)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.jdbenitez94.criollo.kmp.foundation/coroutines.svg)](https://central.sonatype.com/artifact/io.github.jdbenitez94.criollo.kmp.foundation/coroutines)
[![codecov](https://codecov.io/gh/jdbenitez94/criollo-kmp-foundation/graph/badge.svg)](https://codecov.io/gh/jdbenitez94/criollo-kmp-foundation)
[![Codacy grade](https://app.codacy.com/project/badge/Grade/09897325adbd4047ab7fc603b46c5a97)](https://app.codacy.com/gh/jdbenitez94/criollo-kmp-foundation/dashboard)
[![Codacy coverage](https://app.codacy.com/project/badge/Coverage/09897325adbd4047ab7fc603b46c5a97)](https://app.codacy.com/gh/jdbenitez94/criollo-kmp-foundation/dashboard)
[![CodeQL](https://img.shields.io/github/actions/workflow/status/jdbenitez94/criollo-kmp-foundation/codeql.yml?branch=main&label=CodeQL)](https://github.com/jdbenitez94/criollo-kmp-foundation/actions/workflows/codeql.yml)
[![Docs](https://img.shields.io/github/deployments/jdbenitez94/criollo-kmp-foundation/github-pages?label=docs)](https://jdbenitez94.github.io/criollo-kmp-foundation/)
[![Open Worldwide Application Security Project (OWASP) Dependency Check](https://img.shields.io/badge/Security-OWASP_Scan-brightgreen)](https://github.com/jdbenitez94/criollo-kmp-foundation/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

# Criollo KMP Foundation

Kotlin Multiplatform building blocks for the Criollo architecture.

This release focuses on **keyed coroutine task management** (`TaskScope`): launch, skip, replace, and debounce side effects by key—without fire-and-forget `launch` calls. More infrastructure modules will land in this monorepo over time.

**Group:** `io.github.jdbenitez94.criollo.kmp.foundation`

## Artifacts

| Artifact | Maven name | Gradle project | Required? | Role |
| ---------- | ------------ | ---------------- | ----------- | ------ |
| Bill of Materials (BOM) | `bom` | `:bom` | Recommended | Aligns versions of all foundation modules |
| Core | `coroutines` | `:coroutines` | **Yes** | `TaskScope` registry (`TaskKey`, `TaskPolicy`, …) |
| ViewModel | `coroutines-viewmodel` | `:coroutines:viewmodel` | Optional | `by taskScope()` on `ViewModel` |
| Compose | `coroutines-compose` | `:coroutines:compose` | Optional | `rememberTaskScope()` in Composables |
| Tooling | `project-conventions` | `:project-conventions` | Optional | Gradle plugin to sync `.editorconfig` + Detekt configs |

Only `coroutines` is required. Pick ViewModel and/or Compose adapters when you want the convenience APIs; you can also construct `TaskScope(coroutineScope)` yourself.

Packages: `…foundation.coroutines` (+ `.viewmodel` / `.compose`).

## Install

```kotlin
dependencies {
    implementation(platform("io.github.jdbenitez94.criollo.kmp.foundation:bom:0.1.0"))
    implementation("io.github.jdbenitez94.criollo.kmp.foundation:coroutines")

    // Optional adapters — add what you use:
    implementation("io.github.jdbenitez94.criollo.kmp.foundation:coroutines-viewmodel")
    implementation("io.github.jdbenitez94.criollo.kmp.foundation:coroutines-compose")
}
```

`kotlinx-coroutines-core` is exposed transitively (`api`) from `coroutines`.

## Quick start

**ViewModel** (needs `coroutines-viewmodel`):

```kotlin
class SignInViewModel : ViewModel() {
    private val tasks by taskScope()

    fun onSubmit() {
        tasks.launch(TaskKey.of("auth.sign_in"), TaskPolicy.SkipIfActive) {
            // side effect
        }
    }
}
```

**Compose** (needs `coroutines-compose`):

```kotlin
@Composable
fun SignInScreen() {
    val tasks = rememberTaskScope()
    Button(
        onClick = {
            tasks.launch(TaskKey.of("auth.sign_in"), TaskPolicy.SkipIfActive) {
                // side effect
            }
        },
    ) { Text("Sign in") }
}
```

**Core only** (no adapter artifacts):

```kotlin
val tasks = TaskScope(viewModelScope) // or any CoroutineScope
tasks.launch(TaskKey.of("sync.refresh"), TaskPolicy.ReplaceActive) {
    // side effect
}
```

## Docs

Site (MkDocs + Dokka API HTML): [jdbenitez94.github.io/criollo-kmp-foundation](https://jdbenitez94.github.io/criollo-kmp-foundation/).

- [Managed tasks (`TaskScope`)](docs/managed-tasks.md) — policies, adapters, anti-patterns
- [Project conventions](docs/project-conventions.md) — sync shared style configs into consumer repos
- [Contributing](docs/contributing.md) — git hooks and local quality gate
- [Commit conventions](docs/commit-conventions.md) — Conventional Commits types/scopes
- [JS / Wasm webpack notes](docs/js-wasm.md) — `webpack.config.d` fallbacks
- [Adding a module](docs/adding-a-module.md) — checklist for new artifacts in this repo
- [Publishing](docs/publishing.md)
- [Config overview](config/README.md) — Detekt, Kover, Gradle cache encryption

## Build

```bash
./gradlew qualityCheck jvmLibraryTests
```

Requires **JDK 21** (Temurin) and an Android SDK (`local.properties` with `sdk.dir`).  
Point the IDE Gradle JDK at Temurin 21 so `-XX:+UseZGC` works.

## Local hooks

```bash
./gradlew installGitHooks
```

Sets `core.hooksPath` to [`gradle/hooks/`](gradle/hooks) (`pre-commit` format/check, `pre-push` full quality + JVM tests).  
`qualityCheck` runs `installGitHooks` automatically. Details: [contributing.md](docs/contributing.md).

## License

[MIT](LICENSE)
