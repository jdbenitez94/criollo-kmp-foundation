package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

data class KeyRotationConfig(val rotationPeriod: Duration = 90.days, val initialBackoff: Duration = 500.milliseconds, val maxRetries: Int = 3) {
    companion object {
        val DEFAULT = KeyRotationConfig()
    }
}
