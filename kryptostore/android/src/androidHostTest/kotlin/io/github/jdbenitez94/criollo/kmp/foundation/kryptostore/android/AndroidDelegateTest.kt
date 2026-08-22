package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.android

import android.content.Context
import androidx.datastore.core.DataStore
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.Serializable
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs
import kotlin.test.Test

/** REQ-AND-01..04 — Context delegate singleton semantics (androidHostTest). */
class AndroidDelegateTest {
    @Test
    fun encryptedProtoDataStore_delegateKeysByFileName() {
        val cipher = IdentityCipher()
        val delegate = encryptedProtoDataStore(
            fileName = "settings.pb",
            kSerializer = SampleSettings.serializer(),
            defaultValue = SampleSettings(),
            cipher = { cipher },
        )
        expectThat(delegate.toString()).isEqualTo("ContextDataStoreSingleton(settings.pb)")
    }

    @Test
    fun contextDataStoreSingleton_returnsSameInstance() {
        var creates = 0
        val delegate = ContextDataStoreSingleton("key") {
            creates++
            FakeDataStore
        }
        val first = delegate.getValue(FakeContext, TestProps::unused)
        val second = delegate.getValue(FakeContext, TestProps::unused)
        expectThat(first).isSameInstanceAs(second)
        expectThat(creates).isEqualTo(1)
    }

    @Test
    fun encryptedPreferencesDataStore_usesPreferencesPbSuffix() {
        val delegate = encryptedPreferencesDataStore(
            name = "secure",
            cipher = { IdentityCipher() },
        )
        expectThat(delegate.toString()).isEqualTo("ContextDataStoreSingleton(secure.preferences_pb)")
    }

    @Test
    fun plainPreferencesDataStore_usesPreferencesPbSuffix() {
        val delegate = plainPreferencesDataStore(name = "plain")
        expectThat(delegate.toString()).isEqualTo("ContextDataStoreSingleton(plain.preferences_pb)")
    }
}

private object TestProps {
    val unused: DataStore<SampleSettings>? = null
}

@Serializable
private data class SampleSettings(val theme: String = "system")

private class IdentityCipher : Cipher {
    override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = message
    override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = message
}

private object FakeDataStore : DataStore<SampleSettings> {
    override val data = flowOf(SampleSettings())
    override suspend fun updateData(transform: suspend (SampleSettings) -> SampleSettings): SampleSettings = transform(SampleSettings())
}

private object FakeContext : android.content.ContextWrapper(null) {
    override fun getApplicationContext(): Context = this
}
