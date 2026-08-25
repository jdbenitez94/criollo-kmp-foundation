package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

/**
 * Versioned key-id envelope shared by AES (JVM/iOS) and WebCrypto workers:
 * `[keyIdLength:1][keyId UTF-8][payload…]`.
 *
 * Decrypt fails closed when the header is missing or malformed (no legacy plaintext path).
 */
internal object KeyIdEnvelope {
    const val MAX_KEY_ID_LENGTH = 64

    fun encode(keyId: String): ByteArray {
        val keyIdBytes = keyId.encodeToByteArray()
        require(keyIdBytes.isNotEmpty() && keyIdBytes.size <= MAX_KEY_ID_LENGTH) {
            "Key id length out of range."
        }
        return byteArrayOf(keyIdBytes.size.toByte()) + keyIdBytes
    }

    fun decode(message: ByteArray): Pair<String, ByteArray> {
        require(message.isNotEmpty()) { "Ciphertext missing key-id header." }
        val keyIdLength = message.first().toInt() and 0xFF
        require(keyIdLength in 1..MAX_KEY_ID_LENGTH && message.size >= 1 + keyIdLength) {
            "Invalid key-id header."
        }
        val keyId = message.copyOfRange(1, 1 + keyIdLength).decodeToString()
        val payload = message.copyOfRange(1 + keyIdLength, message.size)
        return keyId to payload
    }
}
