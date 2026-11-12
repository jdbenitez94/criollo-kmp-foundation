package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.StoreRegistry
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.EncryptedStoreOptions
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.failClosedCorruptionHandler
import kotlinx.serialization.KSerializer
import okio.Path.Companion.toPath

internal actual fun <T : Any> createNamedEncryptedProtoDataStore(
    cipher: Cipher,
    kSerializer: KSerializer<T>,
    defaultValue: T,
    name: String,
    options: EncryptedStoreOptions,
    migrations: List<DataMigration<T>>,
    registry: StoreRegistry?,
): DataStore<T> {
    val serializer = encryptedProtoSerializer(cipher, kSerializer, defaultValue, options)
    return createEncryptedProtoDataStore(
        storage = IndexedDbStorage(serializer = serializer, name = name),
        migrations = migrations,
        corruptionHandler = failClosedCorruptionHandler(producePath = { name.toPath() }),
        registry = registry,
    )
}
