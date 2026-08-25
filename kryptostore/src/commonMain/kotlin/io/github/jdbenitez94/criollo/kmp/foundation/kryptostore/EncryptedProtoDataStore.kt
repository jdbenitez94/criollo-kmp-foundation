package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.Storage
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.StoreRegistry
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.EncryptedProtoSerializer
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.EncryptedStoreOptions
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.ProtoOkioSerializer
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.StoreLocator
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.failClosedCorruptionHandler
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.requireKryptostoreFileSystem
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.resolve
import kotlinx.serialization.KSerializer
import okio.Path
import androidx.datastore.core.DataStoreFactory as EncryptedDataStoreFactory
import androidx.datastore.core.okio.OkioStorage as EncryptedOkioStorage

/**
 * Creates a [DataStore] over a caller-supplied [storage] (Okio, IndexedDB, etc.).
 *
 * Callers must ensure [CryptoRuntime] is Ready before reading/writing encrypted stores (REQ-STO-05).
 */
fun <T : Any> createEncryptedProtoDataStore(
    storage: Storage<T>,
    migrations: List<DataMigration<T>> = emptyList(),
    corruptionHandler: ReplaceFileCorruptionHandler<T>? = null,
    registry: StoreRegistry? = null,
): DataStore<T> = EncryptedDataStoreFactory.create(
    storage = storage,
    corruptionHandler = corruptionHandler,
    migrations = migrations,
).also { store ->
    registry?.register(store.asEncryptedStoreHandle())
}

/**
 * Canonical encrypted typed factory. Prefer [StoreLocator.Platform] from commonMain
 * (Okio on file targets, IndexedDB on JS/Wasm).
 */
fun <T : Any> createEncryptedProtoDataStore(
    cipher: Cipher,
    kSerializer: KSerializer<T>,
    defaultValue: T,
    locator: StoreLocator,
    options: EncryptedStoreOptions = EncryptedStoreOptions(),
    migrations: List<DataMigration<T>> = emptyList(),
    corruptionHandler: ReplaceFileCorruptionHandler<T>? = null,
    registry: StoreRegistry? = null,
): DataStore<T> = locator.resolve(
    onFile = { producePath ->
        createEncryptedProtoDataStore(
            cipher = cipher,
            kSerializer = kSerializer,
            defaultValue = defaultValue,
            producePath = producePath,
            options = options,
            migrations = migrations,
            corruptionHandler = corruptionHandler,
            registry = registry,
        )
    },
    onNamed = { webName ->
        options.applyDefaultStoreName(webName)
        createNamedEncryptedProtoDataStore(
            cipher = cipher,
            kSerializer = kSerializer,
            defaultValue = defaultValue,
            name = webName,
            options = options,
            migrations = migrations,
            registry = registry,
        )
    },
)

/**
 * Okio typed path factory with AEAD. Prefer [StoreLocator.File] / [StoreLocator.Platform] from shared code.
 */
fun <T : Any> createEncryptedProtoDataStore(
    cipher: Cipher,
    kSerializer: KSerializer<T>,
    defaultValue: T,
    producePath: () -> Path,
    options: EncryptedStoreOptions = EncryptedStoreOptions(),
    migrations: List<DataMigration<T>> = emptyList(),
    corruptionHandler: ReplaceFileCorruptionHandler<T>? = null,
    registry: StoreRegistry? = null,
): DataStore<T> {
    val serializer = EncryptedProtoSerializer(
        inner = ProtoOkioSerializer(kSerializer, defaultValue),
        cipher = cipher,
        options = options,
    )
    val fileSystem = requireKryptostoreFileSystem()
    return createEncryptedProtoDataStore(
        storage = EncryptedOkioStorage(
            fileSystem = fileSystem,
            serializer = serializer,
            producePath = producePath,
        ),
        migrations = migrations,
        corruptionHandler = corruptionHandler ?: failClosedCorruptionHandler(producePath),
        registry = registry,
    )
}

/**
 * Builds an [EncryptedProtoSerializer] for use with custom [Storage] (e.g. IndexedDB).
 */
fun <T : Any> encryptedProtoSerializer(
    cipher: Cipher,
    kSerializer: KSerializer<T>,
    defaultValue: T,
    options: EncryptedStoreOptions = EncryptedStoreOptions(),
): EncryptedProtoSerializer<T> = EncryptedProtoSerializer(
    inner = ProtoOkioSerializer(kSerializer, defaultValue),
    cipher = cipher,
    options = options,
)

internal expect fun <T : Any> createNamedEncryptedProtoDataStore(
    cipher: Cipher,
    kSerializer: KSerializer<T>,
    defaultValue: T,
    name: String,
    options: EncryptedStoreOptions,
    migrations: List<DataMigration<T>>,
    registry: StoreRegistry?,
): DataStore<T>
