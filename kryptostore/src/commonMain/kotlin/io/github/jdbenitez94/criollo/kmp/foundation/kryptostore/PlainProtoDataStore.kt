package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore

import androidx.datastore.core.DataStore
import androidx.datastore.core.Storage
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.ProtoOkioSerializer
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.StoreLocator
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.requireKryptostoreFileSystem
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.resolve
import kotlinx.serialization.KSerializer
import okio.Path
import androidx.datastore.core.DataMigration as PlainDataMigration
import androidx.datastore.core.DataStoreFactory as PlainDataStoreFactory
import androidx.datastore.core.okio.OkioStorage as PlainOkioStorage

/**
 * Plain (unencrypted) typed [DataStore] over caller-supplied [storage] (Okio, IndexedDB, etc.).
 *
 * Readable without a Cipher (unencrypted protobuf on disk / IndexedDB).
 */
fun <T : Any> createPlainProtoDataStore(
    storage: Storage<T>,
    migrations: List<PlainDataMigration<T>> = emptyList(),
    corruptionHandler: ReplaceFileCorruptionHandler<T>? = null,
): DataStore<T> = PlainDataStoreFactory.create(
    storage = storage,
    corruptionHandler = corruptionHandler,
    migrations = migrations,
)

/**
 * Canonical plain typed factory. Prefer [StoreLocator.Platform] from commonMain
 * (Okio on file targets, IndexedDB on JS/Wasm).
 */
fun <T : Any> createPlainProtoDataStore(
    kSerializer: KSerializer<T>,
    defaultValue: T,
    locator: StoreLocator,
    migrations: List<PlainDataMigration<T>> = emptyList(),
    corruptionHandler: ReplaceFileCorruptionHandler<T>? = null,
): DataStore<T> = locator.resolve(
    onFile = { producePath ->
        createPlainProtoDataStore(
            kSerializer = kSerializer,
            defaultValue = defaultValue,
            producePath = producePath,
            migrations = migrations,
            corruptionHandler = corruptionHandler,
        )
    },
    onNamed = { indexedDbName ->
        createNamedPlainProtoDataStore(
            kSerializer = kSerializer,
            defaultValue = defaultValue,
            name = indexedDbName,
            migrations = migrations,
        )
    },
)

/**
 * Okio typed path factory without encryption. Prefer [StoreLocator.File] from shared code.
 */
fun <T : Any> createPlainProtoDataStore(
    kSerializer: KSerializer<T>,
    defaultValue: T,
    producePath: () -> Path,
    migrations: List<PlainDataMigration<T>> = emptyList(),
    corruptionHandler: ReplaceFileCorruptionHandler<T>? = null,
): DataStore<T> {
    val fileSystem = requireKryptostoreFileSystem()
    return createPlainProtoDataStore(
        storage = PlainOkioStorage(
            fileSystem = fileSystem,
            serializer = plainProtoSerializer(kSerializer, defaultValue),
            producePath = producePath,
        ),
        migrations = migrations,
        corruptionHandler = corruptionHandler,
    )
}

/** Builds a [ProtoOkioSerializer] for custom [Storage] (e.g. IndexedDB) without encryption. */
fun <T : Any> plainProtoSerializer(kSerializer: KSerializer<T>, defaultValue: T): ProtoOkioSerializer<T> =
    ProtoOkioSerializer(kSerializer, defaultValue)

internal expect fun <T : Any> createNamedPlainProtoDataStore(
    kSerializer: KSerializer<T>,
    defaultValue: T,
    name: String,
    migrations: List<PlainDataMigration<T>>,
): DataStore<T>
