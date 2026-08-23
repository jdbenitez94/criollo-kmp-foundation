@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import platform.Security.SecRandomCopyBytes

private class IosSecureKeyStore(appId: String) : SecureKeyStore {
    private val keychain = IosKeychain(service = "$appId.crypto")

    override suspend fun readKey(): ByteArray {
        val existing = keychain.read(LEGACY_KEY_ACCOUNT)
        return if (existing != null && existing.length > 0u) {
            existing.toByteArray()
        } else {
            randomPlatformAesKey().also { writeKey(it) }
        }
    }

    override suspend fun writeKey(key: ByteArray) {
        keychain.write(LEGACY_KEY_ACCOUNT, key.toNSData())
        addKeyIdToIndex(VersionedAesGcmCipher.LEGACY_KEY_ID)
    }

    override suspend fun readActiveKeyId(): String {
        val stored = keychain.read(ACTIVE_KEY_ID_ACCOUNT)?.toByteArray()?.decodeToString()
        if (!stored.isNullOrBlank()) return stored
        if (keychain.read(LEGACY_KEY_ACCOUNT) != null) {
            return VersionedAesGcmCipher.LEGACY_KEY_ID
        }
        val initialKeyId = VersionedAesGcmCipher.LEGACY_KEY_ID
        writeKey(randomPlatformAesKey())
        setActiveKeyId(initialKeyId)
        return initialKeyId
    }

    override suspend fun setActiveKeyId(keyId: String) {
        keychain.write(ACTIVE_KEY_ID_ACCOUNT, keyId.encodeToByteArray().toNSData())
        addKeyIdToIndex(keyId)
    }

    override suspend fun readKey(keyId: String): ByteArray {
        val account = accountForKeyId(keyId)
        val existing = keychain.read(account)
        return if (existing != null && existing.length > 0u) {
            existing.toByteArray()
        } else {
            randomPlatformAesKey().also { writeKey(keyId, it) }
        }
    }

    override suspend fun writeKey(keyId: String, key: ByteArray) {
        keychain.write(accountForKeyId(keyId), key.toNSData())
        addKeyIdToIndex(keyId)
    }

    override suspend fun listKeyIds(): List<String> = readKeyringIndex()

    override suspend fun deleteKey(keyId: String) {
        keychain.delete(accountForKeyId(keyId))
        removeKeyIdFromIndex(keyId)
    }

    override suspend fun readLastRotationMillis(): Long = keychain.read(ROTATION_ACCOUNT)?.toByteArray()?.decodeToString()?.toLongOrNull() ?: 0L

    override suspend fun writeLastRotationMillis(value: Long) {
        keychain.write(ROTATION_ACCOUNT, value.toString().encodeToByteArray().toNSData())
    }

    private fun accountForKeyId(keyId: String): String = when (keyId) {
        VersionedAesGcmCipher.LEGACY_KEY_ID -> LEGACY_KEY_ACCOUNT
        else -> "$KEY_ACCOUNT_PREFIX.$keyId"
    }

    private suspend fun readKeyringIndex(): List<String> {
        val raw = keychain.read(KEYRING_INDEX_ACCOUNT)?.toByteArray()?.decodeToString()
        if (raw.isNullOrBlank()) {
            return buildList {
                if (keychain.read(LEGACY_KEY_ACCOUNT) != null) add(VersionedAesGcmCipher.LEGACY_KEY_ID)
            }
        }
        return raw.split(KEYRING_SEPARATOR).filter { it.isNotBlank() }.distinct()
    }

    private suspend fun addKeyIdToIndex(keyId: String) {
        val updated = (readKeyringIndex() + keyId).distinct()
        keychain.write(
            KEYRING_INDEX_ACCOUNT,
            updated.joinToString(KEYRING_SEPARATOR).encodeToByteArray().toNSData(),
        )
    }

    private suspend fun removeKeyIdFromIndex(keyId: String) {
        val updated = readKeyringIndex().filterNot { it == keyId }
        if (updated.isEmpty()) {
            keychain.delete(KEYRING_INDEX_ACCOUNT)
        } else {
            keychain.write(
                KEYRING_INDEX_ACCOUNT,
                updated.joinToString(KEYRING_SEPARATOR).encodeToByteArray().toNSData(),
            )
        }
    }

    companion object {
        private const val LEGACY_KEY_ACCOUNT = "aes_key"
        private const val KEY_ACCOUNT_PREFIX = "aes_key"
        private const val ACTIVE_KEY_ID_ACCOUNT = "active_key_id"
        private const val KEYRING_INDEX_ACCOUNT = "keyring_index"
        private const val ROTATION_ACCOUNT = "last_rotation"
        private const val KEYRING_SEPARATOR = ","
    }
}

actual fun createPlatformCryptoStack(appId: String, rotationConfig: KeyRotationConfig): PlatformCryptoStack = createAesPlatformStack(IosSecureKeyStore(appId), rotationConfig)

internal actual fun randomPlatformAesKey(): ByteArray = ByteArray(32).also { bytes ->
    memScoped {
        SecRandomCopyBytes(null, 32u, bytes.usePinned { it.addressOf(0) })
    }
}

private suspend fun decodeAesKey(key: ByteArray): AES.GCM.Key = CryptographyProvider.Default.get(AES.GCM).keyDecoder()
    .decodeFromByteArray(AES.Key.Format.RAW, key)

internal actual suspend fun aesGcmEncrypt(key: ByteArray, plaintext: ByteArray, associatedData: ByteArray?): ByteArray {
    val cipher = decodeAesKey(key).cipher()
    return cipher.encrypt(
        plaintext = plaintext,
        associatedData = associatedData ?: ByteArray(0),
    )
}

internal actual suspend fun aesGcmDecrypt(key: ByteArray, ciphertext: ByteArray, associatedData: ByteArray?): ByteArray {
    val cipher = decodeAesKey(key).cipher()
    return cipher.decrypt(
        ciphertext = ciphertext,
        associatedData = associatedData ?: ByteArray(0),
    )
}
