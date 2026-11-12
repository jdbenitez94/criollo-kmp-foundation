package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers

import okio.Path

/**
 * Default on-disk locations for [StoreLocator.Platform] / file-backed factories from commonMain.
 *
 * On Android call [io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.android.KryptostoreAndroid.initialize]
 * once before [file]. JS/Wasm throws — use the [StoreLocator.Platform] name branch instead.
 */
object KryptostorePaths {
    /**
     * Resolves [fileName] under the platform datastore directory (created if needed on file targets).
     */
    fun file(fileName: String): Path {
        require(fileName.isNotBlank()) { "fileName must be non-blank" }
        require(!fileName.contains('/') && !fileName.contains('\\')) {
            "fileName must be a plain file name, not a path: $fileName"
        }
        return resolveDataStoreFile(fileName)
    }
}

internal expect fun resolveDataStoreFile(fileName: String): Path
