package com.moneymanager.domain.ai

class PromptContextBuilder {

    fun build(
        categories: List<CategoryEntry>,
        categoryUsageCounts: Map<Long, Int>,
        accounts: List<AccountEntry>,
        peers: List<PeerEntry>,
        tags: List<TagEntry>
    ): PromptContext {
        val top20 = categories
            .sortedByDescending { categoryUsageCounts[it.id] ?: 0 }
            .take(20)

        return PromptContext(
            top20Categories = top20,
            accounts = accounts,
            peers = peers,
            tags = tags
        )
    }
}
