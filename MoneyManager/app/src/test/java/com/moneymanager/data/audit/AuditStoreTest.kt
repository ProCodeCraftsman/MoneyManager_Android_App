package com.moneymanager.data.audit

import com.moneymanager.domain.audit.AuditCheck
import com.moneymanager.domain.audit.AuditFinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuditStoreTest {

    @Test
    fun `findings survive a round trip`() {
        val findings = listOf(
            AuditFinding(AuditCheck.A1_CATEGORY, "A1|swiggy|2", 0.9, txnId = 7L, suggestedCategoryId = 1L),
            AuditFinding(AuditCheck.A6_BALANCE, "A6|3|5000.0", 0.7, accountId = 3L, expected = 5000.0),
        )

        assertEquals(findings, AuditStore.decode(AuditStore.encode(findings)))
    }

    @Test
    fun `missing, blank or broken JSON reads as no findings`() {
        assertTrue(AuditStore.decode(null).isEmpty())
        assertTrue(AuditStore.decode("").isEmpty())
        assertTrue(AuditStore.decode("not json").isEmpty())
    }

    @Test
    fun `an unknown check name is dropped and the rest still load`() {
        val raw = """[{"check":"A99_FUTURE","fingerprint":"x","score":1.0},{"check":"A8_LEFTOVER","fingerprint":"A8|1","score":0.4,"txnId":1}]"""

        val decoded = AuditStore.decode(raw)

        assertEquals(listOf("A8|1"), decoded.map { it.fingerprint })
    }
}
