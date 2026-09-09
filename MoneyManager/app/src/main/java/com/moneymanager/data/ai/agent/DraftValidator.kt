package com.moneymanager.data.ai.agent

import com.moneymanager.domain.ai.TransactionDraft

object DraftValidator {

    data class Result(val valid: Boolean, val issues: List<String>)

    private val validTypes = setOf("expense", "income", "transfer", "savings", "lend", "borrow")

    fun validate(draft: TransactionDraft): Result {
        val issues = buildList {
            if (draft.typeId != null && draft.typeId.lowercase() !in validTypes)
                add("typeId '${draft.typeId}' is invalid; must be one of $validTypes")
            if (draft.amount != null && draft.amount <= 0)
                add("amount ${draft.amount} must be positive")
        }
        return Result(valid = issues.isEmpty(), issues = issues)
    }
}
