package com.moneymanager.app.ui.peerlist

import com.moneymanager.data.entity.PeerContact

data class PeerListUiState(
    val peers: List<PeerContact> = emptyList(),
    val totalLent: Double = 0.0,
    val totalReceived: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val currencyCode: String = "INR",
    val isLoading: Boolean = true,
)
