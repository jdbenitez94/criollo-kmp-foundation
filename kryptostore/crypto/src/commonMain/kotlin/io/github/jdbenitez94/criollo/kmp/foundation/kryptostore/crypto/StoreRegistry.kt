package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Handle for an encrypted store that can rewrite ciphertext under the current [Cipher]
 * after key rotation (REQ-ROT-01).
 */
fun interface EncryptedStoreHandle {
    suspend fun reEncryptInPlace()
}

/**
 * Registry of encrypted stores to re-encrypt when [KeyRotator.rotateKeyIfNeeded] returns true.
 *
 * Register stores during app startup (before concurrent [CryptoRuntime.initialize]).
 * [reEncryptAll] runs handles sequentially so a failure fails closed (REQ-ROT-03).
 */
class StoreRegistry {
    private val mutex = Mutex()
    private val handles = mutableListOf<EncryptedStoreHandle>()

    /**
     * Registers [handle] if not already present.
     *
     * Must not race with [reEncryptAll]; call during single-threaded startup.
     */
    fun register(handle: EncryptedStoreHandle) {
        check(mutex.tryLock()) {
            "StoreRegistry.register contended; register stores before CryptoRuntime.initialize()"
        }
        try {
            if (handle !in handles) {
                handles += handle
            }
        } finally {
            mutex.unlock()
        }
    }

    fun unregister(handle: EncryptedStoreHandle) {
        check(mutex.tryLock()) {
            "StoreRegistry.unregister contended; unregister outside of initialize()"
        }
        try {
            handles.remove(handle)
        } finally {
            mutex.unlock()
        }
    }

    suspend fun reEncryptAll() {
        val snapshot = mutex.withLock { handles.toList() }
        for (handle in snapshot) {
            handle.reEncryptInPlace()
        }
    }

    /** Test / diagnostics: number of registered handles. */
    fun size(): Int {
        check(mutex.tryLock()) { "StoreRegistry.size contended" }
        try {
            return handles.size
        } finally {
            mutex.unlock()
        }
    }
}
