package com.moneymanager.ui.dialog

import com.moneymanager.app.ui.dialogs.getNextCircularTypeId
import org.junit.Assert.assertEquals
import org.junit.Test

class CircularTypeSwipeTest {

    @Test
    fun `swiping left moves forward through transaction types in circular order`() {
        assertEquals("income", getNextCircularTypeId("expense", 1))
        assertEquals("savings", getNextCircularTypeId("income", 1))
        assertEquals("transfer", getNextCircularTypeId("savings", 1))
        assertEquals("lend", getNextCircularTypeId("transfer", 1))
        assertEquals("borrow", getNextCircularTypeId("lend", 1))
        // Circular wrap around from last item to first item
        assertEquals("expense", getNextCircularTypeId("borrow", 1))
    }

    @Test
    fun `swiping right moves backward through transaction types in circular order`() {
        // Circular wrap around from first item to last item
        assertEquals("borrow", getNextCircularTypeId("expense", -1))
        assertEquals("lend", getNextCircularTypeId("borrow", -1))
        assertEquals("transfer", getNextCircularTypeId("lend", -1))
        assertEquals("savings", getNextCircularTypeId("transfer", -1))
        assertEquals("income", getNextCircularTypeId("savings", -1))
        assertEquals("expense", getNextCircularTypeId("income", -1))
    }

    @Test
    fun `unknown current type falls back safely`() {
        assertEquals("income", getNextCircularTypeId("unknown_type", 1))
        assertEquals("borrow", getNextCircularTypeId("unknown_type", -1))
    }
}
