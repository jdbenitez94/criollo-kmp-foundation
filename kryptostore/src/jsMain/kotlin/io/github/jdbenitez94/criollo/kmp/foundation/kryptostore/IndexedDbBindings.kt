@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise

internal actual object IndexedDbBindings {
    private var initialized = false

    private fun ensureInitialized() {
        if (!initialized) {
            evalScript(INDEXED_DB_INIT_SCRIPT)
            initialized = true
        }
    }

    @Suppress("UnusedParameter")
    private fun evalScript(script: String) {
        js("eval(script)")
    }

    actual fun install() {
        ensureInitialized()
        js("globalThis.__kryptostore_idb.install()")
    }

    actual fun read(name: String): Promise<JsAny?> {
        ensureInitialized()
        return js("globalThis.__kryptostore_idb.read(name)")
    }

    actual fun write(name: String, payloadBase64: String): Promise<JsAny?> {
        ensureInitialized()
        return js("globalThis.__kryptostore_idb.write(name, payloadBase64)")
    }

    actual fun version(name: String): Promise<JsAny?> {
        ensureInitialized()
        return js("globalThis.__kryptostore_idb.version(name)")
    }

    actual fun incrementVersion(name: String): Promise<JsAny?> {
        ensureInitialized()
        return js("globalThis.__kryptostore_idb.incrementVersion(name)")
    }

    actual fun lock(name: String): Promise<JsAny?> {
        ensureInitialized()
        return js("globalThis.__kryptostore_idb.lock(name)")
    }

    actual fun tryLock(name: String): Promise<JsAny?> {
        ensureInitialized()
        return js("globalThis.__kryptostore_idb.tryLock(name)")
    }

    actual fun unlock(name: String): Promise<JsAny?> {
        ensureInitialized()
        return js("globalThis.__kryptostore_idb.unlock(name)")
    }
}
