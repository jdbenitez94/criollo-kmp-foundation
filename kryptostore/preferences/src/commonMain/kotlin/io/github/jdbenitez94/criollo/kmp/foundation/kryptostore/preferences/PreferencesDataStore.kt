package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.EncryptedStoreHandle
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.StoreRegistry
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.EncryptedPreferencesSerializer
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.EncryptedStoreOptions
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.failClosedCorruptionHandler
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.requireKryptostoreFileSystem
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.resolve
import okio.Path
import androidx.datastore.core.DataMigration as PrefsDataMigration
import androidx.datastore.core.DataStore as PrefsDataStore
import androidx.datastore.core.DataStoreFactory as PrefsDataStoreFactory
import androidx.datastore.core.Storage as PrefsStorage
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler as PrefsCorruptionHandler
import androidx.datastore.core.okio.OkioStorage as PrefsOkioStorage
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.StoreLocator as PrefsStoreLocator

/**
 * Encrypted Preferences [PrefsDataStore] over caller-supplied [storage].
 *
 * Ensure [CryptoRuntime] is Ready before use (REQ-STO-05).
 */
fun createEncryptedPreferencesDataStore(
    storage: PrefsStorage<Preferences>,
    migrations: List<PrefsDataMigration<Preferences>> = emptyList(),
    corruptionHandler: PrefsCorruptionHandler<Preferences>? = null,
    registry: StoreRegistry? = null,
): PrefsDataStore<Preferences> = PrefsDataStoreFactory.create(
    storage = storage,
    corruptionHandler = corruptionHandler,
    migrations = migrations,
).also { store ->
    registry?.register(EncryptedStoreHandle { store.updateData { it } })
}

/**
 * Canonical encrypted Preferences factory. Prefer [PrefsStoreLocator.Platform] from commonMain
 * (Okio on file targets, localStorage on JS/Wasm). Path must end with `.preferences_pb` when resolved to file.
 */
fun createEncryptedPreferencesDataStore(
    cipher: Cipher,
    locator: PrefsStoreLocator,
    options: EncryptedStoreOptions = EncryptedStoreOptions(),
    migrations: List<PrefsDataMigration<Preferences>> = emptyList(),
    corruptionHandler: PrefsCorruptionHandler<Preferences>? = null,
    registry: StoreRegistry? = null,
): PrefsDataStore<Preferences> = locator.resolve(
    onFile = { pathProducer ->
        createEncryptedPreferencesDataStore(
            cipher = cipher,
            producePath = pathProducer,
            options = options,
            migrations = migrations,
            corruptionHandler = corruptionHandler,
            registry = registry,
        )
    },
    onNamed = { localStorageKey ->
        options.applyDefaultStoreName(localStorageKey)
        return@resolve encryptedPreferencesForLocalStorage(
            cipher = cipher,
            localStorageKey = localStorageKey,
            options = options,
            migrations = migrations,
            registry = registry,
        )
    },
)

private fun encryptedPreferencesForLocalStorage(
    cipher: Cipher,
    localStorageKey: String,
    options: EncryptedStoreOptions,
    migrations: List<PrefsDataMigration<Preferences>>,
    registry: StoreRegistry?,
): PrefsDataStore<Preferences> = createNamedEncryptedPreferencesDataStore(
    cipher = cipher,
    name = localStorageKey,
    options = options,
    migrations = migrations,
    registry = registry,
)

/**
 * Okio Preferences path factory (encrypted). Requires a `.preferences_pb` suffix.
 */
fun createEncryptedPreferencesDataStore(
    cipher: Cipher,
    producePath: () -> Path,
    options: EncryptedStoreOptions = EncryptedStoreOptions(),
    migrations: List<PrefsDataMigration<Preferences>> = emptyList(),
    corruptionHandler: PrefsCorruptionHandler<Preferences>? = null,
    registry: StoreRegistry? = null,
): PrefsDataStore<Preferences> {
    val serializer = EncryptedPreferencesSerializer(cipher = cipher, options = options)
    val fileSystem = requireKryptostoreFileSystem()
    return createEncryptedPreferencesDataStore(
        storage = PrefsOkioStorage(
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
 * Builds an [EncryptedPreferencesSerializer] for custom [PrefsStorage] (e.g. WebLocalStorage).
 */
fun encryptedPreferencesSerializer(
    cipher: Cipher,
    options: EncryptedStoreOptions = EncryptedStoreOptions(),
): EncryptedPreferencesSerializer = EncryptedPreferencesSerializer(cipher = cipher, options = options)

/**
 * Canonical plain Preferences factory. Prefer [PrefsStoreLocator.Platform] from commonMain
 * (Okio on file targets, localStorage on JS/Wasm).
 */
fun createPlainPreferencesDataStore(
    locator: PrefsStoreLocator,
    migrations: List<PrefsDataMigration<Preferences>> = emptyList(),
    corruptionHandler: PrefsCorruptionHandler<Preferences>? = null,
): PrefsDataStore<Preferences> = locator.resolve(
    onFile = { pathProducer ->
        createPlainPreferencesDataStore(
            producePath = pathProducer,
            migrations = migrations,
            corruptionHandler = corruptionHandler,
        )
    },
    onNamed = { key ->
        // Prefer named expect over a private helper (keeps jscpd distinct from typed proto).
        createNamedPlainPreferencesDataStore(name = key, migrations = migrations)
    },
)

/**
 * Okio Preferences path factory (unencrypted). Requires a `.preferences_pb` suffix.
 * Prefer [PrefsStoreLocator.File] / [PrefsStoreLocator.Platform] from shared code.
 */
fun createPlainPreferencesDataStore(
    producePath: () -> Path,
    migrations: List<PrefsDataMigration<Preferences>> = emptyList(),
    corruptionHandler: PrefsCorruptionHandler<Preferences>? = null,
): PrefsDataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
    corruptionHandler = corruptionHandler,
    migrations = migrations,
    produceFile = { producePath().requirePreferencesPbExtension() },
)

internal expect fun createNamedEncryptedPreferencesDataStore(
    cipher: Cipher,
    name: String,
    options: EncryptedStoreOptions,
    migrations: List<PrefsDataMigration<Preferences>>,
    registry: StoreRegistry?,
): PrefsDataStore<Preferences>

internal expect fun createNamedPlainPreferencesDataStore(
    name: String,
    migrations: List<PrefsDataMigration<Preferences>>,
): PrefsDataStore<Preferences>
