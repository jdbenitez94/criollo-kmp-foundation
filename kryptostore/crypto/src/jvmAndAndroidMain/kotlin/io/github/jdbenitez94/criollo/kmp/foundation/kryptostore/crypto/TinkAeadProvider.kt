package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class TinkAeadProvider(private var keysetHandle: KeysetHandle) : AlgorithmProvider<Aead> {
    private val mutex = Mutex()

    @Volatile
    private var initialized = false

    override lateinit var algorithm: Aead
        private set

    fun replaceKeyset(handle: KeysetHandle) {
        keysetHandle = handle
        initialized = false
    }

    override suspend fun initialize() {
        if (initialized) return
        mutex.withLock {
            if (initialized) return
            val configuration = RegistryConfiguration.get()
            algorithm = keysetHandle.getPrimitive(configuration, Aead::class.java)
            initialized = true
        }
    }
}
