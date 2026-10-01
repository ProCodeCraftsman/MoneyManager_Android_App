package com.moneymanager.ui.util

import com.moneymanager.app.ui.util.AmountEdit
import com.moneymanager.app.ui.util.AmountEditor
import org.junit.Assert.assertEquals
import org.junit.Test

class AmountEditorTest {

    @Test
    fun `typing at the end appends like before`() {
        assertEquals(AmountEdit("12", 2), AmountEditor.insert("1", 1, "2"))
        assertEquals(AmountEdit("5", 1), AmountEditor.insert("0", 1, "5"))
        assertEquals(AmountEdit("", 0), AmountEditor.insert("", 0, "+"))
        assertEquals(AmountEdit("5-", 2), AmountEditor.insert("5+", 2, "-"))
    }

    @Test
    fun `digit is inserted in the middle`() {
        assertEquals(AmountEdit("1923", 2), AmountEditor.insert("123", 1, "9"))
    }

    @Test
    fun `delete removes the digit before the cursor`() {
        assertEquals(AmountEdit("153", 1), AmountEditor.delete("1253", 2))
        assertEquals(AmountEdit("125", 3), AmountEditor.delete("1253", 4))
        assertEquals(AmountEdit("1253", 0), AmountEditor.delete("1253", 0))
    }

    @Test
    fun `decimal point is limited to one per number`() {
        assertEquals(AmountEdit("1.5", 2), AmountEditor.insert("15", 1, "."))
        assertEquals(AmountEdit("1.5", 3), AmountEditor.insert("1.5", 3, "."))
        assertEquals(AmountEdit("1.5+2.", 6), AmountEditor.insert("1.5+2", 5, "."))
        assertEquals(AmountEdit("0.5", 2), AmountEditor.insert("5", 0, "."))
        assertEquals(AmountEdit("5+0.", 4), AmountEditor.insert("5+", 2, "."))
    }

    @Test
    fun `operators replace neighbours instead of doubling up`() {
        assertEquals(AmountEdit("5*3", 2), AmountEditor.insert("5+3", 2, "*"))
        assertEquals(AmountEdit("5*3", 2), AmountEditor.insert("5+3", 1, "*"))
        assertEquals(AmountEdit("1+23", 2), AmountEditor.insert("123", 1, "+"))
        assertEquals(AmountEdit("1+", 2), AmountEditor.insert("1.", 2, "+"))
    }

    @Test
    fun `deleting a digit between operators keeps the expression valid`() {
        assertEquals(AmountEdit("5*2", 1), AmountEditor.delete("5+3*2", 3))
        assertEquals(AmountEdit("3", 0), AmountEditor.delete("5+3", 1))
    }
}
