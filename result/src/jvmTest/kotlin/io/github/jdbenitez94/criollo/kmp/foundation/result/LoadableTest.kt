package io.github.jdbenitez94.criollo.kmp.foundation.result

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import kotlin.coroutines.cancellation.CancellationException

class LoadableTest {
    @Test
    fun asLoadable_emitsIndeterminateLoadingThenProgressThenSuccess() = runTest {
        val values = flow {
            emit(LoadEvent.Progress(0.25f))
            emit(LoadEvent.Progress())
            emit(LoadEvent.Done("ok"))
        }.asLoadable().toList()

        expectThat(values).containsExactly(
            Loadable.Loading(),
            Loadable.Loading(0.25f),
            Loadable.Loading(null),
            Loadable.Success("ok"),
        )
    }

    @Test
    fun asLoadable_emitsLoadingThenError() = runTest {
        val boom = IllegalStateException("boom")
        val values = flow<LoadEvent<Int>> { throw boom }.asLoadable().toList()

        expectThat(values).containsExactly(
            Loadable.Loading(),
            Loadable.Error(boom),
        )
    }

    @Test
    fun asLoadable_rethrowsCancellationException() = runTest {
        expectThrows<CancellationException> {
            flow<LoadEvent<Int>> { throw CancellationException("stop") }.asLoadable().toList()
        }
    }

    @Test
    fun loading_rejectsOutOfRangeProgress() {
        expectThrows<IllegalArgumentException> { Loadable.Loading(1.5f) }
        expectThrows<IllegalArgumentException> { Loadable.Loading(-0.1f) }
    }

    @Test
    fun loading_acceptsBoundsAndNull() {
        expectThat(Loadable.Loading(0f).progress).isEqualTo(0f)
        expectThat(Loadable.Loading(1f).progress).isEqualTo(1f)
        expectThat(Loadable.Loading().progress).isNull()
    }

    @Test
    fun toResult_mapsAllStatesAndDropsProgress() {
        val boom = IllegalStateException("boom")
        expectThat(Loadable.Loading(0.5f).toResult()).isEqualTo(Result.Loading)
        expectThat(Loadable.Success(1).toResult()).isEqualTo(Result.Success(1))
        expectThat(Loadable.Error(boom).toResult()).isEqualTo(Result.Error(boom))
    }

    @Test
    fun toLoadable_mapsAllStates() {
        val boom = IllegalStateException("boom")
        expectThat(Result.Loading.toLoadable()).isEqualTo(Loadable.Loading())
        expectThat(Result.Success(1).toLoadable()).isEqualTo(Loadable.Success(1))
        expectThat(Result.Error(boom).toLoadable()).isEqualTo(Loadable.Error(boom))
    }
}
