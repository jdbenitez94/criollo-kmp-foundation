package io.github.jdbenitez94.criollo.kmp.foundation.result

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlin.coroutines.cancellation.CancellationException

/**
 * Async state for long-running loads that can report progress (downloads, uploads, sync).
 *
 * Use [Result] when a plain spinner is enough; use [Loadable] when the UI needs a progress bar.
 */
sealed interface Loadable<out T> {
    /**
     * Load in flight. [progress] is a fraction in `0f..1f`, or `null` when indeterminate.
     */
    data class Loading(val progress: Float? = null) : Loadable<Nothing> {
        init {
            require(progress == null || progress in 0f..1f) {
                "progress must be in 0f..1f or null, was $progress"
            }
        }
    }

    data class Success<T>(val data: T) : Loadable<T>

    data class Error(val exception: Throwable) : Loadable<Nothing>
}

/**
 * Events emitted by a producer that reports progress before delivering its value.
 */
sealed interface LoadEvent<out T> {
    /** Intermediate progress. [fraction] in `0f..1f`, or `null` when indeterminate. */
    data class Progress(val fraction: Float? = null) : LoadEvent<Nothing>

    /** Final (or next) value. */
    data class Done<T>(val value: T) : LoadEvent<T>
}

/** Shorthand for a [Flow] of [LoadEvent]s, e.g. the return type of a download or upload producer. */
typealias LoadFlow<T> = Flow<LoadEvent<T>>

/**
 * Maps [LoadEvent]s to [Loadable]: emits [Loadable.Loading] (indeterminate) on start,
 * [LoadEvent.Progress] to [Loadable.Loading] with progress, [LoadEvent.Done] to [Loadable.Success],
 * and upstream failures to [Loadable.Error].
 *
 * [CancellationException] is rethrown so structured concurrency is preserved.
 */
fun <T> LoadFlow<T>.asLoadable(): Flow<Loadable<T>> = map<LoadEvent<T>, Loadable<T>> { event ->
    when (event) {
        is LoadEvent.Progress -> Loadable.Loading(event.fraction)
        is LoadEvent.Done -> Loadable.Success(event.value)
    }
}
    .onStart { emit(Loadable.Loading()) }
    .catch { cause ->
        if (cause is CancellationException) throw cause
        emit(Loadable.Error(cause))
    }

/** Converts to [Result]; progress information is dropped. */
fun <T> Loadable<T>.toResult(): Result<T> = when (this) {
    is Loadable.Loading -> Result.Loading
    is Loadable.Success -> Result.Success(data)
    is Loadable.Error -> Result.Error(exception)
}

/** Converts to [Loadable]; [Result.Loading] becomes an indeterminate [Loadable.Loading]. */
fun <T> Result<T>.toLoadable(): Loadable<T> = when (this) {
    is Result.Loading -> Loadable.Loading()
    is Result.Success -> Loadable.Success(data)
    is Result.Error -> Loadable.Error(exception)
}
