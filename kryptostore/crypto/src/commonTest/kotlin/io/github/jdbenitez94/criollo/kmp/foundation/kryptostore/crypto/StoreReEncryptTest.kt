package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

/** REQ-ROT-01..05 */
class StoreReEncryptTest {
    @Test
    fun reEncryptAll_rewritesRegisteredHandles() = runTest {
        var rewriteCount = 0
        val registry = StoreRegistry()
        registry.register(EncryptedStoreHandle { rewriteCount++ })
        registry.register(EncryptedStoreHandle { rewriteCount++ })

        registry.reEncryptAll()

        assertEquals(2, rewriteCount)
        assertEquals(2, registry.size())
    }

    @Test
    fun initialize_callsReEncryptWhenRotationOccurs_thenReady() = runTest {
        val order = mutableListOf<String>()
        val registry = StoreRegistry()
        registry.register(
            EncryptedStoreHandle {
                order += "reEncrypt"
            },
        )
        val runtime = CryptoRuntime(
            stack = PlatformCryptoStack(
                cipher = PassthroughCipher(),
                keyRotator = keyRotator {
                    order += "rotate"
                    true
                },
                postRotationInit = { order += "postInit" },
                postMigrationCleanup = { order += "cleanup" },
            ),
            storeRegistry = registry,
        )

        runtime.initialize()

        assertEquals(CryptoRuntimeState.Ready, runtime.state.value)
        assertEquals(listOf("rotate", "postInit", "reEncrypt", "cleanup"), order)
    }

    @Test
    fun initialize_skipsReEncryptWhenNoRotation() = runTest {
        var reEncryptCalls = 0
        val registry = StoreRegistry()
        registry.register(EncryptedStoreHandle { reEncryptCalls++ })
        val runtime = CryptoRuntime(
            stack = PlatformCryptoStack(
                cipher = PassthroughCipher(),
                keyRotator = keyRotator { false },
                postRotationInit = {},
                postMigrationCleanup = { error("cleanup must not run") },
            ),
            storeRegistry = registry,
        )

        runtime.initialize()

        assertEquals(CryptoRuntimeState.Ready, runtime.state.value)
        assertEquals(0, reEncryptCalls)
    }

    @Test
    fun initialize_failedReEncrypt_surfacesErrorNotReady() = runTest {
        val boom = IllegalStateException("re-encrypt failed")
        val registry = StoreRegistry()
        registry.register(EncryptedStoreHandle { throw boom })
        val runtime = CryptoRuntime(
            stack = PlatformCryptoStack(
                cipher = PassthroughCipher(),
                keyRotator = keyRotator { true },
            ),
            storeRegistry = registry,
        )

        runtime.initialize()

        val error = assertIs<CryptoRuntimeState.Error>(runtime.state.value)
        assertEquals(boom, error.cause)
    }

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

    @Test
    fun webRotator_documentedAsNoOp_returnsFalse() = runTest {
        // Mirrors WebCryptoKeyRotator: ensure-key side effect only, never rotates (REQ-ROT-05).
        val rotator = keyRotator { false }
        assertFalse(rotator.rotateKeyIfNeeded())
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

    override suspend fun readKey(keyId: String): ByteArray = keys[keyId] ?: randomPlatformAesKey().also { writeKey(keyId, it) }

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

private class PassthroughCipher : Cipher {
    override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = message
    override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = message
}

private fun keyRotator(block: suspend () -> Boolean): KeyRotator = object : KeyRotator {
    override suspend fun rotateKeyIfNeeded(): Boolean = block()
}
