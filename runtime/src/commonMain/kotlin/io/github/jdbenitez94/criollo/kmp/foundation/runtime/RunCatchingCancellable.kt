@file:Suppress("TooGenericExceptionCaught")

package io.github.jdbenitez94.criollo.kmp.foundation.runtime

import kotlin.coroutines.cancellation.CancellationException

/**
 * Like [runCatching], but rethrows [CancellationException] so structured concurrency is preserved.
 *
 * Prefer this over [runCatching] for any block that may run in a cancellable coroutine context
 * (including non-suspend helpers called from suspend code).
 *
 * Non-[CancellationException] failures (including [Error]) become [Result.failure], matching
 * [suspendRunCatchingCancellable].
 *
 * For suspend blocks, use [suspendRunCatchingCancellable] (separate name avoids overload ambiguity).
 */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = resultOfCancellable(block)

/**
 * Suspend variant of [runCatchingCancellable]. Use instead of [runCatching] around suspend work.
 *
 * Non-[CancellationException] failures (including [Error]) become [Result.failure].
 */
suspend inline fun <T> suspendRunCatchingCancellable(block: suspend () -> T): Result<T> =
    resultOfCancellable { block() }

/**
 * If this [Result] is a failure caused by [CancellationException], rethrows it; otherwise returns this.
 */
fun <T> Result<T>.rethrowCancellation(): Result<T> {
    exceptionOrNull()?.let { error ->
        if (error is CancellationException) throw error
    }
    return this
}

@PublishedApi
internal inline fun <T> resultOfCancellable(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Throwable) {
    Result.failure(error)
}
