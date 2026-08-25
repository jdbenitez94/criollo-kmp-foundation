package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.preferences

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.StoreRegistry
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.EncryptedStoreOptions

internal actual fun createNamedEncryptedPreferencesDataStore(
    cipher: Cipher,
    name: String,
    options: EncryptedStoreOptions,
    migrations: List<DataMigration<Preferences>>,
    registry: StoreRegistry?,
): DataStore<Preferences> = error(
    "StoreLocator.Named (localStorage) is only available on JS/Wasm targets.",
)

internal actual fun createNamedPlainPreferencesDataStore(
    name: String,
    migrations: List<DataMigration<Preferences>>,
): DataStore<Preferences> = error(
    "StoreLocator.Named (localStorage) is only available on JS/Wasm targets.",
)
