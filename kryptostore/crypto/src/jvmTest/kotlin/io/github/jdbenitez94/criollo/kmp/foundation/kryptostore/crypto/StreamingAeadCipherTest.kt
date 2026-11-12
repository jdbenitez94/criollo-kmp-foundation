package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse

/** REQ-HRD-03 — Streaming AEAD round-trip (large payload). */
class StreamingAeadCipherTest {
    @Test
    fun streamingAead_roundTripsLargePayload() = runTest {
        val cipher = createStreamingAeadCipher()
        val aad = "kryptostore-stream".encodeToByteArray()
        val plain = ByteArray(256 * 1024) { (it % 251).toByte() }

        val encrypted = cipher.encrypt(plain, aad)
        assertFalse(encrypted.contentEquals(plain), "ciphertext must differ from plaintext")

        val decrypted = cipher.decrypt(encrypted, aad)
        assertContentEquals(plain, decrypted)
    }
}
