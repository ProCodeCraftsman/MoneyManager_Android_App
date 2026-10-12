package com.moneymanager.domain.capture

import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.domain.ai.TransactionDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftEditsTest {

    private val date = 1_000L
    private val draft = TransactionDraft(
        typeId = "expense", amount = 450.0, categoryId = 3, accountId = 7,
        merchantHint = "SWIGGY", date = date, note = "Swiggy",
    )
    private val saved = TransactionEntity(
        id = 1, accountId = 7, type = "expense", amount = 450.0, categoryId = 3,
        date = date, note = "Swiggy", description = "Swiggy",
    )

    @Test
    fun `saving an untouched draft records no edits`() {
        assertTrue(DraftEdits.changedFields(draft, saved).isEmpty())
    }

    @Test
    fun `a changed category is one field edit with no value recorded`() {
        val changed = DraftEdits.changedFields(draft, saved.copy(categoryId = 4))
        assertEquals(listOf(DraftEdits.CATEGORY), changed)
    }

    @Test
    fun `a field the draft left empty counts as edited when the user fills it`() {
        val noCategory = draft.copy(categoryId = null)
        assertEquals(listOf(DraftEdits.CATEGORY), DraftEdits.changedFields(noCategory, saved))
    }

    @Test
    fun `a date the draft never set is not an edit`() {
        assertTrue(DraftEdits.changedFields(draft.copy(date = null), saved.copy(date = 99L)).isEmpty())
    }

    @Test
    fun `amount compares with a paisa tolerance`() {
        assertTrue(DraftEdits.changedFields(draft, saved.copy(amount = 450.001)).isEmpty())
        assertEquals(listOf(DraftEdits.AMOUNT), DraftEdits.changedFields(draft, saved.copy(amount = 451.0)))
    }

    @Test
    fun `stats count saves, zero-edit saves and per-field edits per source`() {
        val stats = DraftEditStats()
            .record("SMS", emptyList())
            .record("SMS", listOf(DraftEdits.CATEGORY, DraftEdits.NOTE))
            .record("SMS", listOf(DraftEdits.CATEGORY))
            .record("VOICE", emptyList())

        val sms = stats.bySource.getValue("SMS")
        assertEquals(3, sms.saves)
        assertEquals(1, sms.zeroEditSaves)
        assertEquals(2, sms.fieldEdits[DraftEdits.CATEGORY])
        assertEquals(1, sms.fieldEdits[DraftEdits.NOTE])
        assertEquals(1, stats.bySource.getValue("VOICE").zeroEditSaves)
    }
}
