package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers

/**
 * Options for encrypted DataStore serializers (`EncryptedProtoSerializer` and `EncryptedPreferencesSerializer`).
 *
 * - Associated data (AAD) defaults to `deriveStoreAssociatedData(storeName, schemaVersion)`
 *   (`"$storeName|v$schemaVersion"`) unless [associatedData] is explicitly provided.
 * - [legacyAssociatedData] enables transparent fallback decryption when migrating AAD bindings.
 * - [allowPlaintextRead] defaults to **false** (fail-closed). Opt in only for one-way plaintext migration reads.
 */
class EncryptedStoreOptions {
    /** Logical store identifier used to derive default AEAD associated data. */
    var storeName: String = "default"

    /** Schema version number bound into default AEAD associated data. */
    var schemaVersion: Int = 1

    /** Explicit AEAD associated data override; when `null`, derived from [storeName] and [schemaVersion]. */
    var associatedData: ByteArray? = null

    /** Optional previous AEAD associated data tried as a fallback during decryption. */
    var legacyAssociatedData: ByteArray? = null

    /** Whether unencrypted payloads without `SVBLENC1` magic may be read during migration (defaults to `false`). */
    var allowPlaintextRead: Boolean = false

    /** Returns [associatedData] if set, or [deriveStoreAssociatedData] for [storeName] and [schemaVersion]. */
    fun resolvedAssociatedData(): ByteArray = associatedData ?: deriveStoreAssociatedData(storeName, schemaVersion)

    /** When [storeName] is still the class default (`"default"`), adopts [name] (from [StoreLocator]). */
    fun applyDefaultStoreName(name: String) {
        if (storeName == "default") {
            storeName = name
        }
    }
}

/** Derives canonical UTF-8 AEAD associated data (`"$storeName|v$schemaVersion"`). */
fun deriveStoreAssociatedData(storeName: String, schemaVersion: Int): ByteArray =
    "$storeName|v$schemaVersion".encodeToByteArray()

/** Builder DSL for [EncryptedStoreOptions]. */
fun EncryptedStoreOptions(block: EncryptedStoreOptions.() -> Unit): EncryptedStoreOptions =
    EncryptedStoreOptions().apply(block)
