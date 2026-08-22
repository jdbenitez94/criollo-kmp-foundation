package io.github.jdbenitez94.criollo.kmp.foundation.runtime

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test

class RunCatchingCancellableTest {
    @Test
    fun sync_success() {
        val result = runCatchingCancellable { "ok" }
        expectThat(result.isSuccess).isTrue()
        expectThat(result.getOrNull()).isEqualTo("ok")
    }

    @Test
    fun sync_failureBecomesResult() {
        val result = runCatchingCancellable { error("boom") }
        expectThat(result.isFailure).isTrue()
        expectThat(result.exceptionOrNull()?.message).isEqualTo("boom")
    }

    @Test
    fun sync_rethrowsCancellation() {
        expectThrows<CancellationException> {
            runCatchingCancellable {
                throw CancellationException("cancelled")
            }
        }
    }

    @Test
    fun suspend_success() = runTest {
        val result = suspendRunCatchingCancellable {
            delay(1)
            "ok"
        }
        expectThat(result.isSuccess).isTrue()
        expectThat(result.getOrNull()).isEqualTo("ok")
    }

    @Test
    fun suspend_failureBecomesResult() = runTest {
        val result = suspendRunCatchingCancellable {
            delay(1)
            error("boom")
        }
        expectThat(result.isFailure).isTrue()
        expectThat(result.exceptionOrNull()).isNotNull()
    }

    @Test
    fun suspend_rethrowsCancellation() = runTest {
        expectThrows<CancellationException> {
            suspendRunCatchingCancellable {
                throw CancellationException("cancelled")
            }
        }
    }

    @Test
    fun suspend_cancellationDuringDelayDoesNotCompleteBlock() = runTest {
        var completed = false
        val job = launch {
            suspendRunCatchingCancellable {
                delay(1_000)
                completed = true
                "never"
            }
        }
        job.cancel(CancellationException("stop"))
        job.join()
        expectThat(job.isCancelled).isTrue()
        expectThat(completed).isFalse()
    }

    @Test
    fun onFailureExceptCancellation_rethrowsCancellation() {
        val result: Result<String> = Result.failure(CancellationException("stop"))
        expectThrows<CancellationException> {
            result.onFailureExceptCancellation { error("should not handle cancellation") }
        }
    }

    @Test
    fun onFailureExceptCancellation_handlesOtherErrors() {
        var handled = false
        val result: Result<String> = Result.failure(IllegalStateException("x"))
        result.onFailureExceptCancellation { handled = true }
        expectThat(handled).isTrue()
        expectThat(result.exceptionOrNull()).isNotNull()
    }

    @Test
    fun onFailureOrRethrow_rethrowsMatchingType() {
        val result: Result<String> = Result.failure(IllegalArgumentException("bad"))
        expectThrows<IllegalArgumentException> {
            result.onFailureOrRethrow<IllegalArgumentException, String> { error("no") }
        }
    }

    @Test
    fun getOrElseCancellable_rethrowsCancellation() {
        val result: Result<String> = Result.failure(CancellationException("stop"))
        expectThrows<CancellationException> {
            result.getOrElseCancellable { "fallback" }
        }
    }

    @Test
    fun getOrElseCancellable_usesFallbackForOtherErrors() {
        val result: Result<String> = Result.failure(IllegalStateException("x"))
        expectThat(result.getOrElseCancellable { "fallback" }).isEqualTo("fallback")
    }

    @Test
    fun rethrowCancellation_passesThroughSuccess() {
        expectThat(Result.success(1).rethrowCancellation().getOrNull()).isEqualTo(1)
        expectThat(Result.success(1).rethrowCancellation().exceptionOrNull()).isNull()
    }
}
