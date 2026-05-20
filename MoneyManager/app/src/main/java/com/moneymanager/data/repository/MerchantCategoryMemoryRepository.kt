package com.moneymanager.data.repository

import com.moneymanager.data.dao.MerchantCategoryMemoryDao
import com.moneymanager.data.entity.MerchantCategoryMemoryEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerchantCategoryMemoryRepository @Inject constructor(
    private val dao: MerchantCategoryMemoryDao,
) {
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

    private fun normalize(raw: String): String =
        raw.lowercase().replace(Regex("[^a-z0-9]"), "").take(40)
}
