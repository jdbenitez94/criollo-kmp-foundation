package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val GCM_IV_LENGTH = 12
private const val GCM_TAG_BITS = 128

internal actual suspend fun aesGcmEncrypt(key: ByteArray, plaintext: ByteArray, associatedData: ByteArray?): ByteArray = withContext(Dispatchers.Default) {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
    associatedData?.let { cipher.updateAAD(it) }
    val ciphertext = cipher.doFinal(plaintext)
    val iv = cipher.iv
    iv + ciphertext
}

internal actual suspend fun aesGcmDecrypt(key: ByteArray, ciphertext: ByteArray, associatedData: ByteArray?): ByteArray = withContext(Dispatchers.Default) {
    require(ciphertext.size > GCM_IV_LENGTH) { "Ciphertext too short." }
    val iv = ciphertext.copyOfRange(0, GCM_IV_LENGTH)
    val payload = ciphertext.copyOfRange(GCM_IV_LENGTH, ciphertext.size)
    try {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        associatedData?.let { cipher.updateAAD(it) }
        cipher.doFinal(payload)
    } finally {
        iv.fill(0)
        payload.fill(0)
    }
}

internal fun randomAes256Key(): ByteArray = ByteArray(32).also { SecureRandom().nextBytes(it) }

internal actual fun randomPlatformAesKey(): ByteArray = randomAes256Key()
