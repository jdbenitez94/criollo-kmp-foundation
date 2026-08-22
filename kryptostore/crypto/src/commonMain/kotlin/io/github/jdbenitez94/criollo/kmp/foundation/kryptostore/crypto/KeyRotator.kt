package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

interface KeyRotator {
    suspend fun rotateKeyIfNeeded(): Boolean
}
