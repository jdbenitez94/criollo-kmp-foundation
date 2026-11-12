@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore

import androidx.datastore.core.InterProcessCoordinator
import kotlinx.coroutines.await
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise

internal class WebIndexedDbCoordinator(private val name: String) : InterProcessCoordinator {
    private val updates = MutableSharedFlow<Unit>(extraBufferCapacity = 64)

    override val updateNotifications: SharedFlow<Unit>
        get() = updates

    override suspend fun <T> lock(block: suspend () -> T): T = WebIndexedDbLockRegistry.withLock(name) {
        block()
    }

    override suspend fun <T> tryLock(block: suspend (Boolean) -> T): T =
        WebIndexedDbLockRegistry.tryWithLock(name) { acquired ->
            block(acquired)
        }

    override suspend fun getVersion(): Int {
        validateStoreName(name)
        ensureIndexedDbHelpersInstalled()
        return IndexedDbBindings.version(name).awaitJs()?.toString()?.toIntOrNull() ?: 0
    }

    override suspend fun incrementAndGetVersion(): Int {
        validateStoreName(name)
        ensureIndexedDbHelpersInstalled()
        val version = IndexedDbBindings.incrementVersion(name).awaitJs()?.toString()?.toIntOrNull() ?: 0
        updates.tryEmit(Unit)
        return version
    }
}

private suspend fun <T> webLocksExclusive(name: String, block: suspend () -> T): T {
    validateStoreName(name)
    ensureIndexedDbHelpersInstalled()
    IndexedDbBindings.lock(name).awaitJs()
    return try {
        block()
    } finally {
        IndexedDbBindings.unlock(name).awaitJs()
    }
}

internal object WebIndexedDbLockRegistry {
    private class ReentrantLockElement(val heldNames: Set<String>) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<ReentrantLockElement>
    }

    private val registryMutex = Mutex()
    private val mutexes = mutableMapOf<String, Mutex>()

    private suspend fun mutexFor(name: String): Mutex = registryMutex.withLock {
        mutexes.getOrPut(name) { Mutex() }
    }

    private suspend fun heldNamesInCurrentCoroutine(): Set<String> =
        currentCoroutineContext()[ReentrantLockElement]?.heldNames.orEmpty()

    suspend fun <T> withLock(name: String, block: suspend () -> T): T {
        val heldNames = heldNamesInCurrentCoroutine()
        if (name in heldNames) {
            return block()
        }

        val mutex = mutexFor(name)
        return mutex.withLock {
            withContext(ReentrantLockElement(heldNames + name)) {
                webLocksExclusive(name) {
                    block()
                }
            }
        }
    }

    suspend fun <T> tryWithLock(name: String, block: suspend (Boolean) -> T): T {
        val heldNames = heldNamesInCurrentCoroutine()
        if (name in heldNames) {
            return block(true)
        }
        val mutex = mutexFor(name)
        if (!mutex.tryLock()) {
            return block(false)
        }
        return try {
            withContext(ReentrantLockElement(heldNames + name)) {
                validateStoreName(name)
                ensureIndexedDbHelpersInstalled()
                val acquired = IndexedDbBindings.tryLock(name).awaitJs()?.toString()?.toBooleanStrictOrNull() == true
                if (acquired) {
                    try {
                        block(true)
                    } finally {
                        IndexedDbBindings.unlock(name).awaitJs()
                    }
                } else {
                    block(false)
                }
            }
        } finally {
            mutex.unlock()
        }
    }
}

internal fun validateStoreName(name: String) {
    require(name.isNotBlank()) { "IndexedDB store name cannot be blank." }
}

internal fun Char.isAscii(): Boolean = code <= ASCII_MAX_CODE

private const val ASCII_MAX_CODE = 0x7F

internal suspend fun Promise<JsAny?>.awaitJs(): JsAny? = await()

private var indexedDbHelpersInstalled = false

internal fun ensureIndexedDbHelpersInstalled() {
    if (!indexedDbHelpersInstalled) {
        IndexedDbBindings.install()
        indexedDbHelpersInstalled = true
    }
}
