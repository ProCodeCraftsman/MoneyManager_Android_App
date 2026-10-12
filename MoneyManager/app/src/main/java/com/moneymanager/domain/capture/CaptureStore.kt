package com.moneymanager.domain.capture

import com.moneymanager.data.entity.CapturedMessageEntity
import com.moneymanager.domain.ai.AccountEntry
import kotlinx.coroutines.flow.Flow

/** Where the scanner remembers what it has seen. Implemented over Room and DataStore in the data layer. */
interface CaptureStore {
    suspend fun isCaptured(messageHash: String): Boolean

    /** False when a row with the same message hash already exists. Pending, saved and merged rows all go through here. */
    suspend fun insertRow(message: CapturedMessageEntity): Boolean

    suspend fun cursor(): ScanCursor

    suspend fun saveCursor(lastScanned: Long)

    /** Whether a saved transaction of this amount exists within a day of [date], used to tell known spends apart. */
    suspend fun hasSavedTransactionNear(amount: Double, date: Long): Boolean

    suspend fun activeAccounts(): List<AccountEntry>

    /** Number of drafts waiting for review. Drives the home banner and the reminder's "notify only if > 0". */
    fun observePendingCount(): Flow<Int>

    /** The ledger row that already holds this reference number, for the strongest duplicate check (AI-054). */
    suspend fun ledgerRowWithRef(refHash: String): LedgerMatch? = null

    /** Saved transactions of [amount] within the dedup window that no ledger row has claimed yet (AI-054). */
    suspend fun savedCandidatesNear(amount: Double, date: Long): List<CaptureCandidate> = emptyList()

    /** Pending drafts of [amount] within the dedup window (AI-054). */
    suspend fun pendingCandidatesNear(amount: Double, date: Long): List<CaptureCandidate> = emptyList()
}

/** [lastScanned] is the newest message date the scanner has fully processed; [startDate] is the user's chosen floor. */
data class ScanCursor(val lastScanned: Long?, val startDate: Long?)
