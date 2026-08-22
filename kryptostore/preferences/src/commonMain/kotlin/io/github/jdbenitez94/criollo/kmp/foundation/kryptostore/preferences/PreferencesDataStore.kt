package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.preferences

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.EncryptedStoreHandle
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.StoreRegistry
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.EncryptedPreferencesSerializer
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.EncryptedStoreOptions
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.failClosedCorruptionHandler
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.kryptostoreFileSystem
import okio.Path

/**
 * Encrypted Preferences [DataStore] over caller-supplied [storage].
 *
 * Ensure [io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.CryptoRuntime] is Ready
 * before use (REQ-STO-05).
 */
fun createEncryptedPreferencesDataStore(
    storage: Storage<Preferences>,
    migrations: List<DataMigration<Preferences>> = emptyList(),
    corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
    registry: StoreRegistry? = null,
): DataStore<Preferences> = DataStoreFactory.create(
    storage = storage,
    corruptionHandler = corruptionHandler,
    migrations = migrations,
).also { store ->
    registry?.register(EncryptedStoreHandle { store.updateData { it } })
}

/**
 * File-backed encrypted Preferences [DataStore] (Okio). Path must end with `.preferences_pb`.
 */
fun createEncryptedPreferencesDataStore(
    cipher: Cipher,
    producePath: () -> Path,
    options: EncryptedStoreOptions = EncryptedStoreOptions(),
    migrations: List<DataMigration<Preferences>> = emptyList(),
    corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
    registry: StoreRegistry? = null,
): DataStore<Preferences> {
    val serializer = EncryptedPreferencesSerializer(cipher = cipher, options = options)
    val fileSystem = kryptostoreFileSystem
        ?: error(
            "FileSystem is not available on this platform; " +
                "use createEncryptedPreferencesDataStoreLocalStorage on JS/Wasm.",
        )
    return createEncryptedPreferencesDataStore(
        storage = OkioStorage(
            fileSystem = fileSystem,
            serializer = serializer,
            producePath = { producePath().requirePreferencesPbExtension() },
        ),
        migrations = migrations,
        corruptionHandler = corruptionHandler
            ?: failClosedCorruptionHandler(
                producePath = { producePath().requirePreferencesPbExtension() },
            ),
        registry = registry,
    )
}

/**
 * Builds an [EncryptedPreferencesSerializer] for custom [Storage] (e.g. WebLocalStorage).
 */
fun encryptedPreferencesSerializer(cipher: Cipher, options: EncryptedStoreOptions = EncryptedStoreOptions()): EncryptedPreferencesSerializer =
    EncryptedPreferencesSerializer(cipher = cipher, options = options)

/**
 * Plain (unencrypted) Preferences [DataStore] on a file path. Path must end with `.preferences_pb`.
 * Readable without a [Cipher].
 */
fun createPlainPreferencesDataStore(
    producePath: () -> Path,
    migrations: List<DataMigration<Preferences>> = emptyList(),
    corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
    corruptionHandler = corruptionHandler,
    migrations = migrations,
    produceFile = { producePath().requirePreferencesPbExtension() },
)

/** Web encrypted prefs → localStorage. */
expect fun createEncryptedPreferencesDataStoreLocalStorage(
    cipher: Cipher,
    name: String,
    options: EncryptedStoreOptions = EncryptedStoreOptions().apply { storeName = name },
    migrations: List<DataMigration<Preferences>> = emptyList(),
): DataStore<Preferences>

/** Web plain prefs → localStorage. */
expect fun createPlainPreferencesDataStoreLocalStorage(name: String, migrations: List<DataMigration<Preferences>> = emptyList()): DataStore<Preferences>
