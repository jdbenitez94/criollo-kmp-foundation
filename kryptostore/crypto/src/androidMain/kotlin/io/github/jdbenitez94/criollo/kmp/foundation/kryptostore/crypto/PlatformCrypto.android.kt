package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import android.content.Context
import android.content.SharedPreferences
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.TinkProtoKeysetFormat
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.integration.android.AndroidKeystore
import com.google.crypto.tink.subtle.Hex
import kotlinx.coroutines.Dispatchers
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal fun createAndroidTinkStack(appId: String, rotationConfig: KeyRotationConfig = KeyRotationConfig.DEFAULT): PlatformCryptoStack {
    AeadConfig.register()
    val context = AndroidCryptoContextHolder.applicationContext
    val masterKeyAlias = "$appId.master_key"
    val keysetName = "$appId.keyset"
    val preferenceFile = "$appId.keyset_prefs"
    val associatedData = masterKeyAlias.toByteArray()

    if (!AndroidKeystore.hasKey(masterKeyAlias)) {
        AndroidKeystore.generateNewAes256GcmKey(masterKeyAlias)
    }
    val masterAead = AndroidKeystore.getAead(masterKeyAlias)

    val keysetHandle = loadOrCreateKeyset(context, preferenceFile, keysetName, masterAead, associatedData)
    val aeadProvider = TinkAeadProvider(keysetHandle)
    val cipher = TinkCipher(aeadProvider, Dispatchers.Default)
    val keyRotator = AndroidTimeBasedKeyRotator(
        context = context,
        keysetHandle = keysetHandle,
        aeadProvider = aeadProvider,
        keysetEncryptionAead = masterAead,
        associatedData = associatedData,
        keysetName = keysetName,
        preferenceFileName = preferenceFile,
        config = rotationConfig,
    )

    return PlatformCryptoStack(
        cipher = cipher,
        keyRotator = keyRotator,
        postRotationInit = { aeadProvider.initialize() },
    )
}

private fun SharedPreferences.editCommit(block: SharedPreferences.Editor.() -> Unit) {
    edit().apply {
        block()
        apply()
    }
}

private fun loadOrCreateKeyset(context: Context, preferenceFile: String, keysetName: String, masterAead: Aead, associatedData: ByteArray): KeysetHandle {
    val prefs = context.getSharedPreferences(preferenceFile, Context.MODE_PRIVATE)
    val encryptedKeysetHex = prefs.getString(keysetName, null)
    return if (encryptedKeysetHex != null) {
        val serialized = Hex.decode(encryptedKeysetHex)
        TinkProtoKeysetFormat.parseEncryptedKeyset(serialized, masterAead, associatedData)
    } else {
        val newHandle = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
        val encryptedKeyset = TinkProtoKeysetFormat.serializeEncryptedKeyset(newHandle, masterAead, associatedData)
        prefs.editCommit { putString(keysetName, Hex.encode(encryptedKeyset)) }
        val now = Clock.System.now().toEpochMilliseconds()
        context.getSharedPreferences("$preferenceFile.rotation", Context.MODE_PRIVATE).editCommit {
            putLong(AndroidTimeBasedKeyRotator.KEY_LAST_ROTATION, now)
        }
        newHandle
    }
}

private class AndroidTimeBasedKeyRotator(
    private val context: Context,
    private var keysetHandle: KeysetHandle,
    private val aeadProvider: TinkAeadProvider,
    private val keysetEncryptionAead: Aead,
    private val associatedData: ByteArray,
    private val keysetName: String,
    private val preferenceFileName: String,
    private val config: KeyRotationConfig = KeyRotationConfig.DEFAULT,
) : KeyRotator {
    override suspend fun rotateKeyIfNeeded(): Boolean {
        val prefs = context.getSharedPreferences("$preferenceFileName.rotation", Context.MODE_PRIVATE)
        val lastRotation = prefs.getLong(KEY_LAST_ROTATION, 0L)
        val now = Clock.System.now().toEpochMilliseconds()
        if (isWithinRotationPeriod(lastRotation, now, config.rotationPeriod.inWholeMilliseconds)) {
            return false
        }
        if (isUnsetRotationStamp(lastRotation)) {
            prefs.editCommit { putLong(KEY_LAST_ROTATION, now) }
            return false
        }
        val newHandle = keysetHandle.withRotatedAes256GcmPrimary()
        val encrypted = newHandle.serializeEncryptedKeyset(keysetEncryptionAead, associatedData)
        context.getSharedPreferences(preferenceFileName, Context.MODE_PRIVATE).editCommit {
            putString(keysetName, Hex.encode(encrypted))
        }
        prefs.editCommit { putLong(KEY_LAST_ROTATION, now) }
        keysetHandle = newHandle
        aeadProvider.replaceKeyset(newHandle)
        return true
    }

    companion object {
        const val KEY_LAST_ROTATION = "last_rotation_timestamp"
    }
}

actual fun createPlatformCryptoStack(appId: String, rotationConfig: KeyRotationConfig): PlatformCryptoStack = createAndroidTinkStack(appId, rotationConfig)
