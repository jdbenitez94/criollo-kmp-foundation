package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

interface AlgorithmProvider<T> {
    val algorithm: T
        get() = throw IllegalStateException("Algorithm has not been initialized yet.")

    suspend fun initialize()
}
