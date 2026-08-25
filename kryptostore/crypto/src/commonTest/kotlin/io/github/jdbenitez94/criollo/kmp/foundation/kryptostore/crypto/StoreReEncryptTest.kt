package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.assertContentEquals as assertBytesEqual

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
    fun keyIdEnvelope_roundTripsAndRejectsInvalidHeaders() {
        val header = KeyIdEnvelope.encode("active-key")
        val message = header + byteArrayOf(1, 2, 3)
        val (keyId, payload) = KeyIdEnvelope.decode(message)
        assertEquals("active-key", keyId)
        assertBytesEqual(byteArrayOf(1, 2, 3), payload)

        assertFailsWith<IllegalArgumentException> { KeyIdEnvelope.encode("") }
        assertFailsWith<IllegalArgumentException> { KeyIdEnvelope.encode("x".repeat(65)) }
        assertFailsWith<IllegalArgumentException> { KeyIdEnvelope.decode(byteArrayOf()) }
        assertFailsWith<IllegalArgumentException> { KeyIdEnvelope.decode(byteArrayOf(0)) }
        assertFailsWith<IllegalArgumentException> { KeyIdEnvelope.decode(byteArrayOf(65)) }
    }

    @Test
    fun parseJsonStringArray_readsWorkerListPayload() {
        assertEquals(emptyList(), parseJsonStringArray("[]"))
        assertEquals(listOf("a", "b"), parseJsonStringArray("""["a","b"]"""))
        assertFailsWith<IllegalArgumentException> { parseJsonStringArray("not-json") }
    }
}

private class PassthroughCipher : Cipher {
    override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = message
    override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = message
}

private fun keyRotator(block: suspend () -> Boolean): KeyRotator = object : KeyRotator {
    override suspend fun rotateKeyIfNeeded(): Boolean = block()
}
