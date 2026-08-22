package io.github.jdbenitez94.criollo.kmp.foundation.runtime

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class RetryWithBackoffTest {
    @Test
    fun succeedsOnFirstAttempt() = runTest {
        var calls = 0
        val value = retryWithBackoff(RetryPolicy(maxAttempts = 3, jitterFactor = 0.0)) {
            calls++
            "ok"
        }
        expectThat(value).isEqualTo("ok")
        expectThat(calls).isEqualTo(1)
    }

    @Test
    fun retriesUntilSuccess() = runTest {
        var calls = 0
        val value = retryWithBackoff(
            policy = RetryPolicy(
                maxAttempts = 3,
                initialBackoff = 100.milliseconds,
                maxBackoff = 1_000.milliseconds,
                jitterFactor = 0.0,
            ),
            random = Random(0),
        ) { attempt ->
            calls++
            if (attempt < 3) error("fail-$attempt")
            "done"
        }
        expectThat(value).isEqualTo("done")
        expectThat(calls).isEqualTo(3)
    }

    @Test
    fun throwsAfterExhaustingAttempts() = runTest {
        var calls = 0
        expectThrows<IllegalStateException> {
            retryWithBackoff(
                policy = RetryPolicy(
                    maxAttempts = 2,
                    initialBackoff = 50.milliseconds,
                    maxBackoff = 50.milliseconds,
                    jitterFactor = 0.0,
                ),
            ) {
                calls++
                error("always")
            }
        }
        expectThat(calls).isEqualTo(2)
    }

    @Test
    fun doesNotRetryWhenShouldRetryIsFalse() = runTest {
        var calls = 0
        expectThrows<IllegalArgumentException> {
            retryWithBackoff(
                policy = RetryPolicy(
                    maxAttempts = 5,
                    shouldRetry = { false },
                ),
            ) {
                calls++
                throw IllegalArgumentException("no-retry")
            }
        }
        expectThat(calls).isEqualTo(1)
    }

    @Test
    fun doesNotRetryCancellation() = runTest {
        var calls = 0
        expectThrows<CancellationException> {
            retryWithBackoff(RetryPolicy(maxAttempts = 5)) {
                calls++
                throw CancellationException("cancelled")
            }
        }
        expectThat(calls).isEqualTo(1)
    }

    @Test
    fun backoffWithJitter_zeroFactorReturnsBase() {
        val base = 100.milliseconds
        expectThat(backoffWithJitter(base, 0.0, Random(1))).isEqualTo(base)
    }

    @Test
    fun retryPolicy_rejectsInvalidMaxAttempts() {
        expectThrows<IllegalArgumentException> {
            RetryPolicy(maxAttempts = 0)
        }
    }
}
