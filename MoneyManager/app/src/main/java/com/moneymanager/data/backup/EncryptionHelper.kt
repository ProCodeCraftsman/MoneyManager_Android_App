package com.moneymanager.data.backup

import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * AES-256-GCM encryption with PBKDF2 key derivation.
 *
 * Output format: [salt(16 bytes)] + [IV(12 bytes)] + [ciphertext + GCM tag]
 *
 * Cross-device safe: the key is derived from the user-supplied passphrase + random
 * salt, never from a device-bound Android Keystore key. Any device with the same
 * passphrase can decrypt.
 */
@Singleton
class EncryptionHelper @Inject constructor() {

    fun encrypt(data: ByteArray, passphrase: String): ByteArray {
        val salt = Random.nextBytes(SALT_LENGTH)
        val iv = Random.nextBytes(IV_LENGTH)
        val key = deriveKey(passphrase, salt)

        val cipher = Cipher.getInstance(CIPHER_ALGO)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(data)

        return salt + iv + ciphertext
    }

    fun decrypt(data: ByteArray, passphrase: String): ByteArray {
        require(data.size > SALT_LENGTH + IV_LENGTH) { "Payload too short to be valid ciphertext" }

        val salt = data.copyOfRange(0, SALT_LENGTH)
        val iv = data.copyOfRange(SALT_LENGTH, SALT_LENGTH + IV_LENGTH)
        val ciphertext = data.copyOfRange(SALT_LENGTH + IV_LENGTH, data.size)

        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance(CIPHER_ALGO)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGO)
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    companion object {
        private const val CIPHER_ALGO = "AES/GCM/NoPadding"
        private const val KEY_DERIVATION_ALGO = "PBKDF2WithHmacSHA256"
        private const val PBKDF2_ITERATIONS = 100_000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_LENGTH = 16
        private const val IV_LENGTH = 12
        private const val GCM_TAG_BITS = 128
    }
}
