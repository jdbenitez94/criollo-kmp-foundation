package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import kotlinx.serialization.KSerializer

internal actual fun <T : Any> createNamedPlainProtoDataStore(
    kSerializer: KSerializer<T>,
    defaultValue: T,
    name: String,
    migrations: List<DataMigration<T>>,
): DataStore<T> = createPlainProtoDataStore(
    storage = IndexedDbStorage(
        serializer = plainProtoSerializer(kSerializer, defaultValue),
        name = name,
    ),
    migrations = migrations,
)
