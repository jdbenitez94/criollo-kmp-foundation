package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import okio.Buffer
import okio.IOException
import okio.buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import androidx.datastore.core.CorruptionException as CovCorruption
import androidx.datastore.preferences.core.PreferencesSerializer as CovPrefsSerializer
import androidx.datastore.preferences.core.booleanPreferencesKey as CovBoolKey
import androidx.datastore.preferences.core.mutablePreferencesOf as CovMutablePrefs
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher as CovCipher
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.ENCRYPTED_BLOB_MAGIC as CovMagic

/** Extra ProtoOkio / envelope / prefs branches for 100% serializers coverage. */
class ProtoOkioSerializerCoverageTest {
    @Test
    fun readFrom_empty_returnsDefault() = runTest {
        val serializer = ProtoOkioSerializer(CoverageSample.serializer(), CoverageSample())
        assertEquals(CoverageSample(), serializer.readFrom(Buffer()))
    }

    @Test
    fun readFrom_invalidProto_corruption() = runTest {
        val serializer = ProtoOkioSerializer(CoverageSample.serializer(), CoverageSample())
        assertFailsWith<CovCorruption> {
            serializer.readFrom(Buffer().write(byteArrayOf(0xFF.toByte(), 0x00)))
        }
    }

    @Test
    fun readFrom_ioException_corruption() = runTest {
        val serializer = ProtoOkioSerializer(CoverageSample.serializer(), CoverageSample())
        val broken = object : okio.ForwardingSource(Buffer()) {
            override fun read(sink: Buffer, byteCount: Long): Long = throw IOException("disk fail")
        }.buffer()
        assertFailsWith<CovCorruption> {
            serializer.readFrom(broken)
        }
    }

    @Test
    fun writeTo_encodes() = runTest {
        val serializer = ProtoOkioSerializer(CoverageSample.serializer(), CoverageSample())
        val buffer = Buffer()
        serializer.writeTo(CoverageSample(flag = true), buffer)
        assertTrue(buffer.size > 0)
        assertEquals(CoverageSample(flag = true), serializer.readFrom(Buffer().write(buffer.readByteArray())))
    }
}

class EnvelopeEmptyPayloadTest {
    @Test
    fun encryptedProto_emptyPayloadAfterMagic_throws() = runTest {
        val serializer = EncryptedProtoSerializer(
            inner = ProtoOkioSerializer(CoverageSample.serializer(), CoverageSample()),
            cipher = IdentityCipher(),
            associatedData = AAD,
        )
        val bad = Buffer().writeUtf8(CovMagic).readByteArray()
        assertFailsWith<CovCorruption> {
            serializer.readFrom(Buffer().write(bad))
        }
    }
}

class EncryptedPreferencesCoverageTest {
    @Test
    fun empty_returnsDefaultValue() = runTest {
        val serializer = EncryptedPreferencesSerializer(cipher = ReversibleTestCipher(), associatedData = AAD)
        assertEquals(serializer.defaultValue, serializer.readFrom(Buffer()))
    }

    @Test
    fun plaintext_rejected() = runTest {
        val flag = CovBoolKey("enabled")
        val sink = Buffer()
        CovPrefsSerializer.writeTo(CovMutablePrefs(flag to true), sink)
        val bytes = sink.readByteArray()
        assertFailsWith<CovCorruption> {
            EncryptedPreferencesSerializer(cipher = ReversibleTestCipher(), associatedData = AAD)
                .readFrom(Buffer().write(bytes))
        }
    }

    @Test
    fun plaintext_allowed() = runTest {
        val flag = CovBoolKey("enabled")
        val encoded = Buffer().also { CovPrefsSerializer.writeTo(CovMutablePrefs(flag to true), it) }.readByteArray()
        val prefs = EncryptedPreferencesSerializer(
            cipher = ReversibleTestCipher(),
            associatedData = AAD,
            allowPlaintextRead = true,
        ).readFrom(Buffer().write(encoded))
        assertEquals(true, prefs[flag])
    }

    @Test
    fun emptyEncryptedPayload_throws() = runTest {
        val serializer = EncryptedPreferencesSerializer(cipher = ReversibleTestCipher(), associatedData = AAD)
        val bad = Buffer().writeUtf8(CovMagic).readByteArray()
        assertFailsWith<CovCorruption> {
            serializer.readFrom(Buffer().write(bad))
        }
    }

    @Test
    fun unknownEnvelopeVersion_throws() = runTest {
        val serializer = EncryptedPreferencesSerializer(cipher = ReversibleTestCipher(), associatedData = AAD)
        val bad = Buffer()
            .writeUtf8(CovMagic)
            .writeByte(99)
            .write(byteArrayOf(1))
            .readByteArray()
        assertFailsWith<CovCorruption> {
            serializer.readFrom(Buffer().write(bad))
        }
    }
}

class DecryptAllAadsFailTest {
    @Test
    fun allAadsFail_throwsCorruption() = runTest {
        val cipher = object : CovCipher {
            override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = message
            override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray =
                error("no aad works")
        }
        assertFailsWith<CovCorruption> {
            decryptWithAssociatedDataFallback(
                cipher = cipher,
                ciphertext = byteArrayOf(1, 2, 3),
                associatedData = AAD,
                legacyAssociatedData = "legacy".encodeToByteArray(),
            )
        }
    }
}

class EncryptedStoreOptionsTest {
    @Test
    fun resolvedAssociatedData_derivesWhenNull() {
        val options = EncryptedStoreOptions().apply {
            storeName = "settings"
            schemaVersion = 2
            associatedData = null
        }
        assertEquals("settings|v2".encodeToByteArray().toList(), options.resolvedAssociatedData().toList())
    }

    @Test
    fun applyDefaultStoreName_setsWhenDefault() {
        val options = EncryptedStoreOptions()
        options.applyDefaultStoreName("web-store")
        assertEquals("web-store", options.storeName)
    }

    @Test
    fun applyDefaultStoreName_keepsCustom() {
        val options = EncryptedStoreOptions().apply { storeName = "custom" }
        options.applyDefaultStoreName("ignored")
        assertEquals("custom", options.storeName)
    }
}

class KryptostoreFileSystemCoverageTest {
    @Test
    fun requireKryptostoreFileSystem_throwsWhenNull() {
        val error = assertFailsWith<IllegalStateException> {
            requireKryptostoreFileSystem(fileSystem = null)
        }
        assertTrue(error.message!!.contains("FileSystem is not available"))
    }
}

@Serializable
private data class CoverageSample(val flag: Boolean = false)
