package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

interface Cipher {
    suspend fun encrypt(message: ByteArray, associatedData: ByteArray? = null): ByteArray
    suspend fun decrypt(message: ByteArray, associatedData: ByteArray? = null): ByteArray
}
