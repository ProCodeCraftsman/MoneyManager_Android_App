package com.moneymanager.domain.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MerchantHistoryIndexTest {

    private fun row(desc: String, type: String, cat: Long, name: String, n: Int) = HistoryRow(desc, type, cat, name, n)

    private val index = MerchantHistoryIndex(
        listOf(
            row("Swiggy", "expense", 1, "Food", 9),
            row("swiggy", "expense", 2, "Groceries", 1),
            row("Zomato", "expense", 1, "Food", 4),
            row("Zomato", "expense", 3, "Treats", 4),
            row("Kerala Visitors Hotel", "expense", 4, "Travel", 2),
            row("Salary", "income", 5, "Salary", 6),
            row("SBI Card", "transfer", 6, "Transfers", 3),
        )
    )

    @Test
    fun `exact match picks the majority category`() {
        val s = index.suggest("SWIGGY", "expense")!!
        assertEquals(1L, s.categoryId)
        assertEquals("Food", s.categoryName)
    }

    @Test
    fun `no clear majority gives no suggestion`() {
        assertNull(index.suggest("ZOMATO", "expense"))
    }

    @Test
    fun `bank truncated names match by prefix`() {
        assertEquals(4L, index.suggest("KERALA VISI", "expense")?.categoryId)
    }

    @Test
    fun `type must agree and transfers are ignored`() {
        assertNull(index.suggest("Salary", "expense"))
        assertNotNull(index.suggest("Salary", "income"))
        assertNull(index.suggest("SBI Card", null))
    }

    @Test
    fun `short or unknown hints give nothing`() {
        assertNull(index.suggest("AB", "expense"))
        assertNull(index.suggest("Unknown Store", "expense"))
    }
}
