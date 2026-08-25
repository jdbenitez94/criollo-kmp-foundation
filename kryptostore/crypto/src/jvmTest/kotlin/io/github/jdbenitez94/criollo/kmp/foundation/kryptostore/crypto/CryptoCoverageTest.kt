package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class CryptoCoverageTest {
    @Test
    fun aesCipher_roundTrips_andCipherDefaultsPassNullAad() = runTest {
        var keyReads = 0
        val cipher: Cipher = AesGcmCipher {
            keyReads++
            ByteArray(32) { 7 }
        }
        val plaintext = "default-aad".encodeToByteArray()

        val encrypted = cipher.encrypt(plaintext)
        assertContentEquals(plaintext, cipher.decrypt(encrypted))
        assertEquals(2, keyReads)
        assertFailsWith<IllegalArgumentException> {
            aesGcmDecrypt(ByteArray(32), ByteArray(12), null)
        }
    }

    @Test
    fun versionedCipher_handlesCurrentLegacyMalformedAndEmptyPayloads() = runTest {
        val store = RecordingKeyStore()
        val currentKey = ByteArray(32) { 11 }
        val legacyKey = ByteArray(32) { 19 }
        store.keys["current"] = currentKey
        store.legacy = legacyKey
        store.activeId = "current"
        val cipher = VersionedAesGcmCipher(store)
        val plaintext = "versioned".encodeToByteArray()
        val aad = "metadata".encodeToByteArray()

        val current = cipher.encrypt(plaintext, aad)
        assertContentEquals(plaintext, cipher.decrypt(current, aad))
        assertEquals(1, store.namedReads)

        // AES-GCM ciphertext can randomly look like a versioned key-id header; keep
        // regenerating until decodeKeyIdHeader rejects it so decrypt takes the legacy path.
        var legacy = aesGcmEncrypt(legacyKey, plaintext, aad)
        while (true) {
            val keyIdLength = legacy.first().toInt() and 0xFF
            if (keyIdLength == 0 || keyIdLength > 64 || legacy.size < 1 + keyIdLength) break
            legacy = aesGcmEncrypt(legacyKey, plaintext, aad)
        }
        assertContentEquals(plaintext, cipher.decrypt(legacy, aad))
        assertEquals(1, store.legacyReads)

        assertFailsWith<IllegalArgumentException> { cipher.decrypt(byteArrayOf(), null) }
        assertFailsWith<IllegalArgumentException> { cipher.decrypt(byteArrayOf(0), null) }
        assertFailsWith<IllegalArgumentException> { cipher.decrypt(byteArrayOf(65), null) }
        assertFailsWith<IllegalArgumentException> { cipher.decrypt(byteArrayOf(4, 1), null) }

        cipher.clearKeyCache()
        cipher.encrypt(plaintext, aad)
        assertEquals(2, store.namedReads)
    }

    @Test
    fun versionedCipher_rejectsOversizedKeyId() = runTest {
        val store = RecordingKeyStore().apply {
            activeId = "x".repeat(65)
            keys[activeId] = ByteArray(32)
        }

        assertFailsWith<IllegalArgumentException> {
            VersionedAesGcmCipher(store).encrypt(byteArrayOf(1), null)
        }
    }

    @Test
    fun secureStack_rotatesClearsCacheAndRemovesOldKeys() = runTest {
        val store = RecordingKeyStore().apply {
            activeId = "old"
            keys["old"] = ByteArray(32) { 3 }
            keyIds += "old"
            lastRotation = 100
        }
        val stack = createAesPlatformStack(
            store = store,
            rotationConfig = KeyRotationConfig(rotationPeriod = 1.milliseconds),
            nowMillis = { 102 },
        )
        stack.cipher.encrypt(byteArrayOf(1), null)

        assertTrue(stack.keyRotator.rotateKeyIfNeeded())
        assertFalse(store.activeId == "old")
        assertEquals(102, store.lastRotation)
        stack.postMigrationCleanup()
        assertEquals(listOf(store.activeId), store.keyIds)
    }

    @Test
    fun secureStack_defaultConfigurationStampsFirstInitialization() = runTest {
        val store = RecordingKeyStore()
        val stack = createAesPlatformStack(store)
        assertFalse(stack.keyRotator.rotateKeyIfNeeded())
        assertTrue(store.lastRotation > 0)
    }

    @Test
    fun registry_unregisterAndRuntimeConvenienceConstructor() = runTest {
        val handle = EncryptedStoreHandle {}
        val registry = StoreRegistry()
        registry.register(handle)
        registry.register(handle)
        assertEquals(1, registry.size())
        registry.unregister(handle)
        assertEquals(0, registry.size())

        val cipher = object : Cipher {
            override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?) = message
            override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?) = message
        }
        val runtime = CryptoRuntime(
            keyRotator = object : KeyRotator {
                override suspend fun rotateKeyIfNeeded() = false
            },
            postRotationInit = {},
            cipher = cipher,
            storeRegistry = registry,
        )
        assertTrue(runtime.registry === registry)
        assertTrue(runtime.cipher === cipher)
        runtime.initialize()

        val defaultsRuntime = CryptoRuntime(
            keyRotator = object : KeyRotator {
                override suspend fun rotateKeyIfNeeded() = true
            },
            postRotationInit = {},
            cipher = cipher,
        )
        defaultsRuntime.initialize()
        assertEquals(CryptoRuntimeState.Ready, defaultsRuntime.state.value)

        val stackDefaults = PlatformCryptoStack(
            cipher = cipher,
            keyRotator = object : KeyRotator {
                override suspend fun rotateKeyIfNeeded() = false
            },
        )
        stackDefaults.postMigrationCleanup()
    }

    @Test
    fun algorithmProvider_defaultGetterFailsBeforeInitialization() {
        val provider = object : AlgorithmProvider<String> {
            override suspend fun initialize() = Unit
        }
        // Touch via the interface type so JVM DefaultImpls is exercised for Kover.
        val typed: AlgorithmProvider<String> = provider
        assertFailsWith<IllegalStateException> { typed.algorithm }
    }
}

private class RecordingKeyStore : SecureKeyStore {
    var legacy = ByteArray(32)
    var activeId = VersionedAesGcmCipher.LEGACY_KEY_ID
    val keys = mutableMapOf<String, ByteArray>()
    val keyIds = mutableListOf<String>()
    var lastRotation = 0L
    var namedReads = 0
    var legacyReads = 0

    override suspend fun readKey(): ByteArray {
        legacyReads++
        return legacy.copyOf()
    }

    override suspend fun writeKey(key: ByteArray) {
        legacy = key.copyOf()
    }

    override suspend fun readActiveKeyId() = activeId

    override suspend fun setActiveKeyId(keyId: String) {
        activeId = keyId
        if (keyId !in keyIds) keyIds += keyId
    }

    override suspend fun readKey(keyId: String): ByteArray {
        namedReads++
        return keys.getValue(keyId).copyOf()
    }

    override suspend fun writeKey(keyId: String, key: ByteArray) {
        keys[keyId] = key.copyOf()
        if (keyId !in keyIds) keyIds += keyId
    }

    override suspend fun listKeyIds() = keyIds.toList()

    override suspend fun deleteKey(keyId: String) {
        keys.remove(keyId)
        keyIds.remove(keyId)
    }

    override suspend fun readLastRotationMillis() = lastRotation

    override suspend fun writeLastRotationMillis(value: Long) {
        lastRotation = value
    }
}
