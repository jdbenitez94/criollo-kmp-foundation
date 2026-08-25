package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.preferences

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher as WebPrefsCipher
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.EncryptedStoreOptions as WebPrefsOptions
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.StoreLocator as WebPrefsLocator

/** REQ-STO-03, REQ-STO-04 — web preferences via localStorage. */
@OptIn(ExperimentalTime::class)
class WebLocalStoragePreferencesTest {
    @Test
    fun plainPrefs_namedLocator_persistsAcrossRecreate() = runTest {
        val name = "plain-prefs-${Clock.System.now().toEpochMilliseconds()}"
        val emailKey = stringPreferencesKey("email")
        val first = createPlainPreferencesDataStore(locator = WebPrefsLocator.named(name))
        first.edit { it[emailKey] = "user@example.com" }
        assertEquals("user@example.com", first.data.first()[emailKey])

        val second = createPlainPreferencesDataStore(locator = WebPrefsLocator.named(name))
        assertEquals("user@example.com", second.data.first()[emailKey])
    }

    @Test
    fun encryptedPrefs_platformLocator_persistsAcrossRecreate() = runTest {
        val name = "enc-prefs-${Clock.System.now().toEpochMilliseconds()}"
        val cipher = WebPrefsIdentityCipher()
        val key = stringPreferencesKey("token")
        val locator = WebPrefsLocator.platform(
            producePath = { error("unused on web") },
            name = name,
        )
        val first = createEncryptedPreferencesDataStore(
            cipher = cipher,
            locator = locator,
            options = WebPrefsOptions().apply { storeName = name },
        )
        first.edit { it[key] = "abc" }
        assertEquals("abc", first.data.first()[key])

        val second = createEncryptedPreferencesDataStore(
            cipher = cipher,
            locator = WebPrefsLocator.named(name),
            options = WebPrefsOptions().apply { storeName = name },
        )
        assertEquals("abc", second.data.first()[key])
    }

    @Test
    fun producePath_encrypted_whenFileSystemNull_throws() {
        val error = kotlin.test.assertFails {
            createEncryptedPreferencesDataStore(
                cipher = WebPrefsIdentityCipher(),
                producePath = { error("unused") },
            )
        }
        kotlin.test.assertTrue(error.message!!.contains("FileSystem is not available"))
    }
}

/** Identity cipher — encryption is a no-op for localStorage persistence tests. */
private class WebPrefsIdentityCipher : WebPrefsCipher {
    override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = message.copyOf()
    override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = message.copyOf()
}
