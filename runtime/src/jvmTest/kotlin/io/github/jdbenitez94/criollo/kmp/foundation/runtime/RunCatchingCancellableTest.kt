package io.github.jdbenitez94.criollo.kmp.foundation.runtime

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isLessThanOrEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

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
    fun sync_wrapsErrorAsFailure() {
        val boom = OutOfMemoryError("simulated")
        val result = runCatchingCancellable { throw boom }
        expectThat(result.isFailure).isTrue()
        expectThat(result.exceptionOrNull()).isA<OutOfMemoryError>()
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
    fun suspend_wrapsErrorAsFailure() = runTest {
        val boom = OutOfMemoryError("simulated")
        val result = suspendRunCatchingCancellable {
            throw boom
        }
        expectThat(result.isFailure).isTrue()
        expectThat(result.exceptionOrNull()).isA<OutOfMemoryError>()
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
    fun onFailureOrRethrow_passesThroughSuccess() {
        var handled = false
        val result = Result.success("ok").onFailureOrRethrow<IllegalStateException, String> {
            handled = true
        }
        expectThat(handled).isFalse()
        expectThat(result.getOrNull()).isEqualTo("ok")
    }

    @Test
    fun onFailureOrRethrow_rethrowsMatchingType() {
        val result: Result<String> = Result.failure(IllegalArgumentException("bad"))
        expectThrows<IllegalArgumentException> {
            result.onFailureOrRethrow<IllegalArgumentException, String> { error("no") }
        }
    }

    @Test
    fun onFailureOrRethrow_handlesNonMatchingType() {
        var handled = false
        val result: Result<String> = Result.failure(IllegalStateException("x"))
        result.onFailureOrRethrow<IllegalArgumentException, String> { handled = true }
        expectThat(handled).isTrue()
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
    fun getOrElseCancellable_returnsSuccessValue() {
        expectThat(Result.success("ok").getOrElseCancellable { "fallback" }).isEqualTo("ok")
    }

    @Test
    fun rethrowCancellation_passesThroughSuccess() {
        expectThat(Result.success(1).rethrowCancellation().getOrNull()).isEqualTo(1)
        expectThat(Result.success(1).rethrowCancellation().exceptionOrNull()).isNull()
    }

    @Test
    fun rethrowCancellation_passesThroughNonCancellationFailure() {
        val result = Result.failure<Int>(IllegalStateException("x")).rethrowCancellation()
        expectThat(result.isFailure).isTrue()
    }
}

class RetryPolicyTest {
    @Test
    fun equals_ignoresShouldRetryIdentity() {
        val a = RetryPolicy(shouldRetry = { true })
        val b = RetryPolicy(shouldRetry = { false })
        expectThat(a).isEqualTo(b)
        expectThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test
    fun equals_considersNumericFields() {
        val a = RetryPolicy(maxAttempts = 2)
        val b = RetryPolicy(maxAttempts = 3)
        expectThat(a == b).isFalse()
    }

    @Test
    fun equals_considersInitialBackoff() {
        val a = RetryPolicy(initialBackoff = 100.milliseconds, maxBackoff = 1_000.milliseconds)
        val b = RetryPolicy(initialBackoff = 200.milliseconds, maxBackoff = 1_000.milliseconds)
        expectThat(a == b).isFalse()
    }

    @Test
    fun equals_considersMaxBackoff() {
        val a = RetryPolicy(initialBackoff = 100.milliseconds, maxBackoff = 500.milliseconds)
        val b = RetryPolicy(initialBackoff = 100.milliseconds, maxBackoff = 1_000.milliseconds)
        expectThat(a == b).isFalse()
    }

    @Test
    fun equals_considersJitterFactor() {
        val a = RetryPolicy(jitterFactor = 0.0)
        val b = RetryPolicy(jitterFactor = 1.0)
        expectThat(a == b).isFalse()
    }

    @Test
    fun acceptsBoundaryJitterFactors() {
        expectThat(RetryPolicy(jitterFactor = 0.0).jitterFactor).isEqualTo(0.0)
        expectThat(RetryPolicy(jitterFactor = 1.0).jitterFactor).isEqualTo(1.0)
    }

    @Test
    fun rejectsNegativeJitterFactor() {
        expectThrows<IllegalArgumentException> {
            RetryPolicy(jitterFactor = -0.1)
        }
    }

    @Test
    fun copy_overridesSelectedFields() {
        val base = RetryPolicy(maxAttempts = 2, jitterFactor = 0.0)
        val copied = base.copy(maxAttempts = 5)
        expectThat(copied.maxAttempts).isEqualTo(5)
        expectThat(copied.jitterFactor).isEqualTo(0.0)
        expectThat(copied.initialBackoff).isEqualTo(base.initialBackoff)
    }

    @Test
    fun copy_keepsMaxAttemptsWhenOnlyOtherFieldsChange() {
        val base = RetryPolicy(maxAttempts = 4, jitterFactor = 0.0)
        val copied = base.copy(jitterFactor = 0.2, shouldRetry = { false })
        expectThat(copied.maxAttempts).isEqualTo(4)
        expectThat(copied.jitterFactor).isEqualTo(0.2)
        expectThat(copied.shouldRetry(IllegalStateException())).isFalse()
    }

    @Test
    fun equals_sameInstance() {
        val policy = RetryPolicy()
        expectThat(policy == policy).isTrue()
    }

    @Test
    fun equals_rejectsNullAndOtherTypes() {
        val policy = RetryPolicy()
        @Suppress("EqualsNullCall")
        expectThat(policy.equals(null)).isFalse()
        expectThat(policy.equals("RetryPolicy")).isFalse()
    }

    @Test
    fun rejectsInvalidInitialBackoff() {
        expectThrows<IllegalArgumentException> {
            RetryPolicy(initialBackoff = 0.milliseconds)
        }
    }

    @Test
    fun rejectsMaxBackoffBelowInitial() {
        expectThrows<IllegalArgumentException> {
            RetryPolicy(
                initialBackoff = 100.milliseconds,
                maxBackoff = 50.milliseconds,
            )
        }
    }

    @Test
    fun rejectsInvalidJitterFactor() {
        expectThrows<IllegalArgumentException> {
            RetryPolicy(jitterFactor = 1.5)
        }
    }

    @Test
    fun toString_includesNumericFields() {
        val text = RetryPolicy(maxAttempts = 2, jitterFactor = 0.0).toString()
        expectThat(text.contains("maxAttempts=2")).isTrue()
        expectThat(text.contains("jitterFactor=0.0")).isTrue()
    }
}

class BackoffWithJitterTest {
    @Test
    fun jitterStaysWithinExpectedRange() {
        val base = 100.milliseconds
        val factor = 0.1
        val random = Random(42)
        repeat(50) {
            val slept = backoffWithJitter(base, factor, random)
            expectThat(slept.inWholeMilliseconds).isGreaterThanOrEqualTo(90)
            expectThat(slept.inWholeMilliseconds).isLessThanOrEqualTo(110)
        }
    }
}
