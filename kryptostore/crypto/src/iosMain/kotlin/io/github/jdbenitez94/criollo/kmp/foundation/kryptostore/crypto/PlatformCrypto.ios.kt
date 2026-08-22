@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecItemNotFound
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.memcpy

private class IosSecureKeyStore(appId: String) : SecureKeyStore {
    private val service = "$appId.crypto"

    override suspend fun readKey(): ByteArray {
        val existing = readKeychainData(LEGACY_KEY_ACCOUNT)
        return if (existing != null && existing.length > 0u) {
            existing.toByteArray()
        } else {
            randomPlatformAesKey().also { writeKey(it) }
        }
    }

    override suspend fun writeKey(key: ByteArray) {
        writeKeychainData(LEGACY_KEY_ACCOUNT, key.toNSData())
        addKeyIdToIndex(VersionedAesGcmCipher.LEGACY_KEY_ID)
    }

    override suspend fun readActiveKeyId(): String {
        val stored = readKeychainData(ACTIVE_KEY_ID_ACCOUNT)?.toByteArray()?.decodeToString()
        if (!stored.isNullOrBlank()) return stored
        if (readKeychainData(LEGACY_KEY_ACCOUNT) != null) {
            return VersionedAesGcmCipher.LEGACY_KEY_ID
        }
        val initialKeyId = VersionedAesGcmCipher.LEGACY_KEY_ID
        writeKey(randomPlatformAesKey())
        setActiveKeyId(initialKeyId)
        return initialKeyId
    }

    override suspend fun setActiveKeyId(keyId: String) {
        writeKeychainData(ACTIVE_KEY_ID_ACCOUNT, keyId.encodeToByteArray().toNSData())
        addKeyIdToIndex(keyId)
    }

    override suspend fun readKey(keyId: String): ByteArray {
        val account = accountForKeyId(keyId)
        val existing = readKeychainData(account)
        return if (existing != null && existing.length > 0u) {
            existing.toByteArray()
        } else {
            randomPlatformAesKey().also { writeKey(keyId, it) }
        }
    }

    override suspend fun writeKey(keyId: String, key: ByteArray) {
        writeKeychainData(accountForKeyId(keyId), key.toNSData())
        addKeyIdToIndex(keyId)
    }

    override suspend fun listKeyIds(): List<String> = readKeyringIndex()

    override suspend fun deleteKey(keyId: String) {
        deleteKeychainItem(accountForKeyId(keyId))
        removeKeyIdFromIndex(keyId)
    }

    override suspend fun readLastRotationMillis(): Long = readKeychainData(ROTATION_ACCOUNT)?.toByteArray()?.decodeToString()?.toLongOrNull() ?: 0L

    override suspend fun writeLastRotationMillis(value: Long) {
        writeKeychainData(ROTATION_ACCOUNT, value.toString().encodeToByteArray().toNSData())
    }

    private fun accountForKeyId(keyId: String): String = when (keyId) {
        VersionedAesGcmCipher.LEGACY_KEY_ID -> LEGACY_KEY_ACCOUNT
        else -> "$KEY_ACCOUNT_PREFIX.$keyId"
    }

    private suspend fun readKeyringIndex(): List<String> {
        val raw = readKeychainData(KEYRING_INDEX_ACCOUNT)?.toByteArray()?.decodeToString()
        if (raw.isNullOrBlank()) {
            return buildList {
                if (readKeychainData(LEGACY_KEY_ACCOUNT) != null) add(VersionedAesGcmCipher.LEGACY_KEY_ID)
            }
        }
        return raw.split(KEYRING_SEPARATOR).filter { it.isNotBlank() }.distinct()
    }

    private suspend fun addKeyIdToIndex(keyId: String) {
        val updated = (readKeyringIndex() + keyId).distinct()
        writeKeychainData(KEYRING_INDEX_ACCOUNT, updated.joinToString(KEYRING_SEPARATOR).encodeToByteArray().toNSData())
    }

    private suspend fun removeKeyIdFromIndex(keyId: String) {
        val updated = readKeyringIndex().filterNot { it == keyId }
        if (updated.isEmpty()) {
            deleteKeychainItem(KEYRING_INDEX_ACCOUNT)
        } else {
            writeKeychainData(KEYRING_INDEX_ACCOUNT, updated.joinToString(KEYRING_SEPARATOR).encodeToByteArray().toNSData())
        }
    }

