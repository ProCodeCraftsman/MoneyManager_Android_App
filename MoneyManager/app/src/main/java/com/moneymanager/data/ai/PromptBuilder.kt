package com.moneymanager.data.ai

import com.moneymanager.domain.ai.PromptContext
import com.moneymanager.domain.ai.TransactionType

object PromptBuilder {

    fun build(rawText: String, context: PromptContext): String {
        val sanitizedCategories = context.top20Categories.take(20).joinToString("\n") { entry ->
            val safeType = entry.type.replace("\"", "'").replace("\n", " ").trim()
            val safeName = entry.name.replace("\"", "'").replace("\n", " ").trim()
            "$safeType - $safeName"
        }
        val sanitizedAccounts = context.accounts.joinToString(", ") {
            it.name.replace("\"", "'").replace("\n", " ").trim()
        }
        val sanitizedPeers = context.peers.joinToString(", ") {
            it.name.replace("\"", "'").replace("\n", " ").trim()
        }
        val sanitizedTags = context.tags.joinToString(", ") {
            it.name.replace("\"", "'").replace("\n", " ").trim()
        }
        val typeHints = TransactionType.entries.joinToString("\n") { type ->
            "${type.id}: ${type.promptHint}"
        }

        return buildString {
            appendLine("You are a financial transaction extractor. Return ONLY a valid JSON object. No explanation.")
            appendLine("Transaction types: $typeHints")
            appendLine("Known categories (type - name):")
            appendLine(sanitizedCategories)
            appendLine("Known accounts: $sanitizedAccounts")
            appendLine("Known peers: $sanitizedPeers")
            appendLine("Known tags: $sanitizedTags")
            appendLine("Text to parse: $rawText")
            appendLine("JSON schema fields: typeId (string), amount (double), categoryName (string), accountName (string), peerContactName (string), description (string), note (string)")
            append("Return ONLY the JSON object.")
        }
    }
}
