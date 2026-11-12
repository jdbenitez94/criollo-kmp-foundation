package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers

import okio.Path

internal actual fun resolveDataStoreFile(fileName: String): Path = error(
    "KryptostorePaths.file is not available on JS/Wasm; " +
        "use StoreLocator.platform(..., name = ...) so routing selects IndexedDB/localStorage.",
)
