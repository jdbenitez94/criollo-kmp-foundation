package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioStorage
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.StoreRegistry
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.EncryptedProtoSerializer
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.EncryptedStoreOptions
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.ProtoOkioSerializer
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.failClosedCorruptionHandler
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.kryptostoreFileSystem
import kotlinx.serialization.KSerializer
import okio.Path

/**
 * Creates a [DataStore] over a caller-supplied [storage] (Okio, IndexedDB, etc.).
 *
 * Prefer the [producePath] overload for file targets and
 * [createEncryptedProtoDataStoreIndexedDb] for web typed stores.
 *
 * Callers must ensure [io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.CryptoRuntime]
 * is Ready before reading/writing encrypted stores (REQ-STO-05).
 */
fun <T : Any> createEncryptedProtoDataStore(
    storage: Storage<T>,
    migrations: List<DataMigration<T>> = emptyList(),
    corruptionHandler: ReplaceFileCorruptionHandler<T>? = null,
    registry: StoreRegistry? = null,
): DataStore<T> = DataStoreFactory.create(
    storage = storage,
    corruptionHandler = corruptionHandler,
    migrations = migrations,
).also { store ->
    registry?.register(store.asEncryptedStoreHandle())
}

/**
 * File-backed encrypted proto [DataStore] using Okio + platform [kryptostoreFileSystem].
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
    val fileSystem = kryptostoreFileSystem
        ?: error(
            "FileSystem is not available on this platform; " +
                "use createEncryptedProtoDataStoreIndexedDb on JS/Wasm.",
        )
    return createEncryptedProtoDataStore(
        storage = OkioStorage(
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

/**
 * Web typed helper — IndexedDB (proto storage). Prefer this over file paths on JS/Wasm.
 */
expect fun <T : Any> createEncryptedProtoDataStoreIndexedDb(
    cipher: Cipher,
    kSerializer: KSerializer<T>,
    defaultValue: T,
    name: String,
    options: EncryptedStoreOptions = EncryptedStoreOptions().apply { storeName = name },
    migrations: List<DataMigration<T>> = emptyList(),
): DataStore<T>
