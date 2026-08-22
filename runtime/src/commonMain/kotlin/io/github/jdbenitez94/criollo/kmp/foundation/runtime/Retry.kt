package io.github.jdbenitez94.criollo.kmp.foundation.runtime

import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToLong
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Policy for [retryWithBackoff].
 *
 * Not a `data class`: [shouldRetry] is excluded from [equals]/[hashCode] so two policies with the
 * same numeric settings compare equal regardless of predicate identity.
 *
 * @param maxAttempts Total tries including the first (must be ≥ 1).
 * @param initialBackoff Delay before the second attempt.
 * @param maxBackoff Cap for exponential backoff growth.
 * @param jitterFactor Fraction of the current backoff used as ± jitter (0 disables jitter).
 * @param shouldRetry Return false to stop retrying and rethrow immediately (after the failed attempt).
 *   [CancellationException] is always rethrown and never retried.
 */
class RetryPolicy(
    val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    val initialBackoff: Duration = DEFAULT_INITIAL_BACKOFF,
    val maxBackoff: Duration = DEFAULT_MAX_BACKOFF,
    val jitterFactor: Double = DEFAULT_JITTER_FACTOR,
    val shouldRetry: (Exception) -> Boolean = { true },
) {
    init {
        require(maxAttempts >= 1) { "maxAttempts must be >= 1 (was $maxAttempts)" }
        require(initialBackoff.isPositive()) { "initialBackoff must be positive (was $initialBackoff)" }
        require(maxBackoff >= initialBackoff) {
            "maxBackoff ($maxBackoff) must be >= initialBackoff ($initialBackoff)"
        }
        require(jitterFactor in 0.0..1.0) { "jitterFactor must be in 0.0..1.0 (was $jitterFactor)" }
    }

    fun copy(
        maxAttempts: Int = this.maxAttempts,
        initialBackoff: Duration = this.initialBackoff,
        maxBackoff: Duration = this.maxBackoff,
        jitterFactor: Double = this.jitterFactor,
        shouldRetry: (Exception) -> Boolean = this.shouldRetry,
    ): RetryPolicy = RetryPolicy(
        maxAttempts = maxAttempts,
        initialBackoff = initialBackoff,
        maxBackoff = maxBackoff,
        jitterFactor = jitterFactor,
        shouldRetry = shouldRetry,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as RetryPolicy
        return maxAttempts == other.maxAttempts &&
            initialBackoff == other.initialBackoff &&
            maxBackoff == other.maxBackoff &&
            jitterFactor == other.jitterFactor
    }

    override fun hashCode(): Int {
        var result = maxAttempts
        result = 31 * result + initialBackoff.hashCode()
        result = 31 * result + maxBackoff.hashCode()
        result = 31 * result + jitterFactor.hashCode()
        return result
    }

    override fun toString(): String = "RetryPolicy(maxAttempts=$maxAttempts, initialBackoff=$initialBackoff, " +
        "maxBackoff=$maxBackoff, jitterFactor=$jitterFactor)"

    companion object {
        const val DEFAULT_MAX_ATTEMPTS = 3
        val DEFAULT_INITIAL_BACKOFF: Duration = 100.milliseconds
        val DEFAULT_MAX_BACKOFF: Duration = 30.seconds
        const val DEFAULT_JITTER_FACTOR = 0.1

        val DEFAULT = RetryPolicy()
    }
}

/**
 * Runs [block], retrying on failure with exponential backoff and optional jitter.
 *
 * Attempt indices passed to [block] are 1-based (`1` = first try).
 * [CancellationException] is never retried.
 */
suspend fun <T> retryWithBackoff(policy: RetryPolicy = RetryPolicy.DEFAULT, random: Random = Random.Default, block: suspend (attempt: Int) -> T): T {
    var attempt = 1
    var currentBackoff = policy.initialBackoff
    var lastError: Exception? = null

    while (attempt <= policy.maxAttempts) {
        try {
            return block(attempt)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            lastError = error
            val retriesLeft = attempt < policy.maxAttempts
            if (!retriesLeft || !policy.shouldRetry(error)) {
                throw error
            }
            delay(backoffWithJitter(currentBackoff, policy.jitterFactor, random))
            currentBackoff = (currentBackoff * 2).coerceAtMost(policy.maxBackoff)
            attempt++
        }
    }

    throw checkNotNull(lastError) { "retryWithBackoff exhausted without capturing an error" }
}

internal fun backoffWithJitter(backoff: Duration, jitterFactor: Double, random: Random): Duration {
    if (jitterFactor == 0.0) return backoff
    val baseMs = backoff.inWholeMilliseconds.coerceAtLeast(0L)
    val jitterMs = (baseMs * jitterFactor).roundToLong().coerceAtLeast(1L)
    val delta = random.nextLong(-jitterMs, jitterMs + 1)
    return (baseMs + delta).coerceAtLeast(0L).milliseconds
}
