package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers

import okio.Path

/**
 * Where a KryptoStore [androidx.datastore.core.DataStore] persists.
 *
 * Prefer [Platform] from commonMain: file targets use [Platform.producePath], JS/Wasm use
 * [Platform.name] (IndexedDB for typed proto, localStorage for preferences).
 *
 * [File] / [Named] are explicit single-backend escape hatches (fail closed on the wrong target).
 */
sealed interface StoreLocator {
    /**
     * Dual identity with automatic platform routing.
     * - Android / JVM / iOS → Okio via [producePath]
     * - JS / Wasm → named web storage via [name]
     */
    data class Platform(val producePath: () -> Path, val name: String) : StoreLocator {
        init {
            require(name.isNotBlank()) { "StoreLocator.Platform name must be non-blank" }
        }
    }

    /** File-backed only (Okio). */
    data class File(val producePath: () -> Path) : StoreLocator

    /**
     * Named web store only (non-blank).
     * Typed proto → IndexedDB; preferences → localStorage.
     */
    data class Named(val name: String) : StoreLocator {
        init {
            require(name.isNotBlank()) { "StoreLocator.Named name must be non-blank" }
        }
    }

    companion object {
        /** Dual identity — preferred for shared commonMain factories. */
        fun platform(producePath: () -> Path, name: String): StoreLocator = Platform(producePath, name)

        /** File-only escape hatch. */
        fun file(producePath: () -> Path): StoreLocator = File(producePath)

        /** Named-only escape hatch (web). */
        fun named(name: String): StoreLocator = Named(name)
    }
}

/**
 * Resolves this locator to a file path or web name for the current target.
 */
fun <R> StoreLocator.resolve(onFile: (producePath: () -> Path) -> R, onNamed: (name: String) -> R): R =
    resolve(usesFileStorage = kryptostoreUsesFileStorage, onFile = onFile, onNamed = onNamed)

/**
 * Same as [resolve] with an explicit [usesFileStorage] override (tests / tooling).
 */
fun <R> StoreLocator.resolve(
    usesFileStorage: Boolean,
    onFile: (producePath: () -> Path) -> R,
    onNamed: (name: String) -> R,
): R = when (this) {
    is StoreLocator.Platform ->
        if (usesFileStorage) {
            onFile(producePath)
        } else {
            onNamed(name)
        }

    is StoreLocator.File -> onFile(producePath)

    is StoreLocator.Named -> onNamed(name)
}
