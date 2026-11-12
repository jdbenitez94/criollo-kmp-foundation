package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlinx.coroutines.await
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** REQ-ROT-05 — Web multi-key WebCrypto keyring + re-encrypt cleanup. */
@OptIn(ExperimentalEncodingApi::class, ExperimentalUuidApi::class, ExperimentalWasmJsInterop::class)
class WebCryptoKeyringTest {
    @Test
    fun encryptDecrypt_roundTripsWithKeyIdHeader() = runTest {
        val appId = "web-roundtrip-${Uuid.random()}"
        val stack = createPlatformCryptoStack(appId)
        stack.postRotationInit()
        val plain = "hello-web-crypto".encodeToByteArray()
        val aad = "aad|v1".encodeToByteArray()
        val encrypted = stack.cipher.encrypt(plain, aad)
        assertTrue(encrypted.size > 13)
        assertContentEquals(plain, stack.cipher.decrypt(encrypted, aad))
        assertFails { stack.cipher.decrypt(encrypted, "wrong".encodeToByteArray()) }
    }

    @Test
    fun rotateIfNeeded_stampsThenRotates_andCleanupDropsOldKeys() = runTest {
        val appId = "web-rotate-${Uuid.random()}"
        ensureInstalled()
        CryptoBindings.ensureKeyring(appId).await()

        assertFalse(rotate(appId, periodMs = 90_000.0, nowMillis = 1_000.0))
        val before = CryptoBindings.getActiveKeyId(appId).await().toString()
        val plain = "rotate-me".encodeToByteArray()
        val encrypted = encrypt(appId, plain)

        assertTrue(rotate(appId, periodMs = 1.0, nowMillis = 2_000.0))
        val after = CryptoBindings.getActiveKeyId(appId).await().toString()
        assertTrue(before != after)

        // Old ciphertext still decrypts while inactive key remains.
        assertContentEquals(plain, decrypt(appId, encrypted))

        val keyIds = parseJsonStringArray(CryptoBindings.listKeyIds(appId).await().toString())
        assertEquals(2, keyIds.size)

        createPlatformCryptoStack(
            appId = appId,
            rotationConfig = KeyRotationConfig(rotationPeriod = 1.milliseconds),
        ).postMigrationCleanup()

        val remaining = parseJsonStringArray(CryptoBindings.listKeyIds(appId).await().toString())
        assertEquals(listOf(after), remaining)
        assertFails { decrypt(appId, encrypted) }
    }

    @Test
    fun cryptoRuntime_reEncryptsRegisteredHandlesAfterRotation() = runTest {
        val appId = "web-reencrypt-${Uuid.random()}"
        ensureInstalled()
        CryptoBindings.ensureKeyring(appId).await()
        assertFalse(rotate(appId, periodMs = 90_000.0, nowMillis = 5_000.0))

        val stack = createPlatformCryptoStack(
            appId = appId,
            rotationConfig = KeyRotationConfig(rotationPeriod = 1.milliseconds),
        )
        val registry = StoreRegistry()
        var blob = stack.cipher.encrypt("payload".encodeToByteArray(), null)
        registry.register(
            EncryptedStoreHandle {
                val plain = stack.cipher.decrypt(blob, null)
                blob = stack.cipher.encrypt(plain, null)
            },
        )

        // Force rotation outside the stack clock, then run initialize path manually.
        assertTrue(rotate(appId, periodMs = 1.0, nowMillis = 9_000.0))
        val oldBlob = blob.copyOf()
        registry.reEncryptAll()
        stack.postMigrationCleanup()

        assertContentEquals("payload".encodeToByteArray(), stack.cipher.decrypt(blob, null))
        assertFails { stack.cipher.decrypt(oldBlob, null) }
    }

    private fun ensureInstalled() {
        CryptoBindings.install()
    }

    private suspend fun rotate(appId: String, periodMs: Double, nowMillis: Double): Boolean =
        CryptoBindings.rotateIfNeeded(appId, periodMs, nowMillis).await().toString()
            .equals("true", ignoreCase = true)

    private suspend fun encrypt(appId: String, plain: ByteArray): ByteArray {
        val ciphertext = CryptoBindings.encrypt(
            appId = appId,
            plaintextBase64 = Base64.encode(plain),
            associatedDataBase64 = null,
        ).await().toString()
        return Base64.decode(ciphertext)
    }

    private suspend fun decrypt(appId: String, ciphertext: ByteArray): ByteArray {
        val plaintext = CryptoBindings.decrypt(
            appId = appId,
            ciphertextBase64 = Base64.encode(ciphertext),
            associatedDataBase64 = null,
        ).await().toString()
        return Base64.decode(plaintext)
    }
}
