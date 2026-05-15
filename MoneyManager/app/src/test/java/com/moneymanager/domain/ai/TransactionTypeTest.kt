package com.moneymanager.domain.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionTypeTest {

    @Test
    fun `enum has exactly 8 entries in correct order`() {
        val entries = TransactionType.entries
        assertEquals(8, entries.size)
        assertEquals(
            listOf(
                TransactionType.INCOME,
                TransactionType.EXPENSE,
                TransactionType.SAVINGS,
                TransactionType.TRANSFER,
                TransactionType.LEND,
                TransactionType.RECEIVE,
                TransactionType.BORROW,
                TransactionType.REPAY
            ),
            entries
        )
    }

    @Test
    fun `allIds returns all 8 id strings in declaration order`() {
        val ids = TransactionType.allIds()
        assertEquals(8, ids.size)
        assertEquals(
            listOf("income", "expense", "savings", "transfer", "lend", "receive", "borrow", "repay"),
            ids
        )
    }

    @Test
    fun `id values match VALID_TYPES exactly`() {
        val expected = listOf("income", "expense", "savings", "transfer", "lend", "receive", "borrow", "repay")
        expected.forEachIndexed { index, expectedId ->
            assertEquals(expectedId, TransactionType.entries[index].id)
        }
    }

    @Test
    fun `fromId returns correct entry for known ids`() {
        assertEquals(TransactionType.INCOME, TransactionType.fromId("income"))
        assertEquals(TransactionType.EXPENSE, TransactionType.fromId("expense"))
        assertEquals(TransactionType.SAVINGS, TransactionType.fromId("savings"))
        assertEquals(TransactionType.TRANSFER, TransactionType.fromId("transfer"))
        assertEquals(TransactionType.LEND, TransactionType.fromId("lend"))
        assertEquals(TransactionType.RECEIVE, TransactionType.fromId("receive"))
        assertEquals(TransactionType.BORROW, TransactionType.fromId("borrow"))
        assertEquals(TransactionType.REPAY, TransactionType.fromId("repay"))
    }

    @Test
    fun `fromId returns null for unknown id`() {
        assertNull(TransactionType.fromId("unknown"))
        assertNull(TransactionType.fromId(""))
        assertNull(TransactionType.fromId("INCOME"))
    }

    @Test
    fun `displayName values are set`() {
        assertEquals("Income", TransactionType.INCOME.displayName)
        assertEquals("Expense", TransactionType.EXPENSE.displayName)
        assertEquals("Savings", TransactionType.SAVINGS.displayName)
        assertEquals("Transfer", TransactionType.TRANSFER.displayName)
        assertEquals("Lend", TransactionType.LEND.displayName)
        assertEquals("Receive", TransactionType.RECEIVE.displayName)
        assertEquals("Borrow", TransactionType.BORROW.displayName)
        assertEquals("Repay", TransactionType.REPAY.displayName)
    }

    @Test
    fun `requiresCategory is true only for INCOME EXPENSE SAVINGS`() {
        assertTrue(TransactionType.INCOME.requiresCategory)
        assertTrue(TransactionType.EXPENSE.requiresCategory)
        assertTrue(TransactionType.SAVINGS.requiresCategory)

        val noCategory = listOf(
            TransactionType.TRANSFER,
            TransactionType.LEND,
            TransactionType.RECEIVE,
            TransactionType.BORROW,
            TransactionType.REPAY
        )
        noCategory.forEach { entry ->
            assertTrue("${entry.id}.requiresCategory should be false", !entry.requiresCategory)
        }
    }

    @Test
    fun `requiresPeer is true only for LEND RECEIVE BORROW REPAY`() {
        assertTrue(TransactionType.LEND.requiresPeer)
        assertTrue(TransactionType.RECEIVE.requiresPeer)
        assertTrue(TransactionType.BORROW.requiresPeer)
        assertTrue(TransactionType.REPAY.requiresPeer)

        val noPeer = listOf(
            TransactionType.INCOME,
            TransactionType.EXPENSE,
            TransactionType.SAVINGS,
            TransactionType.TRANSFER
        )
        noPeer.forEach { entry ->
            assertTrue("${entry.id}.requiresPeer should be false", !entry.requiresPeer)
        }
    }

    @Test
    fun `promptHint values are set for all entries`() {
        TransactionType.entries.forEach { entry ->
            assertTrue("${entry.id}.promptHint should not be blank", entry.promptHint.isNotBlank())
        }
    }
}
