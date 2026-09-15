package com.moneymanager.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EncryptionHelperTest {

    private val encryptionHelper = EncryptionHelper()

    @Test
    fun `decrypt reverses encrypt with the same passphrase`() {
        val data = "some plaintext backup content".toByteArray()
        val encrypted = encryptionHelper.encrypt(data, "correct passphrase")

        val decrypted = encryptionHelper.decrypt(encrypted, "correct passphrase")

        assertArrayEquals(data, decrypted)
    }

    @Test
    fun `decrypt fails with the wrong passphrase`() {
        val data = "some plaintext backup content".toByteArray()
        val encrypted = encryptionHelper.encrypt(data, "correct passphrase")

        assertThrows(Exception::class.java) {
            encryptionHelper.decrypt(encrypted, "wrong passphrase")
        }
    }

    @Test
    fun `encrypt output differs each call due to random salt and IV`() {
        val data = "some plaintext backup content".toByteArray()

        val first = encryptionHelper.encrypt(data, "same passphrase")
        val second = encryptionHelper.encrypt(data, "same passphrase")

        assert(!first.contentEquals(second))
    }
}
