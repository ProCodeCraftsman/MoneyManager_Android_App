package com.moneymanager.domain.capture

import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.domain.ai.TransactionDraft
import kotlinx.serialization.Serializable
import kotlin.math.abs

/**
 * Which fields the user changed between a captured draft and the transaction they saved (AI-014). Only field names
 * are recorded, never values, so the stats cannot leak what was spent or where.
 *
 * A field the draft left empty and the user filled counts as changed. A field the draft never set (the date is
 * often left to the form's default) does not count as changed.
 */
object DraftEdits {
    const val TYPE = "type"
    const val AMOUNT = "amount"
    const val CATEGORY = "category"
    const val ACCOUNT = "account"
    const val TO_ACCOUNT = "to_account"
    const val DATE = "date"
    const val NOTE = "note"
    const val PEER = "peer"
    const val TAGS = "tags"

    fun changedFields(draft: TransactionDraft, saved: TransactionEntity): List<String> = buildList {
        if (draft.typeId != null && draft.typeId != saved.type) add(TYPE)
        if (draft.amount != null && abs(draft.amount - saved.amount) >= AMOUNT_EPSILON) add(AMOUNT)
        if (draft.categoryId != saved.categoryId) add(CATEGORY)
        if (draft.accountId != saved.accountId) add(ACCOUNT)
        if (draft.toAccountId != null && draft.toAccountId != saved.toAccountId) add(TO_ACCOUNT)
        if (draft.date != null && draft.date != saved.date) add(DATE)
        val draftNote = draft.note ?: draft.description
        if (draftNote != null && draftNote.trim() != saved.note.trim()) add(NOTE)
        if (draft.peerContactId != null && draft.peerContactId != saved.peerContactId) add(PEER)
        if (draft.tagIds.isNotEmpty() && draft.tagIds.joinToString(",") != saved.tagIds) add(TAGS)
    }

    private const val AMOUNT_EPSILON = 0.005
}

/** Edit counts for one capture source (the draft's sourceType, e.g. "SMS"). */
@Serializable
data class SourceEditStat(
    val saves: Int = 0,
    val zeroEditSaves: Int = 0,
    val fieldEdits: Map<String, Int> = emptyMap(),
)

/** Per-source zero-edit rate, the number F07 uses for "70% of drafts approved with zero edits". */
@Serializable
data class DraftEditStats(val bySource: Map<String, SourceEditStat> = emptyMap()) {

    fun record(sourceType: String, changedFields: List<String>): DraftEditStats {
        val current = bySource[sourceType] ?: SourceEditStat()
        val fields = changedFields.fold(current.fieldEdits) { acc, field -> acc + (field to (acc[field] ?: 0) + 1) }
        val updated = current.copy(
            saves = current.saves + 1,
            zeroEditSaves = current.zeroEditSaves + if (changedFields.isEmpty()) 1 else 0,
            fieldEdits = fields,
        )
        return copy(bySource = bySource + (sourceType to updated))
    }
}
