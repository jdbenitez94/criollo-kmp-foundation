package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

/**
 * Authenticated encryption with associated data (AEAD) contract for KryptoStore payloads.
 *
 * Implementations must bind [associatedData] (when non-null) to the ciphertext so tampering
 * with either the ciphertext or store metadata fails authentication.
 */
interface Cipher {
    /**
     * Encrypts plaintext [message] with optional AEAD [associatedData].
     */
    suspend fun encrypt(message: ByteArray, associatedData: ByteArray? = null): ByteArray

    /**
     * Decrypts and authenticates ciphertext [message] with optional AEAD [associatedData].
     */
    suspend fun decrypt(message: ByteArray, associatedData: ByteArray? = null): ByteArray
}
