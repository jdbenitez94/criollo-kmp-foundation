package io.github.jdbenitez94.criollo.kmp.foundation.result

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Async triad for UI / presentation layers: loading → success or error.
 *
 * Distinct from [kotlin.Result]; import this type explicitly when both are in scope.
 */
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>

    data class Error(val exception: Throwable) : Result<Nothing>

    data object Loading : Result<Nothing>
}

/**
 * Maps upstream emissions to [Result.Success], emits [Result.Loading] on start,
 * and [Result.Error] when the upstream fails.
 */
fun <T> Flow<T>.asResult(): Flow<Result<T>> = map<T, Result<T>> { Result.Success(it) }
    .onStart { emit(Result.Loading) }
    .catch { emit(Result.Error(it)) }
