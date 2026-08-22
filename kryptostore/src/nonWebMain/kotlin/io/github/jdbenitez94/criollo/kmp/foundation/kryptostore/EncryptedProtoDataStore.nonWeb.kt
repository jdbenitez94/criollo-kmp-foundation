package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.EncryptedStoreOptions
import kotlinx.serialization.KSerializer

actual fun <T : Any> createEncryptedProtoDataStoreIndexedDb(
    cipher: Cipher,
    kSerializer: KSerializer<T>,
    defaultValue: T,
    name: String,
    options: EncryptedStoreOptions,
    migrations: List<DataMigration<T>>,
): DataStore<T> = error("IndexedDB storage is only available on JS/Wasm targets.")
