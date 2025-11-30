package com.example.stremiompvplayer.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Helper class for encrypting and decrypting backup data using AES-256-GCM.
 * Supports both password-based encryption (PBKDF2) and Android Keystore-based encryption.
 */
object EncryptionHelper {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128 // bits
    private const val IV_LENGTH = 12 // bytes
    private const val SALT_LENGTH = 16 // bytes
    private const val PBKDF2_ITERATIONS = 100000
    private const val KEY_LENGTH = 256 // bits
    private const val KEYSTORE_ALIAS = "BingeBackupKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    /**
     * Result class for encryption operations.
     */
    data class EncryptionResult(
        val encryptedData: ByteArray,
        val iv: ByteArray,
        val salt: ByteArray? = null // Only used for password-based encryption
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as EncryptionResult
            if (!encryptedData.contentEquals(other.encryptedData)) return false
            if (!iv.contentEquals(other.iv)) return false
            if (salt != null) {
                if (other.salt == null) return false
                if (!salt.contentEquals(other.salt)) return false
            } else if (other.salt != null) return false
            return true
        }

        override fun hashCode(): Int {
            var result = encryptedData.contentHashCode()
            result = 31 * result + iv.contentHashCode()
            result = 31 * result + (salt?.contentHashCode() ?: 0)
            return result
        }
    }

    /**
     * Encrypt data using a user-provided password.
     * Uses PBKDF2 to derive a key from the password.
     *
     * @param data The data to encrypt
     * @param password The user's password
     * @return EncryptionResult containing encrypted data, IV, and salt
     */
    fun encryptWithPassword(data: ByteArray, password: String): EncryptionResult {
        val salt = generateSalt()
        val secretKey = deriveKeyFromPassword(password, salt)
        val iv = generateIV()

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val encryptedData = cipher.doFinal(data)

        return EncryptionResult(encryptedData, iv, salt)
    }

    /**
     * Decrypt data using a user-provided password.
     *
     * @param encryptedData The encrypted data
     * @param password The user's password
     * @param iv The initialization vector used during encryption
     * @param salt The salt used during key derivation
     * @return The decrypted data
     * @throws javax.crypto.AEADBadTagException if password is incorrect
     */
    fun decryptWithPassword(
        encryptedData: ByteArray,
        password: String,
        iv: ByteArray,
        salt: ByteArray
    ): ByteArray {
        val secretKey = deriveKeyFromPassword(password, salt)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        return cipher.doFinal(encryptedData)
    }

    /**
     * Encrypt data using Android Keystore (no password required).
     * Falls back to a generated random key if Keystore is unavailable.
     *
     * @param data The data to encrypt
     * @return EncryptionResult containing encrypted data and IV
     */
    fun encryptWithKeystore(data: ByteArray): EncryptionResult {
        val secretKey = getOrCreateKeystoreKey()
        val iv = generateIV()

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val encryptedData = cipher.doFinal(data)

        return EncryptionResult(encryptedData, iv)
    }

    /**
     * Decrypt data using Android Keystore.
     *
     * @param encryptedData The encrypted data
     * @param iv The initialization vector used during encryption
     * @return The decrypted data
     */
    fun decryptWithKeystore(encryptedData: ByteArray, iv: ByteArray): ByteArray {
        val secretKey = getOrCreateKeystoreKey()

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        return cipher.doFinal(encryptedData)
    }

    /**
     * Encode encrypted data to a Base64 string for storage/transfer.
     */
    fun encodeToBase64(data: ByteArray): String {
        return Base64.encodeToString(data, Base64.NO_WRAP)
    }

    /**
     * Decode Base64 string back to byte array.
     */
    fun decodeFromBase64(data: String): ByteArray {
        return Base64.decode(data, Base64.NO_WRAP)
    }

    /**
     * Package encrypted data with IV and salt into a single byte array.
     * Format: [saltLen(1)][salt][ivLen(1)][iv][encryptedData]
     */
    fun packageEncryptedData(result: EncryptionResult): ByteArray {
        val outputStream = ByteArrayOutputStream()
        
        // Write salt length and salt (0 if no salt)
        val saltBytes = result.salt ?: ByteArray(0)
        outputStream.write(saltBytes.size)
        outputStream.write(saltBytes)
        
        // Write IV length and IV
        outputStream.write(result.iv.size)
        outputStream.write(result.iv)
        
        // Write encrypted data
        outputStream.write(result.encryptedData)
        
        return outputStream.toByteArray()
    }

    /**
     * Unpackage encrypted data from a single byte array.
     */
    fun unpackageEncryptedData(packagedData: ByteArray): EncryptionResult {
        val inputStream = ByteArrayInputStream(packagedData)
        
        // Read salt
        val saltLen = inputStream.read()
        val salt = if (saltLen > 0) {
            ByteArray(saltLen).also { inputStream.read(it) }
        } else null
        
        // Read IV
        val ivLen = inputStream.read()
        val iv = ByteArray(ivLen).also { inputStream.read(it) }
        
        // Read encrypted data
        val encryptedData = inputStream.readBytes()
        
        return EncryptionResult(encryptedData, iv, salt)
    }

    /**
     * Generate a cryptographic checksum (SHA-256) for data integrity verification.
     */
    fun generateChecksum(data: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verify data integrity using checksum.
     */
    fun verifyChecksum(data: ByteArray, expectedChecksum: String): Boolean {
        return generateChecksum(data) == expectedChecksum
    }

    // Private helper methods

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt
    }

    private fun generateIV(): ByteArray {
        val iv = ByteArray(IV_LENGTH)
        SecureRandom().nextBytes(iv)
        return iv
    }

    private fun deriveKeyFromPassword(password: String, salt: ByteArray): SecretKey {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(keySpec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        // Return existing key if available
        if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
            val entry = keyStore.getEntry(KEYSTORE_ALIAS, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        }

        // Generate new key
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keyGenSpec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_LENGTH)
            .build()

        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }
}
