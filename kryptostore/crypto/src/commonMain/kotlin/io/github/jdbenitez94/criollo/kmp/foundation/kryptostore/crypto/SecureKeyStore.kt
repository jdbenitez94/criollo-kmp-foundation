package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
internal class SecureKeyRotator(
    private val store: SecureKeyStore,
    private val config: KeyRotationConfig = KeyRotationConfig.DEFAULT,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : KeyRotator {
    override suspend fun rotateKeyIfNeeded(): Boolean {
        val lastRotation = store.readLastRotationMillis()
        val now = nowMillis()
        if (lastRotation == 0L) {
            // First initialize: stamp without rotating (matches Android/JVM Tink rotators).
            store.writeLastRotationMillis(now)
            return false
        }
        if ((now - lastRotation) <= config.rotationPeriod.inWholeMilliseconds) {
            return false
        }
        val newKeyId = Uuid.random().toString()
        store.writeKey(newKeyId, randomPlatformAesKey())
        store.setActiveKeyId(newKeyId)
        store.writeLastRotationMillis(now)
        return true
    }
}

internal interface SecureKeyStore {
    /** Legacy single-key read (account `aes_key`). */
    suspend fun readKey(): ByteArray

    /** Legacy single-key write (account `aes_key`). */
    suspend fun writeKey(key: ByteArray)

    suspend fun readActiveKeyId(): String

    suspend fun setActiveKeyId(keyId: String)

    suspend fun readKey(keyId: String): ByteArray

    suspend fun writeKey(keyId: String, key: ByteArray)

    suspend fun listKeyIds(): List<String>

    suspend fun deleteKey(keyId: String)

    suspend fun readLastRotationMillis(): Long

    suspend fun writeLastRotationMillis(value: Long)
}

internal expect fun randomPlatformAesKey(): ByteArray

@OptIn(ExperimentalTime::class)
internal fun createAesPlatformStack(
    store: SecureKeyStore,
    rotationConfig: KeyRotationConfig = KeyRotationConfig.DEFAULT,
    nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
): PlatformCryptoStack {
    val versionedCipher = VersionedAesGcmCipher(store)
    val rotator = SecureKeyRotator(store, rotationConfig, nowMillis)
    return PlatformCryptoStack(
        cipher = versionedCipher,
        keyRotator = object : KeyRotator {
            override suspend fun rotateKeyIfNeeded(): Boolean {
                val rotated = rotator.rotateKeyIfNeeded()
                if (rotated) versionedCipher.clearKeyCache()
                return rotated
            }
        },
        postMigrationCleanup = {
            val activeKeyId = store.readActiveKeyId()
            store.listKeyIds()
                .filter { it != activeKeyId }
                .forEach { keyId -> store.deleteKey(keyId) }
            versionedCipher.clearKeyCache()
        },
    )
}
