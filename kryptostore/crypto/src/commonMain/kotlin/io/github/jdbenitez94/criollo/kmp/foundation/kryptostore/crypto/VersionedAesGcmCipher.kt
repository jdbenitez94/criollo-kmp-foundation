package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * AES-GCM cipher that prefixes ciphertext with a versioned key-id header:
 * `[keyIdLength:1][keyId UTF-8][nonce+ciphertext]`.
 *
 * Legacy payloads without the header are decrypted using the legacy single-key account.
 */
internal class VersionedAesGcmCipher(private val store: SecureKeyStore) : Cipher {
    private val keyCacheMutex = Mutex()
    private val keyCache = mutableMapOf<String, ByteArray>()

    override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?): ByteArray {
        val activeKeyId = store.readActiveKeyId()
        val key = cachedKey(activeKeyId)
        val ciphertext = aesGcmEncrypt(key, message, associatedData)
        return encodeKeyIdHeader(activeKeyId) + ciphertext
    }

    override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray {
        val (keyId, payload) = decodeKeyIdHeader(message)
        val key = if (keyId != null) {
            cachedKey(keyId)
        } else {
            cachedLegacyKey()
        }
        return aesGcmDecrypt(key, payload, associatedData)
    }

    suspend fun clearKeyCache() {
        keyCacheMutex.withLock {
            keyCache.values.forEach { it.fill(0) }
            keyCache.clear()
        }
    }

    private suspend fun cachedKey(keyId: String): ByteArray = keyCacheMutex.withLock {
        keyCache.getOrPut(keyId) { store.readKey(keyId) }
    }

    private suspend fun cachedLegacyKey(): ByteArray = keyCacheMutex.withLock {
        keyCache.getOrPut(LEGACY_KEY_ID) { store.readKey() }
    }

    private fun encodeKeyIdHeader(keyId: String): ByteArray {
        val keyIdBytes = keyId.encodeToByteArray()
        require(keyIdBytes.size <= MAX_KEY_ID_LENGTH) { "Key id exceeds maximum length." }
        return byteArrayOf(keyIdBytes.size.toByte()) + keyIdBytes
    }

    private fun decodeKeyIdHeader(message: ByteArray): Pair<String?, ByteArray> {
        if (message.isEmpty()) return null to message
        val keyIdLength = message.first().toInt() and 0xFF
        if (keyIdLength == 0 || keyIdLength > MAX_KEY_ID_LENGTH || message.size < 1 + keyIdLength) {
            return null to message
        }
        val keyId = message.copyOfRange(1, 1 + keyIdLength).decodeToString()
        val payload = message.copyOfRange(1 + keyIdLength, message.size)
        return keyId to payload
    }

    companion object {
        private const val MAX_KEY_ID_LENGTH = 64
        internal const val LEGACY_KEY_ID = "__legacy__"
    }
}
