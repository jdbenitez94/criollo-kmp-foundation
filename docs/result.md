# Flow Result

UI-oriented async triad for Kotlin Flows: **Loading → Success / Error**.

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
