package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

internal expect suspend fun aesGcmEncrypt(key: ByteArray, plaintext: ByteArray, associatedData: ByteArray?): ByteArray

internal expect suspend fun aesGcmDecrypt(key: ByteArray, ciphertext: ByteArray, associatedData: ByteArray?): ByteArray

internal class AesGcmCipher(private val keyProvider: suspend () -> ByteArray) : Cipher {
    override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?): ByteArray {
        val key = keyProvider()
        return aesGcmEncrypt(key, message, associatedData)
    }

    override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray {
        val key = keyProvider()
        return aesGcmDecrypt(key, message, associatedData)
    }
}
