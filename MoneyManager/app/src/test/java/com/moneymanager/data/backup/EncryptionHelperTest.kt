package com.moneymanager.data.backup

import org.junit.Assert.assertEquals
import org.junit.Test

class EncryptionHelperTest {

    private val encryptionHelper = EncryptionHelper()

    @Test
    fun `getEffectivePassphrase returns googleId-derived key when googleId is present`() {
        val googleId = "user123"
        val expected = "mm_u_user123_v1"
        val result = encryptionHelper.getEffectivePassphrase(googleId)
        assertEquals(expected, result)
    }

    @Test
    fun `getEffectivePassphrase returns default internal key when googleId is null`() {
        val expected = "moneymanager_default_v1_secure_fallback"
        val result = encryptionHelper.getEffectivePassphrase(null)
        assertEquals(expected, result)
    }

    @Test
    fun `getEffectivePassphrase returns default internal key when googleId is empty`() {
        val expected = "moneymanager_default_v1_secure_fallback"
        val result = encryptionHelper.getEffectivePassphrase("")
        assertEquals(expected, result)
    }
}
