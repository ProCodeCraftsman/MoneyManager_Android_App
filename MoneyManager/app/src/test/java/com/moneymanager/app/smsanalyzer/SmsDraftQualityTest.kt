package com.moneymanager.app.smsanalyzer

import com.moneymanager.domain.ai.AccountEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsDraftQualityTest {

    private val accounts = listOf(
        AccountEntry(1, "HDFC Bank Savings 1234", "bank"),
        AccountEntry(2, "Axis Credit Card", "credit"),
    )
    private val upiSent = "Sent Rs.250.00 From HDFC Bank A/C *1234 To ZOMATO On 02/10/26 Ref 123456789012 Not You? Call 18002586161"

    private fun analyze(ctx: DraftContext?, vararg bodies: String): AnalysisReport {
        val a = SmsAnalyzer(draftContext = ctx)
        bodies.forEach { a.add("AD-HDFCBK-S", it, 1_700_000_000_000) }
        return a.report()
    }

    @Test
    fun `ready when account matched and merchant category known`() {
        val ctx = DraftContext(accounts) { m, _ -> if (m.equals("ZOMATO", true)) MemoryHit("expense") else null }
        val q = analyze(ctx, upiSent).draftQuality
        assertNotNull(q)
        assertEquals(1, q!!.drafts)
        assertEquals(1, q.ready)
        assertEquals(1, q.accountMatched)
        assertEquals(1, q.categoryFilled)
    }

    @Test
    fun `not ready without category memory or with a mismatched memory type`() {
        val none = analyze(DraftContext(accounts) { _, _ -> null }, upiSent).draftQuality!!
        assertEquals(0, none.ready)
        assertEquals(1, none.accountMatched)
        assertEquals(0, none.categoryFilled)
        val wrongType = analyze(DraftContext(accounts) { _, _ -> MemoryHit("income") }, upiSent).draftQuality!!
        assertEquals(0, wrongType.ready)
    }

    @Test
    fun `no accounts means hinted but unmatched`() {
        val q = analyze(DraftContext(emptyList()) { _, _ -> MemoryHit(null) }, upiSent).draftQuality!!
        assertEquals(0, q.accountMatched)
        assertEquals(1, q.accountHintedButUnmatched)
        assertEquals(0, q.ready)
    }

    @Test
    fun `without a context nothing is measured and json has no draft summary`() {
        val r = analyze(null, upiSent)
        assertNull(r.draftQuality)
        assertTrue(!r.toJson().contains("draftSummary"))
    }

    @Test
    fun `json carries counts only`() {
        val json = analyze(DraftContext(accounts) { _, _ -> MemoryHit(null) }, upiSent).toJson()
        assertTrue(json.contains("draftSummary"))
        assertTrue(!json.contains("HDFC Bank Savings"))
    }
}
