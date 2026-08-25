@file:OptIn(ExperimentalEncodingApi::class, ExperimentalWasmJsInterop::class)

package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore

import androidx.datastore.core.ReadScope
import androidx.datastore.core.Storage
import androidx.datastore.core.StorageConnection
import androidx.datastore.core.WriteScope
import androidx.datastore.core.okio.OkioSerializer
import okio.Buffer
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.ExperimentalWasmJsInterop

class IndexedDbStorage<T : Any>(private val serializer: OkioSerializer<T>, private val name: String) : Storage<T> {
    override fun createConnection(): StorageConnection<T> = IndexedDbStorageConnection(serializer, name)
}

private class IndexedDbStorageConnection<T : Any>(private val serializer: OkioSerializer<T>, private val name: String) :
    StorageConnection<T> {
    private val connectionCoordinator = WebIndexedDbCoordinator(name)

    override val coordinator
        get() = connectionCoordinator

    override suspend fun <R> readScope(block: suspend ReadScope<T>.(locked: Boolean) -> R): R =
        connectionCoordinator.lock {
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

private open class IndexedDbReadScope<T : Any>(private val serializer: OkioSerializer<T>, private val name: String) :
    ReadScope<T> {
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
