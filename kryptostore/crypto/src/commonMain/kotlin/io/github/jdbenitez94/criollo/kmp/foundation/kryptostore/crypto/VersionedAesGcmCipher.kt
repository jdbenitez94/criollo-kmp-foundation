package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * AES-GCM cipher that prefixes ciphertext with a versioned key-id header
 * (see [KeyIdEnvelope]).
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
        return KeyIdEnvelope.encode(activeKeyId) + ciphertext
    }

    override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray {
        val parsed = runCatching { KeyIdEnvelope.decode(message) }.getOrNull()
        val key = if (parsed != null) {
            cachedKey(parsed.first)
        } else {
            cachedLegacyKey()
        }
        val payload = parsed?.second ?: message
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

    companion object {
        internal const val LEGACY_KEY_ID = "__legacy__"
    }
}
