@file:OptIn(ExperimentalEncodingApi::class, ExperimentalWasmJsInterop::class)

package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore

import androidx.datastore.core.InterProcessCoordinator
import androidx.datastore.core.ReadScope
import androidx.datastore.core.Storage
import androidx.datastore.core.StorageConnection
import androidx.datastore.core.WriteScope
import androidx.datastore.core.okio.OkioSerializer
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.Buffer
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise

class IndexedDbStorage<T : Any>(private val serializer: OkioSerializer<T>, private val name: String) : Storage<T> {
    override fun createConnection(): StorageConnection<T> = IndexedDbStorageConnection(serializer, name)
}

private class IndexedDbStorageConnection<T : Any>(private val serializer: OkioSerializer<T>, private val name: String) : StorageConnection<T> {
    private val connectionCoordinator = WebIndexedDbCoordinator(name)

    override val coordinator: InterProcessCoordinator
        get() = connectionCoordinator

    override suspend fun <R> readScope(block: suspend ReadScope<T>.(locked: Boolean) -> R): R = connectionCoordinator.lock {
        val scope = IndexedDbReadScope(serializer, name)
        try {
            block(scope, true)
        } finally {
            scope.close()
        }
    }

    override suspend fun writeScope(block: suspend WriteScope<T>.() -> Unit) {
        connectionCoordinator.lock {
            val scope = IndexedDbWriteScope(serializer, name)
            try {
                block(scope)
            } finally {
                scope.close()
            }
        }
    }

    override fun close() = Unit
}

private open class IndexedDbReadScope<T : Any>(private val serializer: OkioSerializer<T>, private val name: String) : ReadScope<T> {
    override suspend fun readData(): T {
        validateStoreName(name)
        ensureIndexedDbHelpersInstalled()
        val payload = IndexedDbBindings.read(name).awaitJs()?.toString()
        if (payload == null || payload == "null") return serializer.defaultValue
        val bytes = Base64.decode(payload)
        return serializer.readFrom(Buffer().write(bytes))
    }

    override fun close() = Unit
}

private class IndexedDbWriteScope<T : Any>(private val serializer: OkioSerializer<T>, private val name: String) :
    IndexedDbReadScope<T>(serializer, name),
    WriteScope<T> {
    override suspend fun writeData(value: T) {
        validateStoreName(name)
        ensureIndexedDbHelpersInstalled()
        val buffer = Buffer()
        serializer.writeTo(value, buffer)
        val payloadBase64 = Base64.encode(buffer.readByteArray())
        require(payloadBase64.all(Char::isAscii)) { "IndexedDB payload must be Base64 ASCII." }
        IndexedDbBindings.write(name, payloadBase64).awaitJs()
    }
}

private class WebIndexedDbCoordinator(private val name: String) : InterProcessCoordinator {
    private val updates = MutableSharedFlow<Unit>(extraBufferCapacity = 64)

    override val updateNotifications: SharedFlow<Unit>
        get() = updates

    override suspend fun <T> lock(block: suspend () -> T): T = WebIndexedDbLockRegistry.withLock(name) {
        block()
    }

    override suspend fun <T> tryLock(block: suspend (Boolean) -> T): T {
        // Re-entrant: same coroutine already holds the lock.
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

private object WebIndexedDbLockRegistry {
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

private fun validateStoreName(name: String) {
    require(name.isNotBlank()) { "IndexedDB store name cannot be blank." }
}

private fun Char.isAscii(): Boolean = code <= ASCII_MAX_CODE

private const val ASCII_MAX_CODE = 0x7F

private suspend fun Promise<JsAny?>.awaitJs(): JsAny? = await()

private var indexedDbHelpersInstalled = false

private fun ensureIndexedDbHelpersInstalled() {
    if (!indexedDbHelpersInstalled) {
        IndexedDbBindings.install()
        indexedDbHelpersInstalled = true
    }
}
