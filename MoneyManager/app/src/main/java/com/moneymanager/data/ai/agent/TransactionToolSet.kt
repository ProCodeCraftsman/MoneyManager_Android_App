package com.moneymanager.data.ai.agent

import android.util.Log
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import com.moneymanager.data.repository.MerchantCategoryMemoryRepository
import com.moneymanager.domain.repository.AccountRepository
import com.moneymanager.domain.repository.CategoryRepository
import com.moneymanager.domain.repository.PeerContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TransactionToolSet"

/**
 * Progress action emitted before/after each tool call for UI feedback.
 * Mirrors gallery's SkillProgressAgentAction pattern.
 */
data class ToolProgressAction(
    val toolName: String,
    val inProgress: Boolean,
    val description: String = "",
)

/**
 * ToolSet exposing app DB to the on-device LLM during draft generation.
 *
 * Mirrors the gallery's AgentTools pattern: @Tool annotations let litertlm-android
 * auto-generate the function-calling schema; methods are synchronous and use
 * runBlocking(Dispatchers.IO) to bridge coroutine repositories — same approach as gallery.
 *
 * The LLM calls these instead of hallucinating account names, category names, and peer
 * names from the prompt. Each tool returns Map<String, String> (gallery convention).
 *
 * Progress is reported via [actionChannel] — UI can collect and show a thin indicator
 * so the user sees tool execution happening (gallery's AgentAction pattern).
 */
@Singleton
class TransactionToolSet @Inject constructor(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val peerRepository: PeerContactRepository,
    private val merchantMemory: MerchantCategoryMemoryRepository,
) : ToolSet {

    private val _actionChannel = Channel<ToolProgressAction>(Channel.UNLIMITED)
    val actionChannel: ReceiveChannel<ToolProgressAction> = _actionChannel

    private fun sendProgress(toolName: String, inProgress: Boolean, description: String = "") {
        runBlocking(Dispatchers.Default) {
            _actionChannel.send(ToolProgressAction(toolName, inProgress, description))
        }
    }

    @Tool(description = "Returns the user's default account name and id. Call this to get a clear view of available transaction's account and to compare the text input")
    fun getDefaultAccount(): Map<String, String> = runBlocking(Dispatchers.IO) {
        sendProgress("getDefaultAccount", true, "Looking up accounts...")
        try {
            val accounts = accountRepository.getAllAccounts().first()
            val default = accounts.firstOrNull { it.type != "peer" } ?: accounts.firstOrNull()
            if (default != null) {
                Log.d(TAG, "getDefaultAccount → ${default.name}(${default.id})")
                mapOf("account_id" to default.id.toString(), "account_name" to default.name)
            } else {
                mapOf("error" to "No accounts configured in the app")
            }
        } finally {
            sendProgress("getDefaultAccount", false)
        }
    }

    @Tool(description = "Search app categories by keyword. Returns up to 5 matching category names, ids, and types. Call this when getMerchantCategory returns no history.")
    fun searchCategories(
        @ToolParam(description = "Keyword related to the transaction, e.g. 'food', 'fuel', 'salary', 'rent', 'medical'")
        query: String,
    ): Map<String, String> = runBlocking(Dispatchers.IO) {
        sendProgress("searchCategories", true, "Searching categories for '$query'...")
        try {
            val allCats = categoryRepository.getAllCategories().first()
            val q = query.trim().lowercase()
            val matches = allCats
                .filter { cat ->
                    val n = cat.name.lowercase()
                    n.contains(q) || q.split("\\s+".toRegex()).any { word -> word.length > 2 && n.contains(word) }
                }
                .take(5)
            if (matches.isEmpty()) {
                Log.d(TAG, "searchCategories('$query') → no matches")
                mapOf("result" to "No categories match '$query'. Try a broader keyword.")
            } else {
                Log.d(TAG, "searchCategories('$query') → ${matches.size} results")
                mapOf("categories" to matches.joinToString("; ") { "${it.id}:${it.name}(${it.type})" })
            }
        } finally {
            sendProgress("searchCategories", false)
        }
    }

    @Tool(description = "Find a peer contact (person) by partial name. Use only for transactions involving a named person — transfers, loans, money received from someone.")
    fun searchPeers(
        @ToolParam(description = "Partial or full name of the person")
        name: String,
    ): Map<String, String> = runBlocking(Dispatchers.IO) {
        sendProgress("searchPeers", true, "Searching peers for '$name'...")
        try {
            val peers = peerRepository.getAllPeers().first()
            val q = name.trim().lowercase()
            val match = peers.firstOrNull { it.displayName.lowercase().contains(q) }
            if (match != null) {
                Log.d(TAG, "searchPeers('$name') → ${match.displayName}(${match.id})")
                mapOf("peer_id" to match.id.toString(), "peer_name" to match.displayName)
            } else {
                mapOf("result" to "No peer contact found matching '$name'")
            }
        } finally {
            sendProgress("searchPeers", false)
        }
    }

    @Tool(description = "Get the most-used category for a merchant based on the user's past transactions. Returns high-confidence result when found. Call this FIRST before searchCategories.")
    fun getMerchantCategory(
        @ToolParam(description = "Merchant or payee name as seen in the transaction text")
        merchant: String,
    ): Map<String, String> = runBlocking(Dispatchers.IO) {
        sendProgress("getMerchantCategory", true, "Looking up merchant '$merchant'...")
        try {
            val entry = merchantMemory.lookup(merchant)
            if (entry != null) {
                Log.d(TAG, "getMerchantCategory('$merchant') → ${entry.categoryName}(${entry.categoryId})")
                mapOf(
                    "category_id"   to entry.categoryId.toString(),
                    "category_name" to entry.categoryName,
                    "type_id"       to (entry.typeId ?: ""),
                    "confidence"    to "high",
                )
            } else {
                mapOf("result" to "No history found for '$merchant'. Call searchCategories to find a category.")
            }
        } finally {
            sendProgress("getMerchantCategory", false)
        }
    }

}
