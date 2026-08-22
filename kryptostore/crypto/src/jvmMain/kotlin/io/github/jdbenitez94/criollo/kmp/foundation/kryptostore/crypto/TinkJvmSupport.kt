package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import com.google.crypto.tink.Aead
import com.google.crypto.tink.subtle.AesGcmJce
import com.sun.jna.FunctionMapper
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import java.io.File
import java.lang.reflect.Method
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.security.SecureRandom
import java.util.EnumSet

internal class JvmMasterKeyAead(private val appId: String) : Aead {
    private val delegate: Aead by lazy {
        val store = JvmSecureMasterKeyStore.current(appId)
        val keyBytes = store.readOrCreate("$appId.master")
        AesGcmJce(keyBytes)
    }

    override fun encrypt(plaintext: ByteArray, associatedData: ByteArray?): ByteArray = delegate.encrypt(plaintext, associatedData)

    override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray?): ByteArray = delegate.decrypt(ciphertext, associatedData)
}

private interface JvmSecureMasterKeyStore {
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

private object MacOsKeychainMasterKeyStore : JvmSecureMasterKeyStore {
    private const val NO_ERR = 0
    private const val ERR_SEC_ITEM_NOT_FOUND = -25300
    private const val ERR_SEC_DUPLICATE_ITEM = -25299
    private const val SERVICE = "io.github.jdbenitez94.criollo.kmp.foundation.kryptostore"
    private val security = Native.load(
        "Security",
        SecurityLibrary::class.java,
        mapOf(Library.OPTION_FUNCTION_MAPPER to SecurityFunctionMapper),
    )

    override fun readOrCreate(account: String): ByteArray {
        read(account)?.let { return it.copyOf(32) }
        val generated = ByteArray(32).also { SecureRandom().nextBytes(it) }
        write(account, generated)
        return generated
    }

    private fun read(account: String): ByteArray? {
        val passwordLength = IntByReference()
        val passwordData = PointerByReference()
        val itemRef = PointerByReference()
        val status = find(account, passwordLength, passwordData, itemRef)
        if (status == ERR_SEC_ITEM_NOT_FOUND) return null
        checkStatus(status, "read")
        return try {
            passwordData.value.getByteArray(0, passwordLength.value)
        } finally {
            security.secKeychainItemFreeContent(null, passwordData.value)
            itemRef.value?.let(security::cfRelease)
        }
    }

    private fun write(account: String, value: ByteArray) {
        val itemRef = PointerByReference()
        val findStatus = find(account, IntByReference(), PointerByReference(), itemRef)
        if (findStatus == NO_ERR && itemRef.value != null) {
            val updateStatus = security.secKeychainItemModifyAttributesAndData(itemRef.value, null, value.size, value)
            itemRef.value?.let(security::cfRelease)
            checkStatus(updateStatus, "update")
            return
        }
        if (findStatus != ERR_SEC_ITEM_NOT_FOUND) checkStatus(findStatus, "lookup-before-write")
        val service = SERVICE.encodeToByteArray()
        val accountBytes = account.encodeToByteArray()
        val status = security.secKeychainAddGenericPassword(
            null,
            service.size,
            service,
            accountBytes.size,
            accountBytes,
            value.size,
            value,
            null,
        )
        if (status != ERR_SEC_DUPLICATE_ITEM) checkStatus(status, "add")
    }

    private fun find(account: String, passwordLength: IntByReference, passwordData: PointerByReference, itemRef: PointerByReference): Int {
        val service = SERVICE.encodeToByteArray()
        val accountBytes = account.encodeToByteArray()
        return security.secKeychainFindGenericPassword(
            null,
            service.size,
            service,
            accountBytes.size,
            accountBytes,
            passwordLength,
            passwordData,
            itemRef,
        )
    }

    private fun checkStatus(status: Int, operation: String) {
        if (status != NO_ERR) {
            throw IllegalStateException("macOS Keychain $operation failed with OSStatus=$status")
        }
    }
}

/**
 * Reduced-security fallback for Windows/Linux: master key bytes in a owner-only file under
 * `~/.kryptostore/<appId>/`. Prefer OS credential stores when available.
 */
private class PosixSealedFileMasterKeyStore(private val appId: String) : JvmSecureMasterKeyStore {
    override fun readOrCreate(account: String): ByteArray {
        val baseDir = File(System.getProperty("user.home"), ".kryptostore/$appId/crypto").apply {
            mkdirs()
            runCatching {
                Files.setPosixFilePermissions(
                    toPath(),
                    EnumSet.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                    ),
                )
            }
        }
        val keyFile = File(baseDir, "$account.key")
        if (keyFile.exists() && keyFile.length() == 32L) {
            return keyFile.readBytes()
        }
        val generated = ByteArray(32).also { SecureRandom().nextBytes(it) }
        keyFile.writeBytes(generated)
        runCatching {
            Files.setPosixFilePermissions(
                keyFile.toPath(),
                EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                ),
            )
        }
        return generated
    }
}

private object SecurityFunctionMapper : FunctionMapper {
    private val names = mapOf(
        "secKeychainFindGenericPassword" to "SecKeychainFindGenericPassword",
        "secKeychainAddGenericPassword" to "SecKeychainAddGenericPassword",
        "secKeychainItemModifyAttributesAndData" to "SecKeychainItemModifyAttributesAndData",
        "secKeychainItemFreeContent" to "SecKeychainItemFreeContent",
        "cfRelease" to "CFRelease",
    )

    override fun getFunctionName(library: NativeLibrary, method: Method): String = names.getValue(method.name)
}

private interface SecurityLibrary : Library {
    fun secKeychainFindGenericPassword(
        keychainOrArray: Pointer?,
        serviceNameLength: Int,
        serviceName: ByteArray,
        accountNameLength: Int,
        accountName: ByteArray,
        passwordLength: IntByReference,
        passwordData: PointerByReference,
        itemRef: PointerByReference?,
    ): Int

    fun secKeychainAddGenericPassword(
        keychain: Pointer?,
        serviceNameLength: Int,
        serviceName: ByteArray,
        accountNameLength: Int,
        accountName: ByteArray,
        passwordLength: Int,
        passwordData: ByteArray,
        itemRef: PointerByReference?,
    ): Int

    fun secKeychainItemModifyAttributesAndData(itemRef: Pointer?, attrList: Pointer?, length: Int, data: ByteArray): Int

    fun secKeychainItemFreeContent(attrList: Pointer?, data: Pointer?): Int

    fun cfRelease(cf: Pointer?)
}
