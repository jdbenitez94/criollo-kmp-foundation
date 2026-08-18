# Managed task scope (`TaskScope`)

Keyed coroutine management for ViewModels and Compose without fire-and-forget `launch` calls.

## Dependencies

```kotlin
implementation(platform("io.github.jdbenitez94.criollo.kmp.foundation:bom:<version>"))
api("io.github.jdbenitez94.criollo.kmp.foundation:coroutines")
// Optional adapters:
implementation("io.github.jdbenitez94.criollo.kmp.foundation:coroutines-viewmodel")
implementation("io.github.jdbenitez94.criollo.kmp.foundation:coroutines-compose")
```

`coroutines` is the only required library module. ViewModel and Compose adapters are optional.
`kotlinx-coroutines-core` is exposed transitively (`api`) from `coroutines`.

## When to use `TaskScope` vs Flow operators

| Situation | Prefer |
|-----------|--------|
| One-off actions (submit, sync, save on click) | `TaskScope` with `SkipIfActive` or `ReplaceActive` |
| Rapid user input coalesced into one network call | `Debounce` or `ReplaceActive` + inline `delay` |
| Continuous stream of values (search query, sensor) | `Flow.debounce` + `flatMapLatest` |
| State that drives UI directly | `StateFlow` / UI state, not task registry |

`TaskScope` solves **job registry** problems (duplicate submits, cancel-previous, debounced side effects). It does not replace reactive pipelines.

## Core types

| Type | Role |
|------|------|
| `TaskKey` | Stable task identifier (`TaskKey.of("feature.action")`) |
| `TaskPolicy` | `SkipIfActive`, `ReplaceActive`, `Debounce(Duration)` |
| `TaskLaunchResult` | `Started(handle)` or `Skipped` |
| `TaskHandle` | Cancel a started task without exposing raw `Job` |
| `TaskScope` | Registry bound to a parent `CoroutineScope` |
| `taskState(key)` | `StateFlow<Idle\|Running>` |

## ViewModel usage

```kotlin
import io.github.jdbenitez94.criollo.kmp.foundation.coroutines.TaskPolicy
import io.github.jdbenitez94.criollo.kmp.foundation.coroutines.viewmodel.taskScope

class SignInViewModel(...) : ViewModel() {
    private val tasks by taskScope()

    fun onSubmit() {
        tasks.launch(AppTaskKey.SignInSubmit.key, TaskPolicy.SkipIfActive) {
            // side effect
        }
    }
}
```

## Compose usage (no ViewModel)

```kotlin
import io.github.jdbenitez94.criollo.kmp.foundation.coroutines.compose.rememberTaskScope

@Composable
fun Example() {
    val tasks = rememberTaskScope()
    // …
}
```

`rememberTaskScope()` cancels all tasks on Composable dispose.

## Typed keys in the app layer

Define domain keys once in the consuming app:

```kotlin
sealed interface AppTaskKey {
    val key: TaskKey
    data object PersistSettings : AppTaskKey {
        override val key = TaskKey.of("app.persist_settings")
    }
}
```

Convention: `feature.action` strings to avoid collisions.

## Anti-patterns

- **Global singleton `TaskScope`** — tasks from different screens collide; use one scope per lifecycle owner.
- **`Job()` inside `viewModelScope.launch`** — breaks structured concurrency.
- **`TaskScope` for UI state** — use state holders / `StateFlow`; use tasks only for side effects.
