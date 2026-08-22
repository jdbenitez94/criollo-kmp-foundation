package io.github.jdbenitez94.criollo.kmp.foundation.result

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import kotlin.coroutines.cancellation.CancellationException

class AsResultTest {
    @Test
    fun asResult_emitsLoadingThenSuccess() = runTest {
        val values = flow {
            emit(1)
            emit(2)
        }.asResult().toList()

        expectThat(values).containsExactly(
            Result.Loading,
            Result.Success(1),
            Result.Success(2),
        )
    }

    @Test
    fun asResult_emitsLoadingThenError() = runTest {
        val boom = IllegalStateException("boom")
        val values = flow<Int> {
            throw boom
        }.asResult().toList()

        expectThat(values).containsExactly(
            Result.Loading,
            Result.Error(boom),
        )
        expectThat(values[1]).isA<Result.Error>().and {
            get { exception }.isEqualTo(boom)
        }
    }

    @Test
    fun asResult_rethrowsCancellationException() = runTest {
        expectThrows<CancellationException> {
            flow<Int> {
                throw CancellationException("stop")
            }.asResult().toList()
        }
    }
}
