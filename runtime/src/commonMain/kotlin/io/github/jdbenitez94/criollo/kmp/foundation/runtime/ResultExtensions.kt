package io.github.jdbenitez94.criollo.kmp.foundation.runtime

import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs [action] on failure unless the failure is of type [E], in which case it is rethrown.
 *
 * Useful when a [Result] may still contain a [CancellationException] (e.g. after raw [runCatching]).
 */
inline fun <reified E : Throwable, T> Result<T>.onFailureOrRethrow(action: (Throwable) -> Unit): Result<T> = onFailure { error ->
    if (error is E) throw error else action(error)
}

/**
 * Handles non-cancellation failures; rethrows [CancellationException] so the coroutine can cancel.
 *
 * Prefer producing the [Result] with [runCatchingCancellable] so cancellation never enters the [Result].
 */
inline fun <T> Result<T>.onFailureExceptCancellation(action: (Throwable) -> Unit): Result<T> = onFailureOrRethrow<CancellationException, T>(action)

/**
 * Like [Result.getOrElse], but rethrows [CancellationException] if present in the failure.
 */
inline fun <T> Result<T>.getOrElseCancellable(onFailure: (Throwable) -> T): T {
    rethrowCancellation()
    return getOrElse(onFailure)
}
