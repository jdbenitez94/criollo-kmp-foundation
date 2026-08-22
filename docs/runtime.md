# Runtime helpers

Shared coroutine runtime utilities for Criollo KMP apps.

## Dependency

```kotlin
implementation(platform("io.github.jdbenitez94.criollo.kmp.foundation:bom:<version>"))
implementation("io.github.jdbenitez94.criollo.kmp.foundation:runtime")
```

## Cancellation-safe `runCatching`

Stdlib `runCatching` catches **all** exceptions, including `CancellationException`. In
coroutines that exception is control flow, not a failure: swallowing it breaks structured
concurrency (ViewModel work that never stops, delayed cleanup, silent hangs).

Use foundation wrappers instead:

```kotlin
val sync = runCatchingCancellable { parse(input) }
val async = suspendRunCatchingCancellable { api.fetch() }

async
    .onFailureExceptCancellation { log(it) }
    .getOrElseCancellable { fallback }
```

| API | Role |
| --- | --- |
| `runCatchingCancellable` | Non-suspend; rethrows `CancellationException` |
| `suspendRunCatchingCancellable` | Suspend; rethrows `CancellationException` |
| `Result.rethrowCancellation()` | Defensive if a `Result` might still hold cancellation |
| `onFailureOrRethrow<E>` | Handle failure unless it is type `E` (rethrow `E`) |
| `onFailureExceptCancellation` | Handle failures except cancellation |
| `getOrElseCancellable` | Fallback value without swallowing cancellation |

Non-[CancellationException] failures (including [Error] such as `OutOfMemoryError`) become
`Result.failure` in both sync and suspend wrappers.

Prefer producing `Result` with these wrappers so cancellation never enters the wrapper.
Keep chains short; for one-off local handling, plain `try/catch` with rethrow of cancellation is fine.
For resources, prefer Kotlin `use { }` over inventing a `finally` on `Result`.

`RetryPolicy` compares equal by numeric fields only (`shouldRetry` is ignored in `equals`/`hashCode`).

## Retry with backoff

`retryWithBackoff` runs a suspend block and retries transient failures with exponential
backoff and optional jitter. `CancellationException` is never retried.

```kotlin
val value = retryWithBackoff(
    policy = RetryPolicy(
        maxAttempts = 3,
        initialBackoff = 100.milliseconds,
        maxBackoff = 5.seconds,
    ),
) { attempt ->
    // attempt is 1-based
    api.fetch()
}
```

Use `shouldRetry` to skip permanent failures (for example validation errors) without waiting
for the full attempt budget.
