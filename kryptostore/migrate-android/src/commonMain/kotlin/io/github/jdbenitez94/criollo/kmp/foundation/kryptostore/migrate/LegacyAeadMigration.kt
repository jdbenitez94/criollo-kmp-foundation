package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.migrate

import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.ENCRYPTED_BLOB_MAGIC

/**
 * Best-effort helpers for migrating onto KryptoStore envelopes (REQ-MIG-02).
 *
 * Prefer docs/kryptostore-migration.md for osipxd / EncryptedSharedPreferences guidance
 * (REQ-MIG-03, REQ-MIG-04). Automatic decrypt of security-crypto formats is out of scope
 * without forbidden dependencies.
 */
object LegacyAeadMigration {
    /**
     * Decrypts a raw Tink AEAD (or [Cipher]-compatible) blob that has **no**
     * [ENCRYPTED_BLOB_MAGIC] envelope — e.g. older AeadSerializer-shaped payloads.
     *
     * After decrypt, re-write via EncryptedProtoSerializer / EncryptedPreferencesSerializer
     * so subsequent reads use envelope v1.
     */
    suspend fun decryptUnenveloped(cipher: Cipher, ciphertext: ByteArray, associatedData: ByteArray? = null): ByteArray {
        require(ciphertext.isNotEmpty()) { "ciphertext must not be empty" }
        require(!ciphertext.startsWithMagic()) {
            "Payload already has $ENCRYPTED_BLOB_MAGIC envelope; use Encrypted*Serializer instead"
        }
        return cipher.decrypt(ciphertext, associatedData)
    }

    private fun ByteArray.startsWithMagic(): Boolean {
        val magic = ENCRYPTED_BLOB_MAGIC.encodeToByteArray()
        if (size < magic.size) return false
        for (i in magic.indices) {
            if (this[i] != magic[i]) return false
        }
        return true
    }
}
