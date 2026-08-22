package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers

import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.ENCRYPTED_BLOB_MAGIC
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * REQ-HRD-01 — frozen envelope v1 fixture.
 *
 * Hex was generated with [IdentityCompatCipher] (ciphertext == plaintext) so the fixture
 * stays stable across platforms without embedding a keyset. Do not change without bumping
 * [ENCRYPTION_ENVELOPE_VERSION].
 */
class CompatBlobFixtureTest {
    @Test
    fun frozenEnvelopeV1_proto_reads() = runTest {
        val bytes = hexToBytes(FROZEN_ENVELOPE_V1_HEX)
        assertEquals(ENCRYPTED_BLOB_MAGIC, bytes.decodeToString(0, ENCRYPTED_BLOB_MAGIC.length))
        assertEquals(ENCRYPTION_ENVELOPE_VERSION, bytes[ENCRYPTED_BLOB_MAGIC.length].toInt())

        val serializer = EncryptedProtoSerializer(
            inner = ProtoOkioSerializer(CompatSample.serializer(), CompatSample()),
            cipher = IdentityCompatCipher,
            associatedData = FIXTURE_AAD,
        )
        assertEquals(CompatSample(enabled = true, label = "v1"), serializer.readFrom(Buffer().write(bytes)))
    }

    @Test
    fun regeneratingFixture_matchesFrozenHex() = runTest {
        val serializer = EncryptedProtoSerializer(
            inner = ProtoOkioSerializer(CompatSample.serializer(), CompatSample()),
            cipher = IdentityCompatCipher,
            associatedData = FIXTURE_AAD,
        )
        val buffer = Buffer()
        serializer.writeTo(CompatSample(enabled = true, label = "v1"), buffer)
        assertEquals(FROZEN_ENVELOPE_V1_HEX, buffer.readByteArray().toHex())
    }
}

@Serializable
private data class CompatSample(val enabled: Boolean = false, val label: String = "")

private object IdentityCompatCipher : Cipher {
    override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = message
    override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = message
}

private val FIXTURE_AAD = "kryptostore-compat-v1".encodeToByteArray()

/**
 * SVBLENC1 + 0x01 + protobuf(CompatSample(enabled=true, label="v1")).
 * Regenerate via regeneratingFixture_matchesFrozenHex if schema changes (then bump envelope).
 */
private const val FROZEN_ENVELOPE_V1_HEX =
    "5356424c454e433101080112027631"

private fun ByteArray.toHex(): String = joinToString("") { b ->
    (b.toInt() and 0xff).toString(16).padStart(2, '0')
}

private fun hexToBytes(hex: String): ByteArray {
    require(hex.length % 2 == 0)
    return ByteArray(hex.length / 2) { i ->
        hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}
