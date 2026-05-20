package com.moneymanager.data.ai

data class PreExtraction(
    val amount: Double?,
    val typeId: String?,       // "expense" / "income" / null
    val epochMs: Long?,        // null → caller uses System.currentTimeMillis()
    val merchantHint: String?, // e.g. "Swiggy", "HDFC", "BigBasket"
)
