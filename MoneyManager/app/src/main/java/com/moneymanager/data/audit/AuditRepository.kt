package com.moneymanager.data.audit

import com.moneymanager.data.dao.CapturedMessageDao
import com.moneymanager.data.dao.MerchantCategoryMemoryDao
import com.moneymanager.data.entity.CapturedMessageEntity
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.audit.AuditChecks
import com.moneymanager.domain.audit.AuditFinding
import com.moneymanager.domain.audit.AuditRanker
import com.moneymanager.domain.audit.AuditTxn
import com.moneymanager.domain.audit.MemoryHint
import com.moneymanager.domain.repository.AccountRepository
import com.moneymanager.domain.repository.CategoryRepository
import com.moneymanager.domain.repository.TransactionRepository
import com.moneymanager.domain.sms.MerchantKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gathers what the audit checks need from the database and preferences, runs them, and keeps the findings the inbox
 * shows (F08 A2). Reads only; the only write is an explicit fix or "It's correct" from the inbox.
 */
@Singleton
class AuditRepository @Inject constructor(
    private val transactions: TransactionRepository,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    private val ledger: CapturedMessageDao,
    private val memory: MerchantCategoryMemoryDao,
    private val preferences: PreferencesManager,
) {
    /** Recomputes the findings and stores the ones to show. Called by the nightly worker and by "Check now". */
    suspend fun runAndStore(now: Long = System.currentTimeMillis()): List<AuditFinding> = withContext(Dispatchers.IO) {
        val (recent, history) = load(now)
        val memoryHints = memory.getAll().associate { it.merchantKey to MemoryHint(it.categoryId, it.hitCount) }
        val findings = AuditChecks.run(recent, history, memoryHints, balances = emptyList(), now = now)
        val shown = AuditRanker.top(findings, preferences.getAuditDismissed())
        preferences.setAuditFindingsJson(AuditStore.encode(shown))
        shown
    }

    fun observeShown(): Flow<List<AuditFinding>> = preferences.auditFindingsJson.map { AuditStore.decode(it) }

    /** "It's correct": the fingerprint is kept so the same finding is never raised again, and the row leaves the list. */
    suspend fun dismiss(finding: AuditFinding) {
        preferences.setAuditDismissed(preferences.getAuditDismissed() + finding.fingerprint)
        removeShown(finding)
    }

    /** A1 "Change to X". A category-only edit, so no account balance moves. The sub-category is cleared with it. */
    suspend fun changeCategory(finding: AuditFinding, categoryId: Long) {
        val txId = finding.txnId ?: return
        val tx = transactions.getTransactionById(txId) ?: return
        transactions.updateTransaction(tx.copy(categoryId = categoryId, subCategoryId = null))
        removeShown(finding)
    }

    private suspend fun removeShown(finding: AuditFinding) {
        val remaining = AuditStore.decode(preferences.auditFindingsJson.first()).filter { it.fingerprint != finding.fingerprint }
        preferences.setAuditFindingsJson(AuditStore.encode(remaining))
    }

    /**
     * Every saved row as the checks see it, plus the rows created in the last 30 days (what the worker reviews).
     * Split parents are left out: they carry a category that is only a summary, not a spend.
     */
    private suspend fun load(now: Long): Pair<List<AuditTxn>, List<AuditTxn>> {
        val accountById = accounts.getAllAccounts().first().associateBy { it.id }
        val categoryById = categories.getAllCategoriesWithArchived().first().associateBy { it.id }
        val links = preferences.getSmsAccountLinks()
        val smsByTransaction = ledger.getAll()
            .filter { it.status == CapturedMessageEntity.STATUS_SAVED }
            .mapNotNull { row -> row.transactionId?.let { it to row } }
            .toMap()

        val history = transactions.getAllTransactions().first()
            .filter { !it.isSplitParent }
            .map { tx ->
                val sms = smsByTransaction[tx.id]
                AuditTxn(
                    id = tx.id,
                    type = tx.type,
                    amount = tx.amount,
                    date = tx.date,
                    createdAt = tx.createdAt,
                    categoryId = tx.categoryId,
                    categoryType = tx.categoryId?.let { categoryById[it]?.type },
                    accountId = tx.accountId,
                    accountArchived = accountById[tx.accountId]?.isArchived ?: false,
                    toAccountId = tx.toAccountId,
                    goalId = tx.goalId,
                    merchantKey = tx.description.takeIf { it.isNotBlank() }
                        ?.let(MerchantKey::canonical)?.takeIf { it.isNotEmpty() },
                    sourceAccountId = sms?.sourceKey?.let { links[it] },
                    smsDate = sms?.messageDate,
                )
            }
        val recent = history.filter { now - it.createdAt <= RECENT_MS }
        return recent to history
    }

    private companion object {
        const val RECENT_MS = 30L * 24 * 60 * 60 * 1000
    }
}
