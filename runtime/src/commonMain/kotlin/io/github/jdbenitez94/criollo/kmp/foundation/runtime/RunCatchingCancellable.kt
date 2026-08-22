package io.github.jdbenitez94.criollo.kmp.foundation.runtime

import kotlin.coroutines.cancellation.CancellationException

/**
 * Like [runCatching], but rethrows [CancellationException] so structured concurrency is preserved.
 *
 * Prefer this over [runCatching] for any block that may run in a cancellable coroutine context
 * (including non-suspend helpers called from suspend code).
 *
 * For suspend blocks, use [suspendRunCatchingCancellable] (separate name avoids overload ambiguity).
 */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = runCatching(block).rethrowCancellation()

/**
 * Suspend variant of [runCatchingCancellable]. Use instead of [runCatching] around suspend work.
 */
suspend inline fun <T> suspendRunCatchingCancellable(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Result.failure(error)
}

/**
 * If this [Result] is a failure caused by [CancellationException], rethrows it; otherwise returns this.
 */
fun <T> Result<T>.rethrowCancellation(): Result<T> {
    exceptionOrNull()?.let { error ->
        if (error is CancellationException) throw error
    }
    return this
}
