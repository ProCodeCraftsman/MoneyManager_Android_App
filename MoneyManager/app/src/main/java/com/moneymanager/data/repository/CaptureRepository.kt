package com.moneymanager.data.repository

import com.moneymanager.data.dao.AccountDao
import com.moneymanager.data.dao.CapturedMessageDao
import com.moneymanager.data.dao.TransactionDao
import com.moneymanager.data.entity.CapturedMessageEntity
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.ai.AccountEntry
import com.moneymanager.domain.capture.CaptureStore
import com.moneymanager.domain.capture.ScanCursor
import com.moneymanager.domain.learn.SavedHistory
import com.moneymanager.domain.learn.SavedRow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** The capture ledger, the scan cursor, and read access to saved transactions for the scanner and history join. */
@Singleton
class CaptureRepository @Inject constructor(
    private val ledger: CapturedMessageDao,
    private val transactions: TransactionDao,
    private val accounts: AccountDao,
    private val preferences: PreferencesManager,
) : CaptureStore, SavedHistory {

    override suspend fun isCaptured(messageHash: String): Boolean = ledger.findByHash(messageHash) != null

    override suspend fun insertPending(message: CapturedMessageEntity): Boolean = ledger.insertIfNew(message) != -1L

    override suspend fun cursor(): ScanCursor =
        ScanCursor(preferences.getCaptureLastScanned(), preferences.getCaptureStartDate())

    override suspend fun saveCursor(lastScanned: Long) = preferences.setCaptureLastScanned(lastScanned)

    override suspend fun hasSavedTransactionNear(amount: Double, date: Long): Boolean =
        transactions.countNearAmount(amount, date - ONE_DAY_MS, date + ONE_DAY_MS) > 0

    override suspend fun activeAccounts(): List<AccountEntry> =
        accounts.getActiveAccounts().first().map { AccountEntry(id = it.id, name = it.name, type = it.type) }

    override suspend fun rowsBetween(from: Long, to: Long): List<SavedRow> =
        transactions.getRowsForHistoryJoin(from, to).map {
            SavedRow(
                id = it.id,
                typeId = it.type,
                amount = it.amount,
                date = it.date,
                categoryId = it.categoryId,
                categoryName = it.categoryName,
                accountId = it.accountId,
                accountName = it.accountName,
                accountType = it.accountType,
            )
        }

    private companion object {
        const val ONE_DAY_MS = 24 * 60 * 60 * 1000L
    }
}
