package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.TinkProtoKeysetFormat
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.util.EnumSet
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal fun createJvmTinkStack(appId: String, rotationConfig: KeyRotationConfig = KeyRotationConfig.DEFAULT): PlatformCryptoStack {
    AeadConfig.register()
    val baseDir = File(System.getProperty("user.home"), ".kryptostore/$appId").apply {
        mkdirs()
        sealPosixDirectory(this)
    }
    val masterAead = JvmMasterKeyAead(appId)
    val keysetName = "keyset"
    val keysetFile = File(baseDir, "$keysetName.enc")
    val rotationFile = File(baseDir, "rotation.properties")
    val associatedData = appId.toByteArray()

    val keysetHandle = loadOrCreateJvmKeyset(keysetFile, rotationFile, masterAead, associatedData)
    val aeadProvider = TinkAeadProvider(keysetHandle)
    val cipher = TinkCipher(aeadProvider, Dispatchers.Default)
    val keyRotator = JvmTimeBasedKeyRotator(
        keysetHandle = keysetHandle,
        aeadProvider = aeadProvider,
        keysetFile = keysetFile,
        rotationFile = rotationFile,
        masterAead = masterAead,
        associatedData = associatedData,
        config = rotationConfig,
    )

    return PlatformCryptoStack(
        cipher = cipher,
        keyRotator = keyRotator,
        postRotationInit = { aeadProvider.initialize() },
    )
}

private fun loadOrCreateJvmKeyset(keysetFile: File, rotationFile: File, masterAead: Aead, associatedData: ByteArray): KeysetHandle {
    if (keysetFile.exists() && keysetFile.length() > 0) {
        val serialized = keysetFile.readBytes()
        return TinkProtoKeysetFormat.parseEncryptedKeyset(serialized, masterAead, associatedData)
    }
    val newHandle = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
    val encrypted = TinkProtoKeysetFormat.serializeEncryptedKeyset(newHandle, masterAead, associatedData)
    keysetFile.writeBytes(encrypted)
    sealPosixFile(keysetFile)
    // Stamp creation time so the first initialize() does not treat "never rotated" as overdue.
    val now = Clock.System.now().toEpochMilliseconds()
    rotationFile.writeText(now.toString())
    sealPosixFile(rotationFile)
    return newHandle
}

private class JvmTimeBasedKeyRotator(
    private var keysetHandle: KeysetHandle,
    private val aeadProvider: TinkAeadProvider,
    private val keysetFile: File,
    private val rotationFile: File,
    private val masterAead: Aead,
    private val associatedData: ByteArray,
    private val config: KeyRotationConfig = KeyRotationConfig.DEFAULT,
) : KeyRotator {
    override suspend fun rotateKeyIfNeeded(): Boolean {
        val lastRotation = rotationFile.takeIf { it.exists() }?.readText()?.toLongOrNull() ?: 0L
        val now = Clock.System.now().toEpochMilliseconds()
        if (lastRotation != 0L && (now - lastRotation) <= config.rotationPeriod.inWholeMilliseconds) {
            return false
        }
        if (lastRotation == 0L) {
            // Existing install without a stamp: record now without rotating.
            rotationFile.writeText(now.toString())
            sealPosixFile(rotationFile)
            return false
        }
        val newEntry = KeysetHandle.generateEntryFromParameters(PredefinedAeadParameters.AES256_GCM)
            .withRandomId()
            .makePrimary()
        val newHandle = KeysetHandle.newBuilder(keysetHandle).addEntry(newEntry).build()
        val encrypted = TinkProtoKeysetFormat.serializeEncryptedKeyset(newHandle, masterAead, associatedData)
        keysetFile.writeBytes(encrypted)
        sealPosixFile(keysetFile)
        rotationFile.writeText(now.toString())
        sealPosixFile(rotationFile)
        keysetHandle = newHandle
        aeadProvider.replaceKeyset(newHandle)
        return true
    }
}

actual fun createPlatformCryptoStack(appId: String, rotationConfig: KeyRotationConfig): PlatformCryptoStack = createJvmTinkStack(appId, rotationConfig)

private val posixDirectoryPermissions = EnumSet.of(
    PosixFilePermission.OWNER_READ,
    PosixFilePermission.OWNER_WRITE,
    PosixFilePermission.OWNER_EXECUTE,
)

private val posixFilePermissions = EnumSet.of(
    PosixFilePermission.OWNER_READ,
    PosixFilePermission.OWNER_WRITE,
)

private fun sealPosixDirectory(directory: File) {
    runCatching {
        Files.setPosixFilePermissions(directory.toPath(), posixDirectoryPermissions)
    }
}

private fun sealPosixFile(file: File) {
    runCatching {
        Files.setPosixFilePermissions(file.toPath(), posixFilePermissions)
    }
}
