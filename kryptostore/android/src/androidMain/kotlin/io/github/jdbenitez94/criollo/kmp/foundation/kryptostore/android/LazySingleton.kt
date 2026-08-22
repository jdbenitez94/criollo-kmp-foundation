package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.android

/**
 * Process-local lazy singleton used by Android Context property delegates (REQ-AND-01).
 * Android/JVM only — not used on Native/JS.
 */
internal class LazySingleton<T>(private val create: () -> T) {
    @Volatile
    private var instance: T? = null
    private val lock = Any()

    fun get(): T = instance ?: synchronized(lock) {
        instance ?: create().also { instance = it }
    }
}
