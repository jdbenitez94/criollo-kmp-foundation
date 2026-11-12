package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import com.google.crypto.tink.Aead
import com.google.crypto.tink.subtle.AesGcmJce

internal class JvmMasterKeyAead(private val appId: String) : Aead {
    private val delegate: Aead by lazy {
        val store = JvmSecureMasterKeyStore.current(appId)
        val keyBytes = store.readOrCreate("$appId.master")
        AesGcmJce(keyBytes)
    }

    override fun encrypt(plaintext: ByteArray, associatedData: ByteArray?): ByteArray =
        delegate.encrypt(plaintext, associatedData)

    override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray?): ByteArray =
        delegate.decrypt(ciphertext, associatedData)
}

internal interface JvmSecureMasterKeyStore {
    fun readOrCreate(account: String): ByteArray

    companion object {
        fun current(appId: String = "default"): JvmSecureMasterKeyStore {
            val os = System.getProperty("os.name").lowercase()
            return when {
                os.contains("mac") -> MacOsKeychainMasterKeyStore

                else -> {
                    System.err.println(
                        "[kryptostore-crypto] WARNING: JVM secure master-key storage for '$os' uses a " +
                            "POSIX-sealed file under ~/.kryptostore/$appId (reduced security vs Keychain). " +
                            "Do not use this for production secrets on shared machines.",
                    )
                    PosixSealedFileMasterKeyStore(appId)
                }
            }
        }
    }
}