    private fun readKeychainData(account: String): NSData? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val query = keychainQuery(account, returnData = true, includeValue = null, forAdd = false)
        val status = SecItemCopyMatching(query, result.ptr)
        if (query != null) CFRelease(query)
        if (status == errSecItemNotFound) return@memScoped null
        check(status == noErr) { "iOS Keychain read failed with OSStatus=$status" }
        CFBridgingRelease(result.value) as? NSData
    }

    private fun writeKeychainData(account: String, data: NSData) {
        val updateQuery = keychainQuery(account, returnData = false, includeValue = null, forAdd = false)
        val values = valueDictionary(data)
        val updateStatus = SecItemUpdate(updateQuery, values)
        if (updateQuery != null) CFRelease(updateQuery)
        if (values != null) CFRelease(values)

        if (updateStatus == errSecItemNotFound) {
            val addQuery = keychainQuery(account, returnData = false, includeValue = data, forAdd = true)
            val addStatus = SecItemAdd(addQuery, null)
            if (addQuery != null) CFRelease(addQuery)
            check(addStatus == noErr) { "iOS Keychain add failed with OSStatus=$addStatus" }
        } else {
            check(updateStatus == noErr) { "iOS Keychain update failed with OSStatus=$updateStatus" }
        }
    }

    private fun deleteKeychainItem(account: String) {
        val query = keychainQuery(account, returnData = false, includeValue = null, forAdd = false)
        val status = SecItemDelete(query)
        if (query != null) CFRelease(query)
        if (status != noErr && status != errSecItemNotFound) {
            check(false) { "iOS Keychain delete failed with OSStatus=$status" }
        }
    }

    private fun keychainQuery(account: String, returnData: Boolean, includeValue: NSData?, forAdd: Boolean): CFDictionaryRef? {
        val capacity = BASE_KEYCHAIN_ENTRIES +
            (if (returnData) 1 else 0) +
            (if (includeValue != null) 1 else 0) +
            (if (forAdd) 1 else 0)
        val query = CFDictionaryCreateMutable(
            kCFAllocatorDefault,
            capacity.convert(),
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        ) ?: return null

        val serviceRef = service.toKeychainCfString()
        val accountRef = account.toKeychainCfString()
        if (serviceRef == null || accountRef == null) {
            serviceRef?.let { CFRelease(it) }
            CFRelease(query)
            return null
        }

        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, serviceRef)
        CFDictionaryAddValue(query, kSecAttrAccount, accountRef)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)
        if (returnData) {
            CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        }
        if (includeValue != null) {
            val valueRef = CFBridgingRetain(includeValue)
            CFDictionaryAddValue(query, kSecValueData, valueRef)
            // Balance BridgingRetain; the dictionary retains the value via CFType callbacks.
            CFRelease(valueRef)
        }
        if (forAdd) {
            CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        }

        CFRelease(serviceRef)
        CFRelease(accountRef)
        return query
    }

    private fun valueDictionary(data: NSData): CFDictionaryRef? {
        val values = CFDictionaryCreateMutable(
            kCFAllocatorDefault,
            1.convert(),
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        ) ?: return null
        val valueRef = CFBridgingRetain(data)
        CFDictionaryAddValue(values, kSecValueData, valueRef)
        CFRelease(valueRef)
        return values
    }

    companion object {
        private const val LEGACY_KEY_ACCOUNT = "aes_key"
        private const val KEY_ACCOUNT_PREFIX = "aes_key"
        private const val ACTIVE_KEY_ID_ACCOUNT = "active_key_id"
        private const val KEYRING_INDEX_ACCOUNT = "keyring_index"
        private const val ROTATION_ACCOUNT = "last_rotation"
        private const val KEYRING_SEPARATOR = ","
        private const val BASE_KEYCHAIN_ENTRIES = 4
        private const val noErr = 0
    }
}

private fun String.toKeychainCfString(): CFStringRef? = CFStringCreateWithCString(kCFAllocatorDefault, this, kCFStringEncodingUTF8)

actual fun createPlatformCryptoStack(appId: String, rotationConfig: KeyRotationConfig): PlatformCryptoStack = createAesPlatformStack(IosSecureKeyStore(appId), rotationConfig)

internal actual fun randomPlatformAesKey(): ByteArray = ByteArray(32).also { bytes ->
    memScoped {
        SecRandomCopyBytes(null, 32u, bytes.usePinned { it.addressOf(0) })
    }
}

private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length == 0) return ByteArray(0)
    val rawBytes = this.bytes ?: return ByteArray(0)
    return ByteArray(length).also { buffer ->
        buffer.usePinned { pinned ->
            memcpy(pinned.addressOf(0), rawBytes, length.convert())
        }
    }
}

private fun ByteArray.toNSData(): NSData = usePinned {
    NSData.create(bytes = it.addressOf(0), length = size.toULong())
}

private suspend fun decodeAesKey(key: ByteArray): AES.GCM.Key = CryptographyProvider.Default.get(AES.GCM).keyDecoder()
    .decodeFromByteArray(AES.Key.Format.RAW, key)

internal actual suspend fun aesGcmEncrypt(key: ByteArray, plaintext: ByteArray, associatedData: ByteArray?): ByteArray {
    val cipher = decodeAesKey(key).cipher()
    return cipher.encrypt(
        plaintext = plaintext,
        associatedData = associatedData ?: ByteArray(0),
    )
}

internal actual suspend fun aesGcmDecrypt(key: ByteArray, ciphertext: ByteArray, associatedData: ByteArray?): ByteArray {
    val cipher = decodeAesKey(key).cipher()
    return cipher.decrypt(
        ciphertext = ciphertext,
        associatedData = associatedData ?: ByteArray(0),
    )
}
