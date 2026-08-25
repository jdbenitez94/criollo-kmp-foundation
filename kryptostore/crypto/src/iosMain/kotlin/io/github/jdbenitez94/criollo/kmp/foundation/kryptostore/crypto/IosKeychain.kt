@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

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

/**
 * Thin Keychain accessors used by [IosSecureKeyStore]. Kept separate so Codacy/file complexity
 * for platform crypto stays focused on key-lifecycle logic.
 */
internal class IosKeychain(private val service: String) {
    fun read(account: String): NSData? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val query = query(account, returnData = true, includeValue = null, forAdd = false)
        val status = SecItemCopyMatching(query, result.ptr)
        if (query != null) CFRelease(query)
        if (status == errSecItemNotFound) return@memScoped null
        check(status == NO_ERR) { "iOS Keychain read failed with OSStatus=$status" }
        CFBridgingRelease(result.value) as? NSData
    }

    fun write(account: String, data: NSData) {
        val updateQuery = query(account, returnData = false, includeValue = null, forAdd = false)
        val values = valueDictionary(data)
        val updateStatus = SecItemUpdate(updateQuery, values)
        if (updateQuery != null) CFRelease(updateQuery)
        if (values != null) CFRelease(values)

        if (updateStatus == errSecItemNotFound) {
            val addQuery = query(account, returnData = false, includeValue = data, forAdd = true)
            val addStatus = SecItemAdd(addQuery, null)
            if (addQuery != null) CFRelease(addQuery)
            check(addStatus == NO_ERR) { "iOS Keychain add failed with OSStatus=$addStatus" }
        } else {
            check(updateStatus == NO_ERR) { "iOS Keychain update failed with OSStatus=$updateStatus" }
        }
    }

    fun delete(account: String) {
        val query = query(account, returnData = false, includeValue = null, forAdd = false)
        val status = SecItemDelete(query)
        if (query != null) CFRelease(query)
        if (status != NO_ERR && status != errSecItemNotFound) {
            check(false) { "iOS Keychain delete failed with OSStatus=$status" }
        }
    }

    private fun query(account: String, returnData: Boolean, includeValue: NSData?, forAdd: Boolean): CFDictionaryRef? {
        val capacity = BASE_ENTRIES +
            (if (returnData) 1 else 0) +
            (if (includeValue != null) 1 else 0) +
            (if (forAdd) 1 else 0)
        val dictionary = CFDictionaryCreateMutable(
            kCFAllocatorDefault,
            capacity.convert(),
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        ) ?: return null

        val serviceRef = service.toKeychainCfString()
        val accountRef = account.toKeychainCfString()
        if (serviceRef == null || accountRef == null) {
            serviceRef?.let { CFRelease(it) }
            CFRelease(dictionary)
            return null
        }

        CFDictionaryAddValue(dictionary, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(dictionary, kSecAttrService, serviceRef)
        CFDictionaryAddValue(dictionary, kSecAttrAccount, accountRef)
        CFDictionaryAddValue(dictionary, kSecMatchLimit, kSecMatchLimitOne)
        if (returnData) {
            CFDictionaryAddValue(dictionary, kSecReturnData, kCFBooleanTrue)
        }
        if (includeValue != null) {
            val valueRef = CFBridgingRetain(includeValue)
            CFDictionaryAddValue(dictionary, kSecValueData, valueRef)
            CFRelease(valueRef)
        }
        if (forAdd) {
            CFDictionaryAddValue(dictionary, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        }

        CFRelease(serviceRef)
        CFRelease(accountRef)
        return dictionary
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

    private companion object {
        const val BASE_ENTRIES = 4
        const val NO_ERR = 0
    }
}

internal fun String.toKeychainCfString(): CFStringRef? =
    CFStringCreateWithCString(kCFAllocatorDefault, this, kCFStringEncodingUTF8)

internal fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length == 0) return ByteArray(0)
    val rawBytes = this.bytes ?: return ByteArray(0)
    return ByteArray(length).also { buffer ->
        buffer.usePinned { pinned ->
            memcpy(pinned.addressOf(0), rawBytes, length.convert())
        }
    }
}

internal fun ByteArray.toNSData(): NSData = usePinned {
    NSData.create(bytes = it.addressOf(0), length = size.toULong())
}
