package com.moneymanager.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor() {

    companion object {
        private const val SALT_SIZE = 16
    }

    fun hashPin(pin: String, salt: ByteArray? = null): Pair<String, String> {
        val actualSalt = salt ?: ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val hash = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray() + actualSalt)
        return Pair(Base64.getEncoder().encodeToString(hash), Base64.getEncoder().encodeToString(actualSalt))
    }

    fun verifyPin(pin: String, storedHash: String, storedSalt: String): Boolean {
        val salt = Base64.getDecoder().decode(storedSalt)
        val (computedHash, _) = hashPin(pin, salt)
        return computedHash == storedHash
    }
}
