package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher as PrefsTestCipher
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.EncryptedStoreOptions as PrefsTestOptions
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.StoreLocator as PrefsTestLocator

/** REQ-STO-04, REQ-STO-07 */
class PlainPreferencesDataStoreTest {
    @Test
    fun plainPrefs_roundTripWithoutCipher() = runTest {
        val dir = Files.createTempDirectory("kryptostore-plain-prefs").toFile()
        try {
            val path = dir.resolve("remember.preferences_pb").absolutePath.toPath()
            val store = createPlainPreferencesDataStore(
                locator = PrefsTestLocator.platform(producePath = { path }, name = "remember"),
            )
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
    fun namedLocator_onJvm_failsClosed() {
        assertFails {
            createPlainPreferencesDataStore(locator = PrefsTestLocator.named("remember-email"))
        }
    }

    @Test
    fun requirePreferencesPbExtension_rejectsBadName() {
        assertFailsWith<IllegalArgumentException> {
            "/tmp/settings.pb".toPath().requirePreferencesPbExtension()
        }
    }
}

/** REQ-STO-03 (Named fail-closed off-web), encrypted prefs file round-trip. */
class EncryptedPreferencesDataStoreTest {
    @Test
    fun encryptedPrefs_fileRoundTrip() = runTest {
        val dir = Files.createTempDirectory("kryptostore-enc-prefs").toFile()
        try {
            val path = dir.resolve("secure.preferences_pb").absolutePath.toPath()
            val key = booleanPreferencesKey("enabled")
            val store = createEncryptedPreferencesDataStore(
                cipher = PreferencesXorCipher(),
                locator = PrefsTestLocator.platform(producePath = { path }, name = "secure"),
                options = PrefsTestOptions().apply { storeName = "secure" },
            )
            store.edit { it[key] = true }
            assertEquals(true, store.data.first()[key])
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun namedLocator_onJvm_failsClosed() {
        assertFails {
            createEncryptedPreferencesDataStore(
                cipher = PreferencesXorCipher(),
                locator = PrefsTestLocator.named("secure"),
            )
        }
    }

    @Test
    fun createEncrypted_storageOnly_usesDefaults() = runTest {
        val dir = Files.createTempDirectory("kryptostore-prefs-storage").toFile()
        try {
            val path = dir.resolve("secure.preferences_pb").absolutePath.toPath()
            val serializer = encryptedPreferencesSerializer(cipher = PreferencesXorCipher())
            val store = createEncryptedPreferencesDataStore(
                storage = androidx.datastore.core.okio.OkioStorage(
                    fileSystem = okio.FileSystem.SYSTEM,
                    serializer = serializer,
                    producePath = { path },
                ),
            )
            val key = booleanPreferencesKey("enabled")
            store.edit { it[key] = true }
            assertEquals(true, store.data.first()[key])
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun createEncrypted_producePath_omitsOptionalArgs() = runTest {
        val dir = Files.createTempDirectory("kryptostore-prefs-omit").toFile()
        try {
            val path = dir.resolve("secure.preferences_pb").absolutePath.toPath()
            val store = createEncryptedPreferencesDataStore(
                cipher = PreferencesXorCipher(),
                producePath = { path },
            )
            val key = booleanPreferencesKey("enabled")
            store.edit { it[key] = false }
            assertEquals(false, store.data.first()[key])
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun createEncrypted_withRegistry_reEncrypts() = runTest {
        val dir = Files.createTempDirectory("kryptostore-prefs-reg").toFile()
        try {
            val path = dir.resolve("secure.preferences_pb").absolutePath.toPath()
            val registry = io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.StoreRegistry()
            val store = createEncryptedPreferencesDataStore(
                cipher = PreferencesXorCipher(),
                producePath = { path },
                options = PrefsTestOptions().apply { storeName = "secure" },
                registry = registry,
            )
            val key = booleanPreferencesKey("enabled")
            store.edit { it[key] = true }
            assertEquals(1, registry.size())
            registry.reEncryptAll()
            assertEquals(true, store.data.first()[key])
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun defaultCorruptionHandler_quarantinesOnCorruption() = runTest {
        val dir = Files.createTempDirectory("kryptostore-prefs-corrupt").toFile()
        try {
            val path = dir.resolve("secure.preferences_pb").absolutePath.toPath()
            // Seed unreadable bytes so the first read trips the default fail-closed handler.
            okio.FileSystem.SYSTEM.write(path) { writeUtf8("not-a-valid-prefs-envelope") }
            val store = createEncryptedPreferencesDataStore(
                cipher = PreferencesXorCipher(),
                producePath = { path },
                options = PrefsTestOptions().apply { storeName = "secure" },
            )
            assertFailsWith<androidx.datastore.core.CorruptionException> {
                store.data.first()
            }
            assertTrue(okio.FileSystem.SYSTEM.exists("$path.corrupt".toPath()) || !okio.FileSystem.SYSTEM.exists(path))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun encryptedPreferencesSerializer_omitsOptions() = runTest {
        val serializer = encryptedPreferencesSerializer(cipher = PreferencesXorCipher())
        val buffer = okio.Buffer()
        val empty = serializer.defaultValue
        serializer.writeTo(empty, buffer)
        assertEquals(empty, serializer.readFrom(okio.Buffer().write(buffer.readByteArray())))
    }
}

private class PreferencesXorCipher : PrefsTestCipher {
    override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?): ByteArray =
        ByteArray(message.size) { index -> (message[index].toInt() xor 0x5A).toByte() }

    override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray =
        encrypt(message, associatedData)
}
