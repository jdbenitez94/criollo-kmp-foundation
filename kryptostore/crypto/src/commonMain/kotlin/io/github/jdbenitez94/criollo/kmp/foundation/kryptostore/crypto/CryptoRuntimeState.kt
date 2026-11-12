package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

/**
 * Lifecycle state of [CryptoRuntime].
 */
sealed interface CryptoRuntimeState {
    /** Initial state before or during [CryptoRuntime.initialize]. */
    data object Initializing : CryptoRuntimeState

    /** Key rotation, post-init, and store re-encryption succeeded; [CryptoRuntime.cipher] is ready. */
    data object Ready : CryptoRuntimeState

    /** [CryptoRuntime.initialize] failed with [cause]; subsequent calls to `initialize()` will retry. */
    data class Error(val cause: Throwable) : CryptoRuntimeState
}
