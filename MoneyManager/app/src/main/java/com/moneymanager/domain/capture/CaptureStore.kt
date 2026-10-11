package com.moneymanager.domain.capture

import com.moneymanager.data.entity.CapturedMessageEntity
import com.moneymanager.domain.ai.AccountEntry

/** Where the scanner remembers what it has seen. Implemented over Room and DataStore in the data layer. */
interface CaptureStore {
    suspend fun isCaptured(messageHash: String): Boolean

    /** False when a row with the same message hash already exists. */
    suspend fun insertPending(message: CapturedMessageEntity): Boolean

    suspend fun cursor(): ScanCursor

    suspend fun saveCursor(lastScanned: Long)

    /** Whether a saved transaction of this amount exists within a day of [date], used to tell known spends apart. */
    suspend fun hasSavedTransactionNear(amount: Double, date: Long): Boolean

    suspend fun activeAccounts(): List<AccountEntry>
}

/** [lastScanned] is the newest message date the scanner has fully processed; [startDate] is the user's chosen floor. */
data class ScanCursor(val lastScanned: Long?, val startDate: Long?)
