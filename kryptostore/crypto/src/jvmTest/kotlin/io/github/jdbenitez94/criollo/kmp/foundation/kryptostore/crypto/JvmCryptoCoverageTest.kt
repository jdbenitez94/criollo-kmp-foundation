package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.subtle.AesGcmJce
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class JvmCryptoCoverageTest {
    @Test
    fun rawAes_helpersCoverAadAndNoAad() = runTest {
        val key = randomAes256Key()
        assertEquals(32, key.size)
        val plain = "raw-aes".encodeToByteArray()

        val withoutAad = aesGcmEncrypt(key, plain, null)
        assertContentEquals(plain, aesGcmDecrypt(key, withoutAad, null))
        val withAad = aesGcmEncrypt(key, plain, byteArrayOf(4))
        assertContentEquals(plain, aesGcmDecrypt(key, withAad, byteArrayOf(4)))
        assertFails { aesGcmDecrypt(key, withAad, byteArrayOf(9)) }
    }

    @Test
    fun tinkHelpers_providerAndCipherExerciseAllInitializationPaths() = runTest {
        AeadConfig.register()
        val first = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
        val provider = TinkAeadProvider(first)
        // Concurrent first-time init hits the inner double-check after the winner finishes.
        (1..8).map { async { provider.initialize() } }.awaitAll()
        provider.initialize()

        val cipher = TinkCipher(provider, Dispatchers.Unconfined)
        val plain = byteArrayOf(1, 2, 3)
        val encrypted = cipher.encrypt(plain, null)
        assertContentEquals(plain, cipher.decrypt(encrypted, null))

        val rotated = first.withRotatedAes256GcmPrimary()
        val master: Aead = AesGcmJce(ByteArray(32) { 5 })
        val serialized = rotated.serializeEncryptedKeyset(master, byteArrayOf(8))
        assertTrue(serialized.isNotEmpty())
        provider.replaceKeyset(rotated)
        (1..4).map { async { provider.initialize() } }.awaitAll()

        assertTrue(isWithinRotationPeriod(10, 11, 1))
        assertFalse(isWithinRotationPeriod(0, 1, 10))
        assertTrue(isUnsetRotationStamp(0))
        assertFalse(isUnsetRotationStamp(1))
    }

    @Test
    fun streamingCipher_roundTripsWithoutAssociatedData() = runTest {
        val cipher = createStreamingAeadCipher(Dispatchers.Unconfined)
        val plain = ByteArray(8_193) { it.toByte() }
        assertContentEquals(plain, cipher.decrypt(cipher.encrypt(plain, null), null))
    }

    @Test
    fun posixStore_createsReadsAndReplacesMalformedKey() = runTest {
        withTemporaryHome("posix") {
            val store = PosixSealedFileMasterKeyStore("coverage")
            val first = store.readOrCreate("master")
            assertContentEquals(first, store.readOrCreate("master"))

            val keyFile = resolve(".kryptostore/coverage/crypto/master.key")
            keyFile.writeText("bad")
            val replacement = store.readOrCreate("master")
            assertEquals(32, replacement.size)
            assertFalse(replacement.contentEquals(first))
        }
    }

    @Test
    fun jvmStack_coversCreateLoadStampAndRotationPaths() = runTest {
        withTemporaryHome("stack") {
            val appId = "coverage"
            val fresh = createJvmTinkStack(appId)
            fresh.postRotationInit()
            assertFalse(fresh.keyRotator.rotateKeyIfNeeded())

            val rotation = resolve(".kryptostore/$appId/rotation.properties")
            rotation.delete()
            val unstamped = createPlatformCryptoStack(appId)
            assertFalse(unstamped.keyRotator.rotateKeyIfNeeded())

            rotation.writeText("1")
            val due = createJvmTinkStack(
                appId,
                KeyRotationConfig(rotationPeriod = 1.milliseconds),
            )
            assertTrue(due.keyRotator.rotateKeyIfNeeded())
            due.postRotationInit()

            rotation.writeText("not-a-number")
            val malformed = createJvmTinkStack(
                appId,
                KeyRotationConfig(rotationPeriod = 1.milliseconds),
            )
            // Malformed stamp parses as 0 → treated as unset / overdue depending on helpers.
            malformed.keyRotator.rotateKeyIfNeeded()

            val message = "persisted".encodeToByteArray()
            val encrypted = due.cipher.encrypt(message)
            assertContentEquals(message, due.cipher.decrypt(encrypted))
        }
    }

    @Test
    fun masterKeySelection_coversPosixAndMacSelection() {
        val original = System.getProperty("os.name")
        try {
            System.setProperty("os.name", "Linux")
            assertTrue(JvmSecureMasterKeyStore.current("linux") is PosixSealedFileMasterKeyStore)
            System.setProperty("os.name", "Mac OS X")
            assertTrue(JvmSecureMasterKeyStore.current("mac") === MacOsKeychainMasterKeyStore)
        } finally {
            System.setProperty("os.name", original)
        }
    }

    @Test
    fun registry_contentionFailsFastForLifecycleMutations() = runTest {
        val registry = StoreRegistry()
        val mutexField = StoreRegistry::class.java.getDeclaredField("mutex").apply { isAccessible = true }
        val mutex = mutexField.get(registry) as Mutex
        val handle = EncryptedStoreHandle {}
        mutex.lock()
        try {
            assertFailsWith<IllegalStateException> { registry.register(handle) }
            assertFailsWith<IllegalStateException> { registry.unregister(handle) }
        } finally {
            mutex.unlock()
        }
    }

    @Test
    fun macKeychain_roundTripsOnMacOs() {
        if (!System.getProperty("os.name").contains("Mac", ignoreCase = true)) return
        val account = "kryptostore-crypto-coverage-test"
        val first = MacOsKeychainMasterKeyStore.readOrCreate(account)
        assertEquals(32, first.size)
        assertContentEquals(first, MacOsKeychainMasterKeyStore.readOrCreate(account))

        val type = MacOsKeychainMasterKeyStore::class.java
        type.getDeclaredMethod("write", String::class.java, ByteArray::class.java).apply {
            isAccessible = true
            invoke(MacOsKeychainMasterKeyStore, account, ByteArray(32) { 12 })
        }
        assertContentEquals(ByteArray(32) { 12 }, MacOsKeychainMasterKeyStore.readOrCreate(account))
        val statusCheck = type.getDeclaredMethod("checkStatus", Int::class.java, String::class.java).apply {
            isAccessible = true
        }
        assertFails { statusCheck.invoke(MacOsKeychainMasterKeyStore, -1, "coverage") }
    }
}

private suspend inline fun withTemporaryHome(label: String, crossinline block: suspend java.io.File.() -> Unit) {
    val directory = Files.createTempDirectory("crypto-$label").toFile()
    val previous = System.getProperty("user.home")
    System.setProperty("user.home", directory.absolutePath)
    try {
        directory.block()
    } finally {
        if (previous == null) System.clearProperty("user.home") else System.setProperty("user.home", previous)
        directory.deleteRecursively()
    }
}
