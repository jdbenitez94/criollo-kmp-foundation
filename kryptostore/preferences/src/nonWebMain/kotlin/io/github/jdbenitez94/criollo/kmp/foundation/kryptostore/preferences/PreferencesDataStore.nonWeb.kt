package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.preferences

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.EncryptedStoreOptions

actual fun createEncryptedPreferencesDataStoreLocalStorage(
    cipher: Cipher,
    name: String,
    options: EncryptedStoreOptions,
    migrations: List<DataMigration<Preferences>>,
): DataStore<Preferences> = error("WebLocalStorage preferences are only available on JS/Wasm targets.")

actual fun createPlainPreferencesDataStoreLocalStorage(name: String, migrations: List<DataMigration<Preferences>>): DataStore<Preferences> =
    error("WebLocalStorage preferences are only available on JS/Wasm targets.")
