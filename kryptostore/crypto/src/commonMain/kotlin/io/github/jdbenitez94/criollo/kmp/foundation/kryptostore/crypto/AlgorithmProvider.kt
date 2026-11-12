package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

/**
 * Lazily initialized holder for a platform cryptographic primitive (e.g. Tink `Aead`).
 */
interface AlgorithmProvider<T> {
    /**
     * Returns the initialized primitive, or throws [IllegalStateException] if [initialize] has not completed.
     */
    val algorithm: T
        get() = throw IllegalStateException("Algorithm has not been initialized yet.")

    /**
     * Initializes or refreshes the underlying cryptographic primitive.
     */
    suspend fun initialize()
}
