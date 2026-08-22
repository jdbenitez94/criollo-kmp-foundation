package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore

import androidx.datastore.core.DataStore
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.EncryptedStoreHandle

/**
 * [EncryptedStoreHandle] that forces a DataStore rewrite so ciphertext is sealed under the
 * current primary key material after rotation.
 */
fun <T> DataStore<T>.asEncryptedStoreHandle(): EncryptedStoreHandle = EncryptedStoreHandle {
    updateData { it }
}
