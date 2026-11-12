package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

/**
 * Evaluates whether the active encryption key is due for rotation and rotates it when needed.
 */
interface KeyRotator {
    /**
     * Rotates the active key if the configured rotation period has elapsed.
     *
     * @return `true` if a new primary/active key was promoted (triggering store re-encryption),
     * or `false` if no rotation occurred.
     */
    suspend fun rotateKeyIfNeeded(): Boolean
}
