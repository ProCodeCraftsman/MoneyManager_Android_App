package com.moneymanager.domain.sms

import com.moneymanager.domain.ai.CategoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsCategoryPromptTest {

    private val food = CategoryEntry(1, "Food", "expense")
    private val travel = CategoryEntry(2, "Travel", "expense")
    private val bills = CategoryEntry(3, "Bills", "expense")
    private val salary = CategoryEntry(4, "Salary", "income")
    private val list = listOf(food, travel, bills)

    @Test
    fun `candidates are the matching type with the most used first and no duplicates`() {
        val got = SmsCategoryPrompt.candidates("expense", mostUsed = listOf(bills, salary), all = listOf(food, travel, bills, salary))
        assertEquals(listOf(bills, food, travel), got)
    }

    @Test
    fun `candidates are capped and empty when the type has none or is unknown`() {
        val many = (1L..40L).map { CategoryEntry(it, "C$it", "expense") }
        assertEquals(SmsCategoryPrompt.MAX_CANDIDATES, SmsCategoryPrompt.candidates("expense", emptyList(), many).size)
        assertTrue(SmsCategoryPrompt.candidates("savings", emptyList(), many).isEmpty())
        assertTrue(SmsCategoryPrompt.candidates(null, emptyList(), many).isEmpty())
    }

    @Test
    fun `prompt lists the choices and carries only merchant and amount`() {
        val p = SmsCategoryPrompt.build("expense", "KERALA VISI", 250.0, list)
        assertTrue(p.contains("1. Food"))
        assertTrue(p.contains("3. Bills"))
        assertTrue(p.contains("\"KERALA VISI\""))
        assertTrue(p.contains("of 250 at"))
        assertFalse(p.contains("250.0"))
    }

    @Test
    fun `a clean number picks that category`() {
        assertEquals(travel, SmsCategoryPrompt.parse("2", list))
        assertEquals(travel, SmsCategoryPrompt.parse("  2.  Travel\n", list))
        assertEquals(food, SmsCategoryPrompt.parse("1. food", list))
    }

    @Test
    fun `zero, out of range, prose and empty replies pick nothing`() {
        assertNull(SmsCategoryPrompt.parse("0", list))
        assertNull(SmsCategoryPrompt.parse("4", list))
        assertNull(SmsCategoryPrompt.parse("", list))
        assertNull(SmsCategoryPrompt.parse("I think it is Travel", list))
        assertNull(SmsCategoryPrompt.parse("None of these", list))
    }

    @Test
    fun `a number that contradicts the name that follows is not trusted`() {
        assertNull(SmsCategoryPrompt.parse("1. Travel", list))
        assertEquals(food, SmsCategoryPrompt.parse("1. Something odd", list))
    }
}
