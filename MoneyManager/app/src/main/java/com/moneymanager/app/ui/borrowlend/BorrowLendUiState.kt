package com.moneymanager.app.ui.borrowlend

import com.moneymanager.data.entity.AccountEntity

data class BorrowLendUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val selectedAccountId: Long? = null,
    val selectedPeerId: Long? = null,
    val peerName: String = "",
    val amount: Double = 0.0,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val expectedReturnDate: Long? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
    val currencyCode: String = "INR"
)
