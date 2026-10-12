package com.moneymanager.data.repository

import com.moneymanager.data.dao.AccountDao
import com.moneymanager.data.dao.CapturedMessageDao
import com.moneymanager.data.dao.TransactionDao
import com.moneymanager.data.entity.CapturedMessageEntity
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.ai.AccountEntry
import com.moneymanager.domain.ai.TransactionDraft
import com.moneymanager.domain.capture.CaptureCandidate
import com.moneymanager.domain.capture.CaptureDedup
import com.moneymanager.domain.capture.CaptureReview
import com.moneymanager.domain.capture.CaptureStore
import com.moneymanager.domain.capture.LedgerMatch
import com.moneymanager.domain.capture.PendingCapture
import com.moneymanager.domain.capture.ScanCursor
import com.moneymanager.domain.learn.SavedHistory
import com.moneymanager.domain.learn.SavedRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The capture ledger, the scan cursor, the inbox's review actions, and read access to saved transactions for the
 * scanner, the dedup check and the history join.
 */
@Singleton
class CaptureRepository @Inject constructor(
    private val ledger: CapturedMessageDao,
    private val transactions: TransactionDao,
    private val accounts: AccountDao,
    private val preferences: PreferencesManager,
) : CaptureStore, CaptureReview, SavedHistory {

    override suspend fun isCaptured(messageHash: String): Boolean = ledger.findByHash(messageHash) != null

    override suspend fun insertRow(message: CapturedMessageEntity): Boolean = ledger.insertIfNew(message) != -1L

    override suspend fun cursor(): ScanCursor =
        ScanCursor(preferences.getCaptureLastScanned(), preferences.getCaptureStartDate())

    override suspend fun saveCursor(lastScanned: Long) = preferences.setCaptureLastScanned(lastScanned)

    override suspend fun hasSavedTransactionNear(amount: Double, date: Long): Boolean =
        transactions.countNearAmount(amount, date - ONE_DAY_MS, date + ONE_DAY_MS) > 0

    override suspend fun activeAccounts(): List<AccountEntry> =
        accounts.getActiveAccounts().first().map { AccountEntry(id = it.id, name = it.name, type = it.type) }

    override fun observePendingCount(): Flow<Int> = ledger.observePendingCount()

    override suspend fun ledgerRowWithRef(refHash: String): LedgerMatch? =
        ledger.findByRefHash(refHash).firstOrNull()?.let { row ->
            when (row.status) {
                CapturedMessageEntity.STATUS_SAVED -> LedgerMatch(row.id, row.transactionId)
                CapturedMessageEntity.STATUS_MERGED -> LedgerMatch(row.mergedIntoId ?: row.id, null)
                else -> LedgerMatch(row.id, null)
            }
        }

    override suspend fun savedCandidatesNear(amount: Double, date: Long): List<CaptureCandidate> =
        transactions.findUnclaimedNearAmount(amount, date - CaptureDedup.WINDOW_MS, date + CaptureDedup.WINDOW_MS).map { tx ->
            CaptureCandidate(
                id = tx.id,
                typeId = tx.type,
                amount = tx.amount,
                date = tx.date,
                accountId = tx.accountId,
                merchantText = "${tx.description} ${tx.note}",
            )
        }

    override suspend fun pendingCandidatesNear(amount: Double, date: Long): List<CaptureCandidate> =
        ledger.findInWindow(amount, date - CaptureDedup.WINDOW_MS, date + CaptureDedup.WINDOW_MS)
            .filter { it.status == CapturedMessageEntity.STATUS_PENDING }
            .mapNotNull { row ->
                val draft = decode(row.draftJson) ?: return@mapNotNull null
                CaptureCandidate(
                    id = row.id,
                    typeId = draft.typeId,
                    amount = row.amount ?: return@mapNotNull null,
                    date = draft.date ?: row.messageDate,
                    accountId = draft.accountId,
                    merchantText = draft.merchantHint.orEmpty(),
                )
            }

    override fun observePending(): Flow<List<PendingCapture>> =
        ledger.observePending().map { rows -> rows.mapNotNull { it.toPending() } }

    override suspend fun pendingDrafts(): List<PendingCapture> = ledger.getPending().mapNotNull { it.toPending() }

    override suspend fun markSaved(ledgerId: Long, transactionId: Long) =
        ledger.updateState(ledgerId, CapturedMessageEntity.STATUS_SAVED, transactionId = transactionId)

    override suspend fun markRejected(ledgerId: Long) =
        ledger.updateState(ledgerId, CapturedMessageEntity.STATUS_REJECTED)

    override suspend fun restorePending(ledgerId: Long, draft: TransactionDraft) =
        ledger.updateState(ledgerId, CapturedMessageEntity.STATUS_PENDING, draftJson = JSON.encodeToString(TransactionDraft.serializer(), draft))

    override suspend fun updateDraft(ledgerId: Long, draft: TransactionDraft) =
        ledger.updateDraftJson(ledgerId, JSON.encodeToString(TransactionDraft.serializer(), draft))

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

    private fun CapturedMessageEntity.toPending(): PendingCapture? {
        val draft = decode(draftJson) ?: return null
        return PendingCapture(ledgerId = id, draft = draft, messageDate = messageDate)
    }

    private fun decode(json: String?): TransactionDraft? =
        json?.let { runCatching { JSON.decodeFromString(TransactionDraft.serializer(), it) }.getOrNull() }

    private companion object {
        const val ONE_DAY_MS = 24 * 60 * 60 * 1000L
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
