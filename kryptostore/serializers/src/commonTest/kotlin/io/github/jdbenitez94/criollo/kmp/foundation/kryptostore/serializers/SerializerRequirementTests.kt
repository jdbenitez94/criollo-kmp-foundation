package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers

import androidx.datastore.core.CorruptionException
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.ENCRYPTED_BLOB_MAGIC
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** REQ-SER-01, REQ-SER-04, REQ-SER-09 */
class EnvelopeFormatTest {
    private val cipher = IdentityCipher()
    private val inner = ProtoOkioSerializer(Sample.serializer(), Sample())

    @Test
    fun write_startsWithMagicAndVersionByteOne() = runTest {
        val serializer = EncryptedProtoSerializer(inner, cipher, associatedData = AAD)
        val buffer = Buffer()
        serializer.writeTo(Sample(enabled = true), buffer)
        val bytes = buffer.readByteArray()
        val magicLen = ENCRYPTED_BLOB_MAGIC.length
        assertEquals(ENCRYPTED_BLOB_MAGIC, bytes.decodeToString(0, magicLen))
        assertEquals(1, bytes[magicLen].toInt())
    }

    @Test
    fun unknownEnvelopeVersion_throwsCorruption() = runTest {
        val serializer = EncryptedProtoSerializer(inner, cipher, associatedData = AAD)
        val bad = Buffer()
            .writeUtf8(ENCRYPTED_BLOB_MAGIC)
            .writeByte(99)
            .write(byteArrayOf(1, 2, 3))
            .readByteArray()
        assertFailsWith<CorruptionException> {
            serializer.readFrom(Buffer().write(bad))
        }
    }

    @Test
    fun emptyPayload_returnsDefaultValue() = runTest {
        val serializer = EncryptedProtoSerializer(inner, cipher, associatedData = AAD)
        assertEquals(Sample(), serializer.readFrom(Buffer()))
    }
}

/** REQ-SER-02, REQ-SER-03, REQ-MIG-01 */
class PlaintextMigrationOptionTest {
    private val inner = ProtoOkioSerializer(Sample.serializer(), Sample())

    @Test
    fun missingMagic_allowPlaintextReadFalse_throws() = runTest {
        val serializer = EncryptedProtoSerializer(
            inner = inner,
            cipher = IdentityCipher(),
            associatedData = AAD,
            allowPlaintextRead = false,
        )
        val error = assertFailsWith<CorruptionException> {
            serializer.readFrom(Buffer().write(inner.encodeBytes(Sample(enabled = true))))
        }
        assertTrue(error.message!!.contains(ENCRYPTED_BLOB_MAGIC))
    }

    @Test
    fun missingMagic_allowPlaintextReadTrue_decodesPlain() = runTest {
        val serializer = EncryptedProtoSerializer(
            inner = inner,
            cipher = IdentityCipher(),
            options = EncryptedStoreOptions {
                associatedData = AAD
                allowPlaintextRead = true
            },
        )
        val plain = inner.encodeBytes(Sample(enabled = true))
        assertEquals(Sample(enabled = true), serializer.readFrom(Buffer().write(plain)))
    }
}

/** REQ-SER-05 */
class AssociatedDataFallbackTest {
    @Test
    fun legacyAad_decrypts_andRewriteUsesPrimary() = runTest {
        val legacyAad = "legacy-aad".encodeToByteArray()
        val newAad = "new-aad".encodeToByteArray()
        val inner = ProtoOkioSerializer(Sample.serializer(), Sample())
        val serializer = EncryptedProtoSerializer(
            inner = inner,
            cipher = SelectiveTestCipher(newAad, legacyAad),
            associatedData = newAad,
            legacyAssociatedData = legacyAad,
        )
        val legacyCipher = SelectiveTestCipher(legacyAad)
        val legacyBytes = Buffer()
            .writeUtf8(ENCRYPTED_BLOB_MAGIC)
            .writeByte(1)
            .write(legacyCipher.encrypt(inner.encodeBytes(Sample(enabled = true)), legacyAad))
            .readByteArray()

        assertEquals(Sample(enabled = true), serializer.readFrom(Buffer().write(legacyBytes)))

        val rewritten = Buffer()
        serializer.writeTo(Sample(enabled = true), rewritten)
        // Decrypt with primary-only cipher succeeds (rewrite used primary AAD).
        val primaryOnly = EncryptedProtoSerializer(
            inner = inner,
            cipher = SelectiveTestCipher(newAad),
            associatedData = newAad,
        )
        assertEquals(Sample(enabled = true), primaryOnly.readFrom(Buffer().write(rewritten.readByteArray())))
    }
}

/** REQ-SER-06 */
class DecryptCancellationTest {
    @Test
    fun cancellationDuringDecrypt_propagates() = runTest {
        val cipher = object : Cipher {
            override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = message
            override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = throw kotlinx.coroutines.CancellationException("cancelled mid-decrypt")
        }
        assertFailsWith<kotlinx.coroutines.CancellationException> {
            decryptWithAssociatedDataFallback(
                cipher = cipher,
                ciphertext = byteArrayOf(1, 2, 3),
                associatedData = AAD,
            )
        }
    }
}

/** REQ-SER-07 */
class EncryptedPreferencesRoundTripTest {
    @Test
    fun preferences_roundTrip() = runTest {
        val key = androidx.datastore.preferences.core.booleanPreferencesKey("enabled")
        val serializer = EncryptedPreferencesSerializer(
            cipher = ReversibleTestCipher(),
            associatedData = AAD,
        )
        val buffer = Buffer()
        serializer.writeTo(
            androidx.datastore.preferences.core.mutablePreferencesOf(key to true),
            buffer,
        )
        val bytes = buffer.readByteArray()
        assertEquals(ENCRYPTED_BLOB_MAGIC, bytes.decodeToString(0, ENCRYPTED_BLOB_MAGIC.length))
        assertEquals(true, serializer.readFrom(Buffer().write(bytes))[key])
    }
}

/** REQ-SER-08 */
class EncryptedProtoRoundTripTest {
    @Test
    fun proto_roundTrip() = runTest {
        val serializer = EncryptedProtoSerializer(
            inner = ProtoOkioSerializer(Sample.serializer(), Sample()),
            cipher = ReversibleTestCipher(),
            associatedData = AAD,
        )
        val buffer = Buffer()
        serializer.writeTo(Sample(enabled = true), buffer)
        val bytes = buffer.readByteArray()
        assertTrue(bytes.startsWithEncryptedBlobMagic())
        assertEquals(Sample(enabled = true), serializer.readFrom(Buffer().write(bytes)))
    }

    @Test
    fun deriveStoreAssociatedData_matchesSpec() {
        assertEquals(
            "settings|v1".encodeToByteArray().toList(),
            deriveStoreAssociatedData("settings", 1).toList(),
        )
    }
}

@Serializable
internal data class Sample(val enabled: Boolean = false)

internal val AAD = "test-associated-data".encodeToByteArray()

internal class IdentityCipher : Cipher {
    override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = message
    override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = message
}

internal class ReversibleTestCipher : Cipher {
    override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = message.reversedArray()
    override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = message.reversedArray()
}

internal class SelectiveTestCipher(private vararg val acceptedAads: ByteArray) : Cipher {
    override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?): ByteArray {
        require(acceptedAads.any { it.contentEquals(associatedData) })
        return message.reversedArray()
    }

    override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray {
        require(acceptedAads.any { it.contentEquals(associatedData) })
        return message.reversedArray()
    }
}
