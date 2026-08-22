package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.EncryptedStoreOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith

/** REQ-STO-04, REQ-STO-07 */
class PlainPreferencesDataStoreTest {
    @Test
    fun plainPrefs_roundTripWithoutCipher() = runTest {
        val dir = Files.createTempDirectory("kryptostore-plain-prefs").toFile()
        try {
            val path = dir.resolve("remember.preferences_pb").absolutePath.toPath()
            val store = createPlainPreferencesDataStore(producePath = { path })
            val emailKey = stringPreferencesKey("email")
            val flagKey = booleanPreferencesKey("remember")

            store.edit { prefs ->
                prefs[emailKey] = "user@example.com"
                prefs[flagKey] = true
            }

            val prefs = store.data.first()
            assertEquals("user@example.com", prefs[emailKey])
            assertEquals(true, prefs[flagKey])
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun plainPrefs_pathValidation_onAccess() = runTest {
        val store = createPlainPreferencesDataStore(producePath = { "/tmp/not-prefs-ext".toPath() })
        assertFailsWith<IllegalArgumentException> {
            store.data.first()
        }
    }

    @Test
    fun requirePreferencesPbExtension_rejectsBadName() {
        assertFailsWith<IllegalArgumentException> {
            "/tmp/settings.pb".toPath().requirePreferencesPbExtension()
        }
    }
}

/** REQ-STO-03 (LocalStorage factories fail-closed off-web), encrypted prefs file round-trip. */
class EncryptedPreferencesDataStoreTest {
    @Test
    fun encryptedPrefs_fileRoundTrip() = runTest {
        val dir = Files.createTempDirectory("kryptostore-enc-prefs").toFile()
        try {
            val path = dir.resolve("secure.preferences_pb").absolutePath.toPath()
            val key = booleanPreferencesKey("enabled")
            val store = createEncryptedPreferencesDataStore(
                cipher = ReversibleCipher(),
                producePath = { path },
                options = EncryptedStoreOptions().apply { storeName = "secure" },
            )
            store.edit { it[key] = true }
            assertEquals(true, store.data.first()[key])
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun localStorageFactories_failOnJvm() {
        assertFails {
            createPlainPreferencesDataStoreLocalStorage("remember-email")
        }
        assertFails {
            createEncryptedPreferencesDataStoreLocalStorage(
                cipher = ReversibleCipher(),
                name = "secure",
            )
        }
    }
}

private class ReversibleCipher : Cipher {
    override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = message.reversedArray()
    override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = message.reversedArray()
}
