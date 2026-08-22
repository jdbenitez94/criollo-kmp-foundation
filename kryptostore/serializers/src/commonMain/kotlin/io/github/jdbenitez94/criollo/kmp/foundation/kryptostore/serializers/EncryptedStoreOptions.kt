package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers

/**
 * Options for encrypted DataStore serializers.
 *
 * [allowPlaintextRead] defaults to **false** (fail-closed). Opt in only for migration reads.
 */
class EncryptedStoreOptions {
    var storeName: String = "default"
    var schemaVersion: Int = 1
    var associatedData: ByteArray? = null
    var legacyAssociatedData: ByteArray? = null
    var allowPlaintextRead: Boolean = false

    fun resolvedAssociatedData(): ByteArray = associatedData ?: deriveStoreAssociatedData(storeName, schemaVersion)
}

fun deriveStoreAssociatedData(storeName: String, schemaVersion: Int): ByteArray = "$storeName|v$schemaVersion".encodeToByteArray()

fun EncryptedStoreOptions(block: EncryptedStoreOptions.() -> Unit): EncryptedStoreOptions = EncryptedStoreOptions().apply(block)
