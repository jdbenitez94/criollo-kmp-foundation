package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.migrate

import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.ENCRYPTED_BLOB_MAGIC
import kotlinx.coroutines.test.runTest
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import kotlin.test.Test

/** REQ-MIG-02 */
class LegacyAeadMigrationTest {
    @Test
    fun decryptUnenveloped_roundTripsTinkAeadBlob() = runTest {
        AeadConfig.register()
        val aead = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
            .getPrimitive(com.google.crypto.tink.Aead::class.java)
        val cipher = object : Cipher {
            override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = aead.encrypt(message, associatedData)

            override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = aead.decrypt(message, associatedData)
        }
        val plain = "migrate-me".encodeToByteArray()
        val aad = "aad".encodeToByteArray()
        val blob = cipher.encrypt(plain, aad)

        val recovered = LegacyAeadMigration.decryptUnenveloped(cipher, blob, aad)
        expectThat(recovered.decodeToString()).isEqualTo("migrate-me")
    }

    @Test
    fun decryptUnenveloped_rejectsEnvelopeMagic() = runTest {
        val cipher = object : Cipher {
            override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = message
            override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = message
        }
        val enveloped = (ENCRYPTED_BLOB_MAGIC + "x").encodeToByteArray()
        expectThrows<IllegalArgumentException> {
            LegacyAeadMigration.decryptUnenveloped(cipher, enveloped)
        }
    }
}
