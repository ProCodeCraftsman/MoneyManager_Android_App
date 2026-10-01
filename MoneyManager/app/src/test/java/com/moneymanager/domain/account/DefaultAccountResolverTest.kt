package com.moneymanager.domain.account

import com.moneymanager.data.dao.AccountUsage
import com.moneymanager.data.entity.AccountEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultAccountResolverTest {

    private val bankA = AccountEntity(id = 1, name = "A Bank", type = "bank")
    private val bankB = AccountEntity(id = 2, name = "B Bank", type = "bank")
    private val cash = AccountEntity(id = 3, name = "Cash", type = "cash")
    private val invest = AccountEntity(id = 4, name = "Zerodha", type = "savings")
    private val peer = AccountEntity(id = 5, name = "Ravi", type = "peer")
    private val all = listOf(bankA, bankB, cash, invest, peer)

    private fun resolve(
        slot: DefaultAccountSlot,
        accounts: List<AccountEntity> = all,
        configured: Map<String, Long> = emptyMap(),
        usage: List<AccountUsage> = emptyList(),
        exclude: Long? = null
    ) = DefaultAccountResolver.resolve(slot, accounts, configured, usage, exclude)

    @Test
    fun `configured default wins over usage`() {
        val usage = listOf(AccountUsage("expense", 1, null, 50))
        assertEquals(3L, resolve(DefaultAccountSlot.EXPENSE, configured = mapOf("expense" to 3L), usage = usage))
    }

    @Test
    fun `configured default pointing at archived account falls back to most used`() {
        val accounts = listOf(bankA, bankB.copy(isArchived = true), cash)
        val usage = listOf(AccountUsage("income", 3, null, 4), AccountUsage("income", 1, null, 2))
        assertEquals(3L, resolve(DefaultAccountSlot.INCOME, accounts, mapOf("income" to 2L), usage))
    }

    @Test
    fun `configured default missing from accounts falls back`() {
        assertEquals(1L, resolve(DefaultAccountSlot.INCOME, configured = mapOf("income" to 99L)))
    }

    @Test
    fun `most used is counted per transaction type`() {
        val usage = listOf(
            AccountUsage("expense", 1, null, 2),
            AccountUsage("expense", 3, null, 5),
            AccountUsage("income", 1, null, 9)
        )
        assertEquals(3L, resolve(DefaultAccountSlot.EXPENSE, usage = usage))
        assertEquals(1L, resolve(DefaultAccountSlot.INCOME, usage = usage))
    }

    @Test
    fun `usage rows for the same account are summed`() {
        val usage = listOf(
            AccountUsage("transfer", 1, 2, 2),
            AccountUsage("transfer", 1, 3, 2),
            AccountUsage("transfer", 3, 1, 3)
        )
        assertEquals(1L, resolve(DefaultAccountSlot.TRANSFER_FROM, usage = usage))
    }

    @Test
    fun `destination slots count toAccountId`() {
        val usage = listOf(
            AccountUsage("transfer", 1, 3, 4),
            AccountUsage("transfer", 3, 2, 1)
        )
        assertEquals(3L, resolve(DefaultAccountSlot.TRANSFER_TO, usage = usage))
    }

    @Test
    fun `no history falls back to first eligible account`() {
        assertEquals(1L, resolve(DefaultAccountSlot.EXPENSE))
    }

    @Test
    fun `tie keeps list order`() {
        val usage = listOf(AccountUsage("income", 2, null, 3), AccountUsage("income", 1, null, 3))
        assertEquals(1L, resolve(DefaultAccountSlot.INCOME, usage = usage))
    }

    @Test
    fun `peer accounts only eligible for lend and borrow`() {
        val peerOnly = listOf(peer)
        assertNull(resolve(DefaultAccountSlot.EXPENSE, peerOnly))
        assertNull(resolve(DefaultAccountSlot.INCOME, peerOnly))
        assertNull(resolve(DefaultAccountSlot.TRANSFER_FROM, peerOnly))
        assertEquals(5L, resolve(DefaultAccountSlot.LEND, peerOnly))
        assertEquals(5L, resolve(DefaultAccountSlot.BORROW, peerOnly))
    }

    @Test
    fun `savings destination is investment accounts only`() {
        assertEquals(4L, resolve(DefaultAccountSlot.SAVINGS_TO))
        assertNull(resolve(DefaultAccountSlot.SAVINGS_TO, listOf(bankA, cash)))
    }

    @Test
    fun `expense and savings source exclude investment accounts`() {
        val accounts = listOf(invest, bankA)
        assertEquals(1L, resolve(DefaultAccountSlot.EXPENSE, accounts))
        assertEquals(1L, resolve(DefaultAccountSlot.SAVINGS_FROM, accounts))
        assertNull(resolve(DefaultAccountSlot.EXPENSE, listOf(invest)))
    }

    @Test
    fun `excluded account is skipped even when configured or most used`() {
        val usage = listOf(AccountUsage("transfer", 1, 2, 9))
        assertEquals(2L, resolve(DefaultAccountSlot.TRANSFER_TO, configured = mapOf("transfer_to" to 1L), usage = usage, exclude = 1L))
        assertEquals(2L, resolve(DefaultAccountSlot.TRANSFER_FROM, usage = usage, exclude = 1L))
    }

    @Test
    fun `usage is filtered by transaction type`() {
        val usage = listOf(AccountUsage("lend", 3, null, 1), AccountUsage("borrow", 1, null, 7))
        assertEquals(3L, resolve(DefaultAccountSlot.LEND, usage = usage))
    }

    @Test
    fun `slot lookup by transaction type and side`() {
        assertEquals(DefaultAccountSlot.EXPENSE, DefaultAccountSlot.of("expense", false))
        assertNull(DefaultAccountSlot.of("expense", true))
        assertEquals(DefaultAccountSlot.TRANSFER_FROM, DefaultAccountSlot.of("transfer", false))
        assertEquals(DefaultAccountSlot.TRANSFER_TO, DefaultAccountSlot.of("transfer", true))
        assertEquals(DefaultAccountSlot.SAVINGS_TO, DefaultAccountSlot.of("savings", true))
        assertNull(DefaultAccountSlot.of("lend", true))
        assertNull(DefaultAccountSlot.of("unknown", false))
    }

    @Test
    fun `slot keys round trip`() {
        DefaultAccountSlot.entries.forEach { assertEquals(it, DefaultAccountSlot.fromKey(it.key)) }
        assertNull(DefaultAccountSlot.fromKey("nope"))
    }
}
