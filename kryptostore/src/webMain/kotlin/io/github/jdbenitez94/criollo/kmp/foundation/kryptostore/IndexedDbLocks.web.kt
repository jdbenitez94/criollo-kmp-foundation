@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore

import androidx.datastore.core.InterProcessCoordinator
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    override suspend fun <T> tryLock(block: suspend (Boolean) -> T): T {
        if (WebIndexedDbLockRegistry.isHeld(name)) {
            return block(true)
        }
        return WebIndexedDbLockRegistry.tryWithLock(name) { acquired ->
            block(acquired)
        }
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
    private data class LockState(val mutex: Mutex, var reentrantDepth: Int = 0)

    private val registryMutex = Mutex()
    private val states = mutableMapOf<String, LockState>()

    private suspend fun stateFor(name: String): LockState = registryMutex.withLock {
        states.getOrPut(name) { LockState(Mutex()) }
    }

    suspend fun isHeld(name: String): Boolean = registryMutex.withLock {
        (states[name]?.reentrantDepth ?: 0) > 0
    }

    suspend fun <T> withLock(name: String, block: suspend () -> T): T {
        val state = stateFor(name)
        if (state.reentrantDepth > 0) {
            state.reentrantDepth++
            return try {
                block()
            } finally {
                state.reentrantDepth--
            }
        }

        return state.mutex.withLock {
            state.reentrantDepth = 1
            try {
                webLocksExclusive(name) {
                    block()
                }
            } finally {
                state.reentrantDepth = 0
            }
        }
    }

    suspend fun <T> tryWithLock(name: String, block: suspend (Boolean) -> T): T {
        val state = stateFor(name)
        if (state.reentrantDepth > 0) {
            state.reentrantDepth++
            return try {
                block(true)
            } finally {
                state.reentrantDepth--
            }
        }
        if (!state.mutex.tryLock()) {
            return block(false)
        }
        state.reentrantDepth = 1
        return try {
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
        } finally {
            state.reentrantDepth = 0
            state.mutex.unlock()
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
