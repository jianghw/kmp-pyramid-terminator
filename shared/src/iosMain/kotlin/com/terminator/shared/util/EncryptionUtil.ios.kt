package com.terminator.shared.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault

@OptIn(ExperimentalForeignApi::class)
actual object EncryptionUtil {
    private const val GCM_IV_LENGTH = 12
    private const val SALT_LENGTH = 16
    private const val KEY_LENGTH = 32

    actual fun encrypt(plainText: String, key: ByteArray): ByteArray {
        val iv = generateIv()
        val plainBytes = plainText.encodeToByteArray()
        val encrypted = xorCrypt(plainBytes, key, iv)
        return iv + encrypted
    }

    actual fun decrypt(cipherData: ByteArray, key: ByteArray): String {
        val iv = cipherData.sliceArray(0 until GCM_IV_LENGTH)
        val encrypted = cipherData.sliceArray(GCM_IV_LENGTH until cipherData.size)
        val decrypted = xorCrypt(encrypted, key, iv)
        return decrypted.decodeToString()
    }

    actual fun generateKey(): ByteArray {
        val key = ByteArray(KEY_LENGTH)
        key.usePinned { pinned ->
            SecRandomCopyBytes(kSecRandomDefault, KEY_LENGTH.toULong(), pinned.addressOf(0))
        }
        return key
    }

    actual fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        salt.usePinned { pinned ->
            SecRandomCopyBytes(kSecRandomDefault, SALT_LENGTH.toULong(), pinned.addressOf(0))
        }
        return salt
    }

    actual fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val combined = password.encodeToByteArray() + salt
        val key = ByteArray(KEY_LENGTH)
        for (i in key.indices) {
            val a = combined.getOrElse(i) { 0 }.toInt() and 0xFF
            val b = salt.getOrElse(i % salt.size) { 0 }.toInt() and 0xFF
            key[i] = (a xor b).toByte()
        }
        return key
    }

    private fun generateIv(): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH)
        iv.usePinned { pinned ->
            SecRandomCopyBytes(kSecRandomDefault, GCM_IV_LENGTH.toULong(), pinned.addressOf(0))
        }
        return iv
    }

    private fun xorCrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val result = ByteArray(data.size)
        for (i in data.indices) {
            val keyByte = key[i % key.size].toInt() and 0xFF
            val ivByte = iv[i % iv.size].toInt() and 0xFF
            val dataByte = data[i].toInt() and 0xFF
            result[i] = (dataByte xor keyByte xor ivByte).toByte()
        }
        return result
    }
}
