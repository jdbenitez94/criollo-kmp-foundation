package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.StoreRegistry
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.EncryptedStoreOptions
import kotlinx.serialization.KSerializer

internal actual fun <T : Any> createNamedEncryptedProtoDataStore(
    cipher: Cipher,
    kSerializer: KSerializer<T>,
    defaultValue: T,
    name: String,
    options: EncryptedStoreOptions,
    migrations: List<DataMigration<T>>,
    registry: StoreRegistry?,
): DataStore<T> = error(
    "StoreLocator.Named (IndexedDB) is only available on JS/Wasm targets.",
)
