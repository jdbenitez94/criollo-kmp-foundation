package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

/** REQ-ROT-04 — SecureKeyRotator uses raw platform AES keys (non-web). */
class SecureKeyRotatorTest {
    @Test
    fun secureKeyRotator_honorsRotationPeriod_withFakeClock() = runTest {
        val store = InMemorySecureKeyStore()
        var now = 1_000L
        val rotator = SecureKeyRotator(
            store = store,
            config = KeyRotationConfig(rotationPeriod = 10.days),
            nowMillis = { now },
        )

        assertFalse(rotator.rotateKeyIfNeeded()) // stamp only
        assertFalse(rotator.rotateKeyIfNeeded()) // within period

        now += 10.days.inWholeMilliseconds + 1
        assertTrue(rotator.rotateKeyIfNeeded())
        assertFalse(rotator.rotateKeyIfNeeded()) // just rotated
    }
}

private class InMemorySecureKeyStore : SecureKeyStore {
    private var legacy: ByteArray? = null
    private var activeKeyId: String = VersionedAesGcmCipher.LEGACY_KEY_ID
    private val keys = mutableMapOf<String, ByteArray>()
    private var lastRotation = 0L
    private val index = mutableListOf<String>()

    override suspend fun readKey(): ByteArray = legacy ?: randomPlatformAesKey().also { writeKey(it) }

    override suspend fun writeKey(key: ByteArray) {
        legacy = key
        keys[VersionedAesGcmCipher.LEGACY_KEY_ID] = key
        if (VersionedAesGcmCipher.LEGACY_KEY_ID !in index) index += VersionedAesGcmCipher.LEGACY_KEY_ID
    }

    override suspend fun readActiveKeyId(): String = activeKeyId

    override suspend fun setActiveKeyId(keyId: String) {
        activeKeyId = keyId
        if (keyId !in index) index += keyId
    }

    override suspend fun readKey(keyId: String): ByteArray =
        keys[keyId] ?: randomPlatformAesKey().also { writeKey(keyId, it) }

    override suspend fun writeKey(keyId: String, key: ByteArray) {
        keys[keyId] = key
        if (keyId !in index) index += keyId
    }

    override suspend fun listKeyIds(): List<String> = index.toList()

    override suspend fun deleteKey(keyId: String) {
        keys.remove(keyId)
        index.remove(keyId)
    }

    override suspend fun readLastRotationMillis(): Long = lastRotation

    override suspend fun writeLastRotationMillis(value: Long) {
        lastRotation = value
    }
}
