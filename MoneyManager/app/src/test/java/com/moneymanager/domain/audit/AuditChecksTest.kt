package com.moneymanager.domain.audit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuditChecksTest {

    private val day = 24 * 60 * 60 * 1000L
    private val now = 1_800_000_000_000L

    private fun txn(
        id: Long,
        type: String = "expense",
        amount: Double = 100.0,
        date: Long = now,
        createdAt: Long = now,
        categoryId: Long? = 1L,
        categoryType: String? = "expense",
        accountId: Long = 1L,
        archived: Boolean = false,
        toAccountId: Long? = null,
        goalId: Long? = null,
        merchant: String? = "swiggy",
        sourceAccountId: Long? = null,
        smsDate: Long? = null,
    ) = AuditTxn(
        id, type, amount, date, createdAt, categoryId, categoryType, accountId, archived,
        toAccountId, goalId, merchant, sourceAccountId, smsDate,
    )

    private fun run(
        recent: List<AuditTxn>,
        history: List<AuditTxn> = recent,
        memory: Map<String, MemoryHint> = emptyMap(),
        balances: List<BalanceObservation> = emptyList(),
    ) = AuditChecks.run(recent, history, memory, balances, now)

    private fun List<AuditFinding>.only(check: AuditCheck) = filter { it.check == check }

    // A1

    @Test
    fun `A1 flags a merchant saved under a category it is almost always filed under`() {
        val history = (1L..4L).map { txn(it, categoryId = 1L) }
        val recent = txn(9, categoryId = 2L)

        val f = run(listOf(recent), history + recent).only(AuditCheck.A1_CATEGORY).single()

        assertEquals(1L, f.suggestedCategoryId)
        assertEquals("A1|swiggy|2", f.fingerprint)
    }

    @Test
    fun `A1 does not flag a merchant that is split between categories`() {
        val history = listOf(txn(1, categoryId = 1L), txn(2, categoryId = 1L), txn(3, categoryId = 2L), txn(4, categoryId = 2L))
        val recent = txn(9, categoryId = 3L)

        assertTrue(run(listOf(recent), history + recent).only(AuditCheck.A1_CATEGORY).isEmpty())
    }

    @Test
    fun `A1 counts a learned memory entry as evidence`() {
        val recent = txn(9, categoryId = 2L)

        val f = run(listOf(recent), memory = mapOf("swiggy" to MemoryHint(1L, 3))).only(AuditCheck.A1_CATEGORY)

        assertEquals(1, f.size)
        assertEquals(1L, f.single().suggestedCategoryId)
    }

    // A2

    @Test
    fun `A2 flags an amount ten times the merchant median`() {
        val history = (1L..5L).map { txn(it, amount = 200.0) }
        val recent = txn(9, amount = 2000.0)

        val f = run(listOf(recent), history + recent).only(AuditCheck.A2_AMOUNT).single()

        assertEquals(200.0, f.expected!!, 0.001)
    }

    @Test
    fun `A2 ignores a normal amount and needs five earlier rows`() {
        val history = (1L..5L).map { txn(it, amount = 200.0) }
        val normal = txn(9, amount = 250.0)
        assertTrue(run(listOf(normal), history + normal).only(AuditCheck.A2_AMOUNT).isEmpty())

        val thin = txn(8, merchant = "newshop", amount = 9000.0)
        assertTrue(run(listOf(thin), listOf(thin)).only(AuditCheck.A2_AMOUNT).isEmpty())
    }

    @Test
    fun `A2 falls back to the category 95th percentile for unnamed rows`() {
        val history = (1L..10L).map { txn(it, merchant = null, amount = 100.0) }
        val recent = txn(11, merchant = null, amount = 600.0)

        val f = run(listOf(recent), history + recent).only(AuditCheck.A2_AMOUNT).single()

        assertEquals(100.0, f.expected!!, 0.001)
    }

    // A3

    @Test
    fun `A3 flags an income category on an expense`() {
        val t = txn(1, categoryType = "income")
        assertEquals(AuditCheck.A3_TYPE, run(listOf(t)).single().check)
    }

    @Test
    fun `A3 flags a self transfer and a savings row with nowhere to go`() {
        assertEquals(1, run(listOf(txn(1, type = "transfer", toAccountId = 1L, categoryId = null, categoryType = null))).only(AuditCheck.A3_TYPE).size)
        assertEquals(1, run(listOf(txn(2, type = "savings", categoryType = "savings"))).only(AuditCheck.A3_TYPE).size)
        assertTrue(run(listOf(txn(3, type = "savings", categoryType = "savings", goalId = 5L))).only(AuditCheck.A3_TYPE).isEmpty())
    }

    // A4

    @Test
    fun `A4 suggests the account the SMS source is linked to`() {
        val f = run(listOf(txn(1, sourceAccountId = 2L))).only(AuditCheck.A4_ACCOUNT).single()
        assertEquals(2L, f.suggestedAccountId)
    }

    @Test
    fun `A4 flags a recent row on an archived account but not an old one`() {
        assertEquals(1, run(listOf(txn(1, archived = true))).only(AuditCheck.A4_ACCOUNT).size)

        val old = txn(2, archived = true, createdAt = now - 40 * day)
        assertTrue(run(listOf(old)).only(AuditCheck.A4_ACCOUNT).isEmpty())
    }

    // A5

    @Test
    fun `A5 pairs the later row with the earlier one`() {
        val earlier = txn(1, amount = 150.0, createdAt = now - 3600_000, date = now - 3600_000)
        val later = txn(2, amount = 150.0, createdAt = now, date = now)

        val f = run(listOf(later), listOf(earlier, later)).only(AuditCheck.A5_DUPLICATE).single()

        assertEquals(1L, f.otherTxnId)
        assertTrue(run(listOf(earlier), listOf(earlier, later)).only(AuditCheck.A5_DUPLICATE).isEmpty())
    }

    @Test
    fun `A5 leaves a habitual same-amount purchase alone`() {
        val habit = (1L..3L).map { txn(it, amount = 20.0, date = now - it * day, createdAt = now - it * day) }
        val earlier = txn(4, amount = 20.0, date = now - 3600_000, createdAt = now - 3600_000)
        val later = txn(5, amount = 20.0, date = now, createdAt = now)

        assertTrue(run(listOf(later), habit + earlier + later).only(AuditCheck.A5_DUPLICATE).isEmpty())
    }

    // A6

    @Test
    fun `A6 flags an SMS balance that differs from the app when the SMS is the newest`() {
        val row = txn(1, date = now - 2 * day)
        val obs = BalanceObservation(accountId = 1L, smsBalance = 5000.0, observedAt = now, appBalance = 4200.0)

        val f = run(listOf(row), listOf(row), balances = listOf(obs)).only(AuditCheck.A6_BALANCE).single()

        assertEquals(5000.0, f.expected!!, 0.001)
        assertEquals(1L, f.accountId)
    }

    @Test
    fun `A6 waits for a balance SMS newer than the last row, and ignores matching balances`() {
        val row = txn(1, date = now)
        val stale = BalanceObservation(1L, 5000.0, now - day, 4200.0)
        assertTrue(run(listOf(row), listOf(row), balances = listOf(stale)).only(AuditCheck.A6_BALANCE).isEmpty())

        val matching = BalanceObservation(1L, 4200.0, now, 4200.4)
        assertTrue(run(listOf(row), listOf(row), balances = listOf(matching)).only(AuditCheck.A6_BALANCE).isEmpty())
    }

    // A7

    @Test
    fun `A7 flags a future date and a date far from its SMS`() {
        assertEquals(1, run(listOf(txn(1, date = now + 2 * day))).only(AuditCheck.A7_DATE).size)
        assertEquals(1, run(listOf(txn(2, date = now, smsDate = now - 5 * day))).only(AuditCheck.A7_DATE).size)
        assertTrue(run(listOf(txn(3, date = now, smsDate = now - day))).only(AuditCheck.A7_DATE).isEmpty())
    }

    // A8

    @Test
    fun `A8 flags an uncategorised row after three days only`() {
        assertEquals(1, run(listOf(txn(1, categoryId = null, createdAt = now - 4 * day))).only(AuditCheck.A8_LEFTOVER).size)
        assertTrue(run(listOf(txn(2, categoryId = null, createdAt = now - day))).only(AuditCheck.A8_LEFTOVER).isEmpty())
    }

    // Ranker

    @Test
    fun `ranker drops dismissed fingerprints, keeps one per row and caps the count`() {
        val a = AuditFinding(AuditCheck.A8_LEFTOVER, "A8|1", 0.4, txnId = 1L)
        val aDup = AuditFinding(AuditCheck.A3_TYPE, "A3|1", 1.0, txnId = 1L)
        val b = AuditFinding(AuditCheck.A7_DATE, "A7|2", 0.8, txnId = 2L)
        val c = AuditFinding(AuditCheck.A2_AMOUNT, "A2|3", 0.9, txnId = 3L)
        val d = AuditFinding(AuditCheck.A6_BALANCE, "A6|9|10.0", 0.7, accountId = 9L)

        val shown = AuditRanker.top(listOf(a, aDup, b, c, d), dismissed = setOf("A2|3"))

        assertEquals(listOf("A3|1", "A7|2", "A6|9|10.0"), shown.map { it.fingerprint })
        assertNull(AuditRanker.top(listOf(c), dismissed = setOf("A2|3")).firstOrNull())
    }
}
