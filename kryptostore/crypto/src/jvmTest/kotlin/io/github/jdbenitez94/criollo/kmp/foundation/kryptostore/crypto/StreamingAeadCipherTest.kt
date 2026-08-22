package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlinx.coroutines.test.runTest
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import kotlin.test.Test

/** REQ-HRD-03 */
class StreamingAeadCipherTest {
    @Test
    fun streamingAead_roundTripsLargePayload() = runTest {
        val cipher = createStreamingAeadCipher()
        val aad = "kryptostore-stream".encodeToByteArray()
        val plain = ByteArray(256 * 1024) { (it % 251).toByte() }

        val encrypted = cipher.encrypt(plain, aad)
        expectThat(encrypted).isNotEqualTo(plain)

        val decrypted = cipher.decrypt(encrypted, aad)
        expectThat(decrypted.toList()).isEqualTo(plain.toList())
    }
}
