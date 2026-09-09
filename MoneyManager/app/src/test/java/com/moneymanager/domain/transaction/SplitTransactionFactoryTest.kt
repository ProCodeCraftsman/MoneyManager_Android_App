package com.moneymanager.domain.transaction

import com.moneymanager.app.ui.dialogs.SplitRowData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitTransactionFactoryTest {

    @Test
    fun `encode then decode round-trips split rows`() {
        val rows = listOf(
            SplitRowData(localId = 0, categoryId = 1L, subCategoryId = 2L, description = "Groceries", amount = "300.0"),
            SplitRowData(localId = 1, categoryId = 3L, subCategoryId = null, description = "Snacks", amount = "50.0"),
        )

        val encoded = SplitTransactionFactory.encodeSplitTemplate(rows)
        val decoded = SplitTransactionFactory.decodeSplitTemplate(encoded)

        assertEquals(2, decoded.size)
        assertEquals(1L, decoded[0].categoryId)
        assertEquals(2L, decoded[0].subCategoryId)
        assertEquals("Groceries", decoded[0].description)
        assertEquals("300.0", decoded[0].amount)
        assertEquals(3L, decoded[1].categoryId)
        assertNull(decoded[1].subCategoryId)
    }

    @Test
    fun `decodeSplitTemplate returns empty list for null or blank input`() {
        assertTrue(SplitTransactionFactory.decodeSplitTemplate(null).isEmpty())
        assertTrue(SplitTransactionFactory.decodeSplitTemplate("").isEmpty())
        assertTrue(SplitTransactionFactory.decodeSplitTemplate("not json").isEmpty())
    }

    @Test
    fun `buildSplitChildrenFromTemplate builds correct child transactions`() {
        val rows = listOf(
            SplitRowData(localId = 0, categoryId = 1L, description = "Rent share", amount = "700.0"),
            SplitRowData(localId = 1, categoryId = 2L, description = "Utilities share", amount = "300.0"),
        )
        val splitData = SplitTransactionFactory.encodeSplitTemplate(rows)

        val children = SplitTransactionFactory.buildSplitChildrenFromTemplate(
            parentId = 99L,
            accountId = 5L,
            type = "expense",
            date = 1_700_000_000_000L,
            splitData = splitData,
        )

        assertEquals(2, children.size)
        assertTrue(children.all { it.isSplitChild })
        assertTrue(children.all { it.parentTransactionId == 99L })
        assertTrue(children.all { it.accountId == 5L })
        assertEquals(700.0, children[0].amount, 0.001)
        assertEquals(300.0, children[1].amount, 0.001)
    }

    @Test
    fun `buildSplitChildren skips rows with zero or unparseable amount`() {
        val rows = listOf(
            SplitRowData(localId = 0, categoryId = 1L, description = "Valid", amount = "100.0"),
            SplitRowData(localId = 1, categoryId = 2L, description = "Zero", amount = "0"),
            SplitRowData(localId = 2, categoryId = 3L, description = "Invalid", amount = "abc"),
        )

        val children = SplitTransactionFactory.buildSplitChildren(
            parentId = 1L,
            accountId = 1L,
            type = "expense",
            date = 0L,
            rows = rows,
        )

        assertEquals(1, children.size)
        assertEquals("Valid", children[0].description)
    }
}
