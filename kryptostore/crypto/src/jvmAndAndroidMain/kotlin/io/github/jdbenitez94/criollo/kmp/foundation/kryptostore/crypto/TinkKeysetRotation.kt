package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.TinkProtoKeysetFormat
import com.google.crypto.tink.aead.PredefinedAeadParameters

/** Returns true when rotation is not due yet (period still active). */
internal fun isWithinRotationPeriod(lastRotationEpochMs: Long, nowEpochMs: Long, rotationPeriodMs: Long): Boolean =
    lastRotationEpochMs != 0L && (nowEpochMs - lastRotationEpochMs) <= rotationPeriodMs

/** True when no rotation stamp exists yet (record stamp without rotating). */
internal fun isUnsetRotationStamp(lastRotationEpochMs: Long): Boolean = lastRotationEpochMs == 0L

/** Adds a new AES256-GCM primary entry to [this] keyset (previous primaries remain for decrypt). */
internal fun KeysetHandle.withRotatedAes256GcmPrimary(): KeysetHandle {
    val newEntry = KeysetHandle.generateEntryFromParameters(PredefinedAeadParameters.AES256_GCM)
        .withRandomId()
        .makePrimary()
    return KeysetHandle.newBuilder(this).addEntry(newEntry).build()
}

internal fun KeysetHandle.serializeEncryptedKeyset(masterAead: Aead, associatedData: ByteArray): ByteArray =
    TinkProtoKeysetFormat.serializeEncryptedKeyset(this, masterAead, associatedData)
