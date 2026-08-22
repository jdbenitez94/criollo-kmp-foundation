package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

sealed interface CryptoRuntimeState {
    data object Initializing : CryptoRuntimeState
    data object Ready : CryptoRuntimeState
    data class Error(val cause: Throwable) : CryptoRuntimeState
}
