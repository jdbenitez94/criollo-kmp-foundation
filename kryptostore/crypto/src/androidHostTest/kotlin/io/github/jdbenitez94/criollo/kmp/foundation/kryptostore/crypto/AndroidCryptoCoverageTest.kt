package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.subtle.AesGcmJce
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class AndroidCryptoCoverageTest {
    @Test
    fun contextHolder_keepsApplicationContext() {
        val context = MemoryContext()
        AndroidCryptoContextHolder.init(context)
        val held = AndroidCryptoContextHolder.applicationContext
        assertTrue(held === context)
        assertTrue(AndroidCryptoContextHolder.applicationContext === held)
    }

    @Test
    fun keysetPersistence_andAndroidRotationCoverEveryTimingPath() = runTest {
        AeadConfig.register()
        val context = MemoryContext()
        val master = AesGcmJce(ByteArray(32) { 6 })
        val associatedData = "alias".encodeToByteArray()
        AndroidCryptoContextHolder.init(context)
        val deviceFactory = androidMasterAeadFactory
        androidMasterAeadFactory = AndroidMasterAeadFactory { master }
        try {
            val stack = createPlatformCryptoStack("platform")
            stack.postRotationInit()
            val bytes = byteArrayOf(9, 2)
            assertContentEquals(bytes, stack.cipher.decrypt(stack.cipher.encrypt(bytes)))
            createAndroidTinkStack("direct").postRotationInit()
        } finally {
            androidMasterAeadFactory = deviceFactory
        }
        val first = loadOrCreateKeyset(context, "keys", "primary", master, associatedData)
        val loaded = loadOrCreateKeyset(context, "keys", "primary", master, associatedData)
        val firstProvider = TinkAeadProvider(first)
        firstProvider.initialize()
        val loadedProvider = TinkAeadProvider(loaded)
        loadedProvider.initialize()
        val plaintext = byteArrayOf(3, 1, 4)
        val encrypted = firstProvider.algorithm.encrypt(plaintext, null)
        assertContentEquals(plaintext, loadedProvider.algorithm.decrypt(encrypted, null))

        val rotationPrefs = context.getSharedPreferences("keys.rotation", Context.MODE_PRIVATE)
        rotationPrefs.edit().clear().apply()
        val rotator = AndroidTimeBasedKeyRotator(
            context,
            loaded,
            loadedProvider,
            master,
            associatedData,
            "primary",
            "keys",
            KeyRotationConfig(rotationPeriod = (-1).milliseconds),
        )
        assertFalse(rotator.rotateKeyIfNeeded())
        assertTrue(rotator.rotateKeyIfNeeded())
        loadedProvider.initialize()

        val current = rotationPrefs.getLong(AndroidTimeBasedKeyRotator.KEY_LAST_ROTATION, 0)
        val withinPeriod = AndroidTimeBasedKeyRotator(
            context,
            loaded,
            loadedProvider,
            master,
            associatedData,
            "primary",
            "keys",
        )
        assertTrue(current > 0)
        assertFalse(withinPeriod.rotateKeyIfNeeded())
    }
}

private class MemoryContext : ContextWrapper(null) {
    private val stores = mutableMapOf<String, SharedPreferences>()

    override fun getApplicationContext(): Context = this

    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
        stores.getOrPut(name) { MemoryPreferences() }
}

private class MemoryPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): Map<String, *> = values.toMap()
    override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
        (values[key] as? Set<String>) ?: defValues

    override fun getInt(key: String, defValue: Int): Int = values[key] as? Int ?: defValue
    override fun getLong(key: String, defValue: Long): Long = values[key] as? Long ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = values[key] as? Float ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
    override fun contains(key: String): Boolean = key in values
    override fun edit(): SharedPreferences.Editor = MemoryEditor(values)
    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit
}

private class MemoryEditor(private val values: MutableMap<String, Any?>) : SharedPreferences.Editor {
    private val pending = mutableMapOf<String, Any?>()
    private var clear = false

    override fun putString(key: String, value: String?) = apply { pending[key] = value }
    override fun putStringSet(key: String, values: Set<String>?) = apply { pending[key] = values }
    override fun putInt(key: String, value: Int) = apply { pending[key] = value }
    override fun putLong(key: String, value: Long) = apply { pending[key] = value }
    override fun putFloat(key: String, value: Float) = apply { pending[key] = value }
    override fun putBoolean(key: String, value: Boolean) = apply { pending[key] = value }
    override fun remove(key: String) = apply { pending[key] = Removed }
    override fun clear() = apply { clear = true }
    override fun commit(): Boolean {
        applyChanges()
        return true
    }
    override fun apply() = applyChanges()

    private fun applyChanges() {
        if (clear) values.clear()
        pending.forEach { (key, value) ->
            if (value === Removed) values.remove(key) else values[key] = value
        }
    }

    private object Removed
}
