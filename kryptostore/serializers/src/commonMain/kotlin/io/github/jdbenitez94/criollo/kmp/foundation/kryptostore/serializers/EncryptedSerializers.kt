package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.ENCRYPTED_BLOB_MAGIC
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.protobuf.ProtoBuf
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.IOException

@OptIn(ExperimentalSerializationApi::class)
class ProtoOkioSerializer<T : Any>(private val kSerializer: KSerializer<T>, override val defaultValue: T) : OkioSerializer<T> {
    override suspend fun readFrom(source: BufferedSource): T = try {
        val bytes = source.readByteArray()
        if (bytes.isEmpty()) {
            defaultValue
        } else {
            ProtoBuf.decodeFromByteArray(kSerializer, bytes)
        }
    } catch (e: SerializationException) {
        throw CorruptionException("Cannot read proto.", e)
    } catch (e: IOException) {
        throw CorruptionException("Cannot read proto from disk.", e)
    }

    override suspend fun writeTo(t: T, sink: BufferedSink) {
        sink.write(ProtoBuf.encodeToByteArray(kSerializer, t))
    }

    fun encodeBytes(value: T): ByteArray = ProtoBuf.encodeToByteArray(kSerializer, value)

    fun decodeBytes(bytes: ByteArray): T = ProtoBuf.decodeFromByteArray(kSerializer, bytes)
}

class EncryptedProtoSerializer<T : Any>(
    private val inner: ProtoOkioSerializer<T>,
    private val cipher: Cipher,
    private val options: EncryptedStoreOptions = EncryptedStoreOptions(),
) : OkioSerializer<T> {
    constructor(
        inner: ProtoOkioSerializer<T>,
        cipher: Cipher,
        associatedData: ByteArray,
        legacyAssociatedData: ByteArray? = null,
        allowPlaintextRead: Boolean = false,
    ) : this(
        inner = inner,
        cipher = cipher,
        options = EncryptedStoreOptions {
            this.associatedData = associatedData
            this.legacyAssociatedData = legacyAssociatedData
            this.allowPlaintextRead = allowPlaintextRead
        },
    )

    override val defaultValue: T
        get() = inner.defaultValue

    private val primaryAad: ByteArray
        get() = options.resolvedAssociatedData()

    override suspend fun readFrom(source: BufferedSource): T {
        val bytes = source.readByteArray()
        if (bytes.isEmpty()) return defaultValue
        if (!bytes.startsWithEncryptedBlobMagic()) {
            if (options.allowPlaintextRead) {
                return inner.decodeBytes(bytes)
            }
            rejectPlaintextPayload()
        }
        return readEncryptedProto(bytes)
    }

    override suspend fun writeTo(t: T, sink: BufferedSink) {
        val plain = inner.encodeBytes(t)
        val encrypted = cipher.encrypt(plain, primaryAad)
        sink.writeUtf8(ENCRYPTED_BLOB_MAGIC)
        sink.writeByte(ENCRYPTION_ENVELOPE_VERSION)
        sink.write(encrypted)
    }

    private suspend fun readEncryptedProto(bytes: ByteArray): T {
        val payload = bytes.copyOfRange(ENCRYPTED_BLOB_MAGIC.length, bytes.size)
        if (payload.isEmpty()) throw CorruptionException("Encrypted payload is empty.")
        if (payload.first().toInt() != ENCRYPTION_ENVELOPE_VERSION) {
            throw CorruptionException("Unsupported encryption envelope version: ${payload.first().toInt()}")
        }
        val plain = decryptWithAssociatedDataFallback(
            cipher = cipher,
            ciphertext = payload.copyOfRange(1, payload.size),
            associatedData = primaryAad,
            legacyAssociatedData = options.legacyAssociatedData,
        )
        return inner.decodeBytes(plain)
    }
}

/** Migration alias — prefer [EncryptedProtoSerializer]. */
typealias EncryptedOkioSerializer<T> = EncryptedProtoSerializer<T>

class EncryptedPreferencesSerializer(private val cipher: Cipher, private val options: EncryptedStoreOptions = EncryptedStoreOptions()) : OkioSerializer<Preferences> {
    constructor(
        cipher: Cipher,
        associatedData: ByteArray,
        legacyAssociatedData: ByteArray? = null,
        allowPlaintextRead: Boolean = false,
    ) : this(
        cipher = cipher,
        options = EncryptedStoreOptions {
            this.associatedData = associatedData
            this.legacyAssociatedData = legacyAssociatedData
            this.allowPlaintextRead = allowPlaintextRead
        },
    )

    private val inner = PreferencesSerializer

    override val defaultValue: Preferences
        get() = inner.defaultValue

    private val primaryAad: ByteArray
        get() = options.resolvedAssociatedData()

    override suspend fun readFrom(source: BufferedSource): Preferences {
        val bytes = source.readByteArray()
        if (bytes.isEmpty()) return defaultValue
        if (!bytes.startsWithEncryptedBlobMagic()) {
            if (options.allowPlaintextRead) {
                return inner.readFrom(Buffer().write(bytes))
            }
            rejectPlaintextPayload()
        }
        val payload = decryptEnvelope(bytes)
        return inner.readFrom(Buffer().write(payload))
    }

    override suspend fun writeTo(t: Preferences, sink: BufferedSink) {
        val plainBuffer = Buffer()
        inner.writeTo(t, plainBuffer)
        val encrypted = cipher.encrypt(plainBuffer.readByteArray(), primaryAad)
        sink.writeUtf8(ENCRYPTED_BLOB_MAGIC)
        sink.writeByte(ENCRYPTION_ENVELOPE_VERSION)
        sink.write(encrypted)
    }

    private suspend fun decryptEnvelope(bytes: ByteArray): ByteArray {
        val magicLength = ENCRYPTED_BLOB_MAGIC.length
        val payload = bytes.copyOfRange(magicLength, bytes.size)
        if (payload.isEmpty()) {
            throw CorruptionException("Encrypted payload is empty.")
        }
        if (payload.first().toInt() != ENCRYPTION_ENVELOPE_VERSION) {
            throw CorruptionException("Unsupported encryption envelope version: ${payload.first().toInt()}")
        }
        return decryptWithAssociatedDataFallback(
            cipher = cipher,
            ciphertext = payload.copyOfRange(1, payload.size),
            associatedData = primaryAad,
            legacyAssociatedData = options.legacyAssociatedData,
        )
    }
}
