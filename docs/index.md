# Criollo KMP Foundation

Kotlin Multiplatform building blocks for the Criollo architecture.

This release focuses on **keyed coroutine task management** (`TaskScope`): launch, skip,
replace, and debounce side effects by key.

**Group:** `io.github.jdbenitez94.criollo.kmp.foundation`

## Artifacts

| Artifact | Maven name | Gradle project | Role |
|----------|------------|----------------|------|
| BOM | `bom` | `:bom` | Aligns versions of all foundation modules |
| Core | `coroutines` | `:coroutines` | `TaskScope` registry |
| ViewModel | `coroutines-viewmodel` | `:coroutines:viewmodel` | `by taskScope()` on `ViewModel` |
| Compose | `coroutines-compose` | `:coroutines:compose` | `rememberTaskScope()` in Composables |
| Tooling | `project-conventions` | `:project-conventions` | Gradle plugin to sync shared style configs |

## Guides

- [Managed tasks (`TaskScope`)](managed-tasks.md)
- [Project conventions](project-conventions.md)
- [Publishing](publishing.md)
- [API reference (Dokka)](api.md)

Source repository: [jdbenitez94/criollo-kmp-foundation](https://github.com/jdbenitez94/criollo-kmp-foundation).
