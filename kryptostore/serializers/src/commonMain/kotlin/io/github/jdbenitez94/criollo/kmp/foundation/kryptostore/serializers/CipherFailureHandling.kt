package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.ENCRYPTED_BLOB_MAGIC
import kotlinx.coroutines.CancellationException
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

internal const val ENCRYPTION_ENVELOPE_VERSION = 1

/**
 * Shared cancel-safe decrypt helper for encrypted DataStore serializers.
 *
 * [CancellationException] always propagates so a cancelled read cannot be
 * misreported as [CorruptionException] and trigger quarantine.
 */
internal suspend fun decryptWithAssociatedDataFallback(cipher: Cipher, ciphertext: ByteArray, associatedData: ByteArray, legacyAssociatedData: ByteArray? = null): ByteArray {
    var lastError: Throwable? = null
    for (aad in associatedDataCandidates(associatedData, legacyAssociatedData)) {
        try {
            return cipher.decrypt(ciphertext, aad)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            lastError = error
        }
    }
    throw CorruptionException("Encrypted payload could not be decrypted.", lastError)
}

internal fun ByteArray.startsWithEncryptedBlobMagic(): Boolean {
    val magic = ENCRYPTED_BLOB_MAGIC.encodeToByteArray()
    if (size < magic.size) return false
    return copyOfRange(0, magic.size).contentEquals(magic)
}

internal fun rejectPlaintextPayload(): Nothing = throw CorruptionException("Encrypted store rejected plaintext payload (missing $ENCRYPTED_BLOB_MAGIC magic).")

/**
 * Fail-closed corruption handler: quarantine the unreadable file as `*.corrupt` and rethrow.
 * Defaults are never written automatically — recovery requires an explicit user action.
 */
fun <T> failClosedCorruptionHandler(producePath: () -> Path, fileSystem: () -> FileSystem? = { kryptostoreFileSystem }): ReplaceFileCorruptionHandler<T> =
    ReplaceFileCorruptionHandler { exception ->
        quarantineCorruptFile(producePath(), fileSystem())
        throw exception
    }

internal fun quarantineCorruptFile(path: Path, fileSystem: FileSystem? = kryptostoreFileSystem) {
    if (fileSystem == null || !fileSystem.exists(path)) return
    val corruptPath = "${path}$CORRUPT_SUFFIX".toPath()
    runCatching {
        if (fileSystem.exists(corruptPath)) {
            fileSystem.delete(corruptPath, mustExist = false)
        }
        fileSystem.atomicMove(path, corruptPath)
    }
}

private fun associatedDataCandidates(associatedData: ByteArray, legacyAssociatedData: ByteArray?): List<ByteArray> = buildList {
    add(associatedData)
    legacyAssociatedData?.let { legacy ->
        if (!legacy.contentEquals(associatedData)) add(legacy)
    }
}

private const val CORRUPT_SUFFIX = ".corrupt"
