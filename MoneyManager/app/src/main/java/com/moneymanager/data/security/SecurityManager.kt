package com.moneymanager.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor() {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "MoneyManagerPinKey"
        private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
        private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"
        private const val IV_SIZE = 12
        private const val TAG_SIZE = 128
        private const val SALT_SIZE = 16
        private const val MAX_ATTEMPTS = 5
    }

    private var wrongAttempts = 0

    fun hashPin(pin: String, salt: ByteArray? = null): Pair<String, String> {
        val actualSalt = salt ?: ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val combined = pin.toByteArray() + actualSalt
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(combined)
        return Pair(Base64.getEncoder().encodeToString(hash), Base64.getEncoder().encodeToString(actualSalt))
    }

    fun verifyPin(pin: String, storedHash: String, storedSalt: String): Boolean {
        val salt = Base64.getDecoder().decode(storedSalt)
        val (computedHash, _) = hashPin(pin, salt)
        return computedHash == storedHash
    }

    fun isLocked(): Boolean = wrongAttempts >= MAX_ATTEMPTS

    fun recordWrongAttempt(): Int {
        wrongAttempts++
        return MAX_ATTEMPTS - wrongAttempts
    }

    fun resetWrongAttempts() {
        wrongAttempts = 0
    }

    fun getRemainingAttempts(): Int = MAX_ATTEMPTS - wrongAttempts

    fun generateSalt(): String {
        val salt = ByteArray(SALT_SIZE)
        SecureRandom().nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    fun encryptData(data: ByteArray): ByteArray {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateKey()
        }
        
        val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)
        
        return iv + encrypted
    }

    fun decryptData(encryptedData: ByteArray): ByteArray {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        
        val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey
        val iv = encryptedData.copyOfRange(0, IV_SIZE)
        val data = encryptedData.copyOfRange(IV_SIZE, encryptedData.size)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        
        return cipher.doFinal(data)
    }

    private fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM, ANDROID_KEYSTORE)
        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .setUserAuthenticationRequired(false)
            .build()
        
        keyGenerator.init(keySpec)
        keyGenerator.generateKey()
    }
}
