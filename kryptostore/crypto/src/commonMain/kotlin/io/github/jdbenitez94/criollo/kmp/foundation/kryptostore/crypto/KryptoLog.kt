package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

/**
 * Optional logging hook for KryptoStore. Default is a no-op so the library has no
 * hard dependency on app logging frameworks.
 */
fun interface KryptoLog {
    fun error(t: Throwable?, msg: () -> String)

    companion object {
        val NoOp: KryptoLog = KryptoLog { _, _ -> }
    }
}
