package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.android

/**
 * Process-local lazy singleton (JVM mirror of androidMain for unit tests).
 */
internal class LazySingleton<T>(private val create: () -> T) {
    @Volatile
    private var instance: T? = null
    private val lock = Any()

    fun get(): T = instance ?: synchronized(lock) {
        instance ?: create().also { instance = it }
    }
}
