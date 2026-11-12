package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

/**
 * Policy for automatic time-based key rotation in [createPlatformCryptoStack].
 */
data class KeyRotationConfig(
    val rotationPeriod: Duration = 90.days,
    val initialBackoff: Duration = 500.milliseconds,
    val maxRetries: Int = 3,
) {
    companion object {
        /** Default rotation policy (90-day rotation period). */
        val DEFAULT = KeyRotationConfig()
    }
}
