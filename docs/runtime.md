# Runtime helpers

Shared coroutine runtime utilities for Criollo KMP apps.

## Dependency

```kotlin
implementation(platform("io.github.jdbenitez94.criollo.kmp.foundation:bom:<version>"))
implementation("io.github.jdbenitez94.criollo.kmp.foundation:runtime")
```

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
