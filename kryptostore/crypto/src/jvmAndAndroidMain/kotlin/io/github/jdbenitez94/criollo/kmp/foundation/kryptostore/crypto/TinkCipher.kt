package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import com.google.crypto.tink.Aead
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal class TinkCipher(private val aeadProvider: AlgorithmProvider<Aead>, private val dispatcher: CoroutineDispatcher) : Cipher {
    override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = withContext(dispatcher) {
        aeadProvider.algorithm.encrypt(message, associatedData)
    }

    override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = withContext(dispatcher) {
        aeadProvider.algorithm.decrypt(message, associatedData)
    }
}
