package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.preferences

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.WebLocalStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.StoreRegistry
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.EncryptedStoreOptions
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.failClosedCorruptionHandler
import okio.Path.Companion.toPath

internal actual fun createNamedEncryptedPreferencesDataStore(
    cipher: Cipher,
    name: String,
    options: EncryptedStoreOptions,
    migrations: List<DataMigration<Preferences>>,
    registry: StoreRegistry?,
): DataStore<Preferences> {
    val serializer = encryptedPreferencesSerializer(cipher, options)
    return createEncryptedPreferencesDataStore(
        storage = WebLocalStorage(serializer = serializer, name = name),
        migrations = migrations,
        corruptionHandler = failClosedCorruptionHandler(producePath = { name.toPath() }),
        registry = registry,
    )
}

internal actual fun createNamedPlainPreferencesDataStore(
    name: String,
    migrations: List<DataMigration<Preferences>>,
): DataStore<Preferences> = PreferenceDataStoreFactory.create(
    storage = WebLocalStorage(serializer = PreferencesSerializer, name = name),
    migrations = migrations,
)
