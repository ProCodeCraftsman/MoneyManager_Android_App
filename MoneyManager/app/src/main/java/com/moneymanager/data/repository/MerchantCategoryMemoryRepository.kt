package com.moneymanager.data.repository

import com.moneymanager.data.dao.MerchantCategoryMemoryDao
import com.moneymanager.data.dao.TransactionDao
import com.moneymanager.data.entity.MerchantCategoryMemoryEntity
import com.moneymanager.domain.sms.HistoryRow
import com.moneymanager.domain.sms.MerchantHistoryIndex
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerchantCategoryMemoryRepository @Inject constructor(
    private val dao: MerchantCategoryMemoryDao,
    private val transactionDao: TransactionDao,
) {
    @Volatile private var historyIndex: MerchantHistoryIndex? = null
    @Volatile private var historyBuiltAt = 0L

    /**
     * Look up cached category for a merchant. Returns null if unknown or key is too short.
     * Call this BEFORE triggering the AI inference — a hit means we can skip the AI call.
     */
    suspend fun lookup(merchantHint: String): MerchantCategoryMemoryEntity? {
        val key = normalize(merchantHint)
        if (key.length < 3) return null
        return dao.findByKey(key)
    }

    /**
     * Like [lookup], but when nothing was learned yet for [merchantHint] falls back to the category the user
     * already uses for similar descriptions in their transaction history. The result is a suggestion and is
     * not persisted. A remembered entry only counts when it was learned for the same [typeId].
     */
    suspend fun lookupWithHistory(merchantHint: String, typeId: String?): MerchantCategoryMemoryEntity? {
        lookup(merchantHint)?.takeIf { typeId == null || it.typeId == null || it.typeId == typeId }?.let { return it }
        val s = history().suggest(merchantHint, typeId) ?: return null
        return MerchantCategoryMemoryEntity(
            merchantKey = MerchantHistoryIndex.normalize(merchantHint),
            categoryId = s.categoryId,
            categoryName = s.categoryName,
            typeId = s.typeId,
            hitCount = s.count,
        )
    }

    private suspend fun history(): MerchantHistoryIndex {
        val now = System.currentTimeMillis()
        historyIndex?.takeIf { now - historyBuiltAt < HISTORY_TTL_MS }?.let { return it }
        val rows = transactionDao.getMerchantCategoryCounts()
            .map { HistoryRow(it.description, it.type, it.categoryId, it.categoryName, it.count) }
        return MerchantHistoryIndex(rows).also {
            historyIndex = it
            historyBuiltAt = now
        }
    }

    /**
     * Record a merchant→category association after a successful inference.
     * If the same merchant already maps to the same category, increments the hit counter.
     * If it maps to a different category (user corrected it), overwrites with the new one.
     */
    suspend fun record(merchantHint: String, categoryId: Long, categoryName: String, typeId: String?) {
        val key = normalize(merchantHint)
        if (key.length < 3) return
        val existing = dao.findByKey(key)
        if (existing != null && existing.categoryId == categoryId) {
            dao.incrementHit(key)
        } else {
            dao.upsert(
                MerchantCategoryMemoryEntity(
                    merchantKey = key,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    typeId = typeId,
                )
            )
        }
    }

    private fun normalize(raw: String): String = MerchantHistoryIndex.normalize(raw)

    private companion object {
        const val HISTORY_TTL_MS = 60_000L
    }
}
