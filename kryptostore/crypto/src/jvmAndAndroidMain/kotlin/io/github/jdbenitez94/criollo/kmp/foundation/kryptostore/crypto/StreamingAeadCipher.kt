package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.streamingaead.PredefinedStreamingAeadParameters
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Optional [Cipher] backed by Tink [StreamingAead] for large payloads (REQ-HRD-03).
 *
 * Compatible with the KryptoStore envelope when used from `EncryptedProtoSerializer` /
 * `EncryptedPreferencesSerializer`: the envelope still wraps the StreamingAead ciphertext.
 *
 * Prefer the default platform [Cipher] from [createPlatformCryptoStack] for settings blobs,
 * which manages key persistence and rotation automatically. If you construct a [StreamingAeadCipher]
 * for persistent stores, supply a [StreamingAead] primitive backed by a persisted `KeysetHandle`.
 */
class StreamingAeadCipher(
    private val streamingAead: StreamingAead,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : Cipher {
    override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = withContext(dispatcher) {
        val out = ByteArrayOutputStream()
        streamingAead.newEncryptingStream(out, associatedData ?: ByteArray(0)).use { stream ->
            stream.write(message)
        }
        out.toByteArray()
    }

    override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = withContext(dispatcher) {
        ByteArrayInputStream(message).use { input ->
            streamingAead.newDecryptingStream(input, associatedData ?: ByteArray(0)).use { stream ->
                stream.readBytes()
            }
        }
    }
}

/**
 * Creates a standalone, **in-memory** [StreamingAeadCipher] with `AES256_GCM_HKDF_1MB` (REQ-HRD-03).
 *
 * **Warning:** This helper generates a fresh ephemeral keyset in memory on every call. Do not pass it
 * to a persistent `DataStore` across process restarts; for persistent stores, construct
 * [StreamingAeadCipher] with a [StreamingAead] primitive loaded from a persisted Tink `KeysetHandle`.
 */
fun createStreamingAeadCipher(dispatcher: CoroutineDispatcher = Dispatchers.Default): StreamingAeadCipher {
    StreamingAeadConfig.register()
    val handle = KeysetHandle.generateNew(PredefinedStreamingAeadParameters.AES256_GCM_HKDF_1MB)
    return StreamingAeadCipher(handle.getPrimitive(StreamingAead::class.java), dispatcher)
}
