package com.moneymanager.data.ai

data class PreExtraction(
    val amount: Double?,
    val typeId: String?,       // "expense" / "income" / null
    val epochMs: Long?,        // null → caller uses System.currentTimeMillis()
    val merchantHint: String?, // e.g. "Swiggy", "HDFC", "BigBasket"
    /** Extra ground-truth facts for the prompt, e.g. "instrument=credit_card", "institution=HDFC Bank". */
    val hints: List<String> = emptyList(),
)
