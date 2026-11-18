# Flow Result

UI-oriented async triad for Kotlin Flows: **Loading → Success / Error**, plus
`Loadable<T>` for loads that report progress.

Distinct from `kotlin.Result`. Import `io.github.jdbenitez94.criollo.kmp.foundation.result.Result`
explicitly when both are in scope.

## Dependency

```kotlin
implementation(platform("io.github.jdbenitez94.criollo.kmp.foundation:bom:<version>"))
implementation("io.github.jdbenitez94.criollo.kmp.foundation:result")
```

## Usage

```kotlin
repository.observeItems()
    .asResult()
    .collect { result ->
        when (result) {
            is Result.Loading -> showSpinner()
            is Result.Success -> show(result.data)
            is Result.Error -> showError(result.exception)
        }
    }
```

`asResult()` emits `Loading` on collection start, maps each upstream value to `Success`,
and turns upstream failures into `Error` without cancelling the collector.

## Loadable (loads with progress)

Use `Loadable<T>` instead of `Result<T>` when the UI needs a progress bar (downloads,
uploads, long syncs). `Loading.progress` is a fraction in `0f..1f`, or `null` when
indeterminate (out-of-range values throw `IllegalArgumentException`).

The producer emits `LoadEvent`s; `asLoadable()` maps them:

```kotlin
fun download(url: String): Flow<LoadEvent<ByteArray>> = flow {
    emit(LoadEvent.Progress(0.5f))
    emit(LoadEvent.Done(bytes))
}

download(url)
    .asLoadable()
    .collect { state ->
        when (state) {
            is Loadable.Loading -> showProgress(state.progress) // null = indeterminate
            is Loadable.Success -> show(state.data)
            is Loadable.Error -> showError(state.exception)
        }
    }
```

| Upstream | Emitted |
| --- | --- |
| collection start | `Loadable.Loading()` (indeterminate) |
| `LoadEvent.Progress(f)` | `Loadable.Loading(f)` |
| `LoadEvent.Done(v)` | `Loadable.Success(v)` |
| failure | `Loadable.Error(e)` (`CancellationException` is rethrown) |

Interop: `Loadable.toResult()` (drops progress) and `Result.toLoadable()`
(`Loading` becomes indeterminate).
