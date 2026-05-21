package com.moneymanager.domain.ai

data class CategoryEntry(val id: Long, val name: String, val type: String, val parentId: Long? = null)
data class AccountEntry(val id: Long, val name: String, val type: String = "general")
data class PeerEntry(val id: Long, val name: String)
data class TagEntry(val id: Long, val name: String)

data class PromptContext(
    val top20Categories: List<CategoryEntry>,
    val allCategories: List<CategoryEntry> = emptyList(),
    val accounts: List<AccountEntry>,
    val peers: List<PeerEntry>,
    val tags: List<TagEntry>
)
