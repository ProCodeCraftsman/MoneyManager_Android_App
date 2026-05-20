package com.moneymanager.data.ai

import com.moneymanager.domain.ai.CategoryEntry
import com.moneymanager.domain.ai.PromptContext
import com.moneymanager.domain.ai.TransactionType

object PromptBuilder {

    private const val MAX_PROMPT_TOKENS = 1024

    // ── Agentic builders (system instruction + user message) ─────────────────

    /**
     * Compact system instruction wired into ConversationConfig.systemInstruction.
     *
     * Instructs the model to:
     *   1. Use tool calls to resolve merchant history, categories, accounts, and peers
     *      (tools are attached by EdgeAiClient via TransactionToolSet).
     *   2. Fall back to the [narrowedCategories] list if tools aren't available
     *      (enables NanoAiClient default path which combines both into one prompt).
     *   3. Output ONLY valid JSON — no markdown, no explanation.
     *
     * Keeping the category/account lists here means NanoAiClient (no tool support)
     * still gets a fully self-contained prompt via the GenAiClient default overload.
     */
    fun buildSystemInstruction(
        context: PromptContext,
        narrowedCategories: List<CategoryEntry>,
    ): String = buildString {
        appendLine("You are a financial transaction classifier for MoneyManager (Android personal finance app).")
        appendLine("Output ONLY a valid JSON object. No markdown, no explanation, no extra text.")
        appendLine()
        appendLine("JSON schema:")
        appendLine("""{"typeId":"expense|income|transfer","categoryName":"string","accountName":"string","peerContactName":"string or null","description":"string or null"}""")
        appendLine()
        appendLine("Rules:")
        appendLine("- typeId MUST be exactly: expense, income, or transfer.")
        appendLine("- Use the exact category and account names from your tools or the lists below.")
        appendLine("- peerContactName: only for transactions involving a named person. Use null otherwise.")
        appendLine("- description: short merchant or purpose summary (max 40 chars). Use null if unclear.")
        appendLine()
        appendLine("If tools are available, follow this order:")
        appendLine("1. Call getMerchantCategory(merchant) to check past category for this merchant.")
        appendLine("2. If no history: call searchCategories(keyword) to find a matching category.")
        appendLine("3. Call getDefaultAccount() if no account is mentioned.")
        appendLine("4. Call searchPeers(name) only if a named person is involved.")
        appendLine()
        // Category list: used by NanoAiClient (no tools) AND as a hint for EdgeAiClient
        val parentMap = (context.allCategories.ifEmpty { context.top20Categories }).associateBy { it.id }
        val catLines = narrowedCategories.joinToString("\n") { cat ->
            val prefix = "  ${cat.type}: "
            if (cat.parentId != null) {
                val parentName = parentMap[cat.parentId]?.name ?: ""
                "$prefix$parentName > ${cat.name}"
            } else {
                "$prefix${cat.name}"
            }
        }
        appendLine("Available categories:")
        appendLine(catLines)
        if (context.accounts.isNotEmpty()) {
            appendLine("Available accounts: ${context.accounts.joinToString(", ") { it.name }}")
        }
        if (context.peers.isNotEmpty()) {
            appendLine("Available peers: ${context.peers.take(10).joinToString(", ") { it.name }}")
        }
    }.trimEnd()

    /**
     * Lean user message: pre-extracted certainties + raw text.
     * Pre-extracted fields (amount, typeId, date) are established by DeterministicExtractor
     * and should NOT be re-derived by the model — they are overridden post-inference anyway,
     * but telling the model saves tokens and prevents conflicting output.
     */
    fun buildUserMessage(rawText: String, pre: PreExtraction?): String = buildString {
        if (pre != null) {
            val hints = buildList {
                pre.amount?.let { add("amount=$it") }
                pre.typeId?.let { add("typeId=$it") }
                pre.merchantHint?.let { add("merchant=${it}") }
            }
            if (hints.isNotEmpty()) {
                appendLine("Pre-extracted (treat as ground truth, do not re-derive): ${hints.joinToString(", ")}")
            }
        }
        appendLine("Transaction text:")
        appendLine(rawText)
        append("Return ONLY the JSON object.")
    }

    // ── Image / vision builders ───────────────────────────────────────────────

    /**
     * System instruction for the ask-image pipeline.
     * Instructs the model to analyze a receipt image, tag per-field confidence,
     * call tools to resolve category/account, and never hallucinate missing data.
     */
    fun buildImageSystemInstruction(
        context: PromptContext,
        narrowedCategories: List<CategoryEntry>,
    ): String = buildString {
        appendLine("You are a receipt analyst for MoneyManager (Android personal finance app).")
        appendLine("Analyze the receipt image provided and extract transaction details.")
        appendLine("Output ONLY a valid JSON object. No markdown, no explanation, no extra text.")
        appendLine()
        appendLine("JSON schema:")
        appendLine("""{"typeId":"expense|income|transfer","amount":0.00,"categoryName":"string","accountName":"string","peerContactName":"string or null","description":"string or null","date":"YYYY-MM-DD or null","confidence":{"amount":"high|medium|low","typeId":"high|medium|low","date":"high|medium|low","merchant":"high|medium|low"},"needs_review":false,"flags":[]}""")
        appendLine()
        appendLine("Field rules:")
        appendLine("- typeId: receipts are usually expense. Use income only for cashback/refund receipts.")
        appendLine("- amount: use TOTAL or GRAND TOTAL — never subtotal or a single line item price.")
        appendLine("- date: extract from receipt header. Use null if not readable.")
        appendLine("- description: short merchant or purpose summary (max 40 chars). Use null if unclear.")
        appendLine("- confidence: rate each field high/medium/low based on how clearly it appears in the image.")
        appendLine("- needs_review: set true if ANY of amount, typeId, or date confidence is low.")
        appendLine("- flags: list uncertainty reasons, e.g. [\"blurry_image\", \"partial_receipt\", \"multiple_items\", \"amount_unclear\"].")
        appendLine()
        appendLine("No-hallucination rule: NEVER invent or guess values.")
        appendLine("If a field is unclear, set it null and mark its confidence as low.")
        appendLine()
        appendLine("Tool calling order:")
        appendLine("1. Call getMerchantCategory(merchant) — check merchant history first.")
        appendLine("2. If no history: call searchCategories(keyword) — find matching category.")
        appendLine("3. Call getDefaultAccount() — if no account is printed on the receipt.")
        appendLine("4. Call searchPeers(name) — only if a named person is involved.")
        appendLine()
        val parentMap = (context.allCategories.ifEmpty { context.top20Categories }).associateBy { it.id }
        val catLines = narrowedCategories.joinToString("\n") { cat ->
            val prefix = "  ${cat.type}: "
            if (cat.parentId != null) {
                val parentName = parentMap[cat.parentId]?.name ?: ""
                "$prefix$parentName > ${cat.name}"
            } else {
                "$prefix${cat.name}"
            }
        }
        appendLine("Available categories:")
        appendLine(catLines)
        if (context.accounts.isNotEmpty()) {
            appendLine("Available accounts: ${context.accounts.joinToString(", ") { it.name }}")
        }
        if (context.peers.isNotEmpty()) {
            appendLine("Available peers: ${context.peers.take(10).joinToString(", ") { it.name }}")
        }
    }.trimEnd()

    /** Minimal user message for the ask-image turn — the image bytes carry the content. */
    fun buildImageUserMessage(): String =
        "Analyze this receipt image and extract the transaction details. Return ONLY the JSON object."

    fun build(rawText: String, context: PromptContext): String {
        val typeHints = TransactionType.entries.joinToString("\n") { type ->
            "${type.id}: ${type.promptHint}"
        }

        var categoriesSection = ""
        var accountsSection = ""
        var peersSection = ""
        var tagsSection = ""

        val builder = StringBuilder()
        builder.appendLine("You are a financial transaction extractor. Return ONLY a valid JSON object. No explanation.")
        builder.appendLine("Transaction types: $typeHints")
        builder.appendLine("Known categories (type - name, indent shows subcategory):")
        val parentMap = context.allCategories.ifEmpty { context.top20Categories }.associateBy { it.id }
        for (entry in context.top20Categories) {
            val safeType = entry.type.replace("\"", "'").replace("\n", " ").trim()
            val safeName = entry.name.replace("\"", "'").replace("\n", " ").trim()
            if (entry.parentId != null) {
                val parentName = parentMap[entry.parentId]?.name?.replace("\"", "'")?.replace("\n", " ")?.trim() ?: ""
                categoriesSection += "$safeType - $parentName > $safeName\n"
            } else {
                categoriesSection += "$safeType - $safeName\n"
            }
        }
        builder.append(categoriesSection)
        builder.appendLine("Known accounts:")
        for (acct in context.accounts) {
            val safeName = acct.name.replace("\"", "'").replace("\n", " ").trim()
            accountsSection += "$safeName, "
        }
        builder.appendLine(accountsSection.removeSuffix(", "))
        builder.appendLine("Known peers:")
        for (peer in context.peers) {
            val safeName = peer.name.replace("\"", "'").replace("\n", " ").trim()
            peersSection += "$safeName, "
        }
        builder.appendLine(peersSection.removeSuffix(", "))
        builder.appendLine("Known tags:")
        for (tag in context.tags) {
            val safeName = tag.name.replace("\"", "'").replace("\n", " ").trim()
            tagsSection += "$safeName, "
        }
        builder.appendLine(tagsSection.removeSuffix(", "))
        builder.appendLine("Text to parse: $rawText")
        builder.appendLine("JSON schema fields: typeId (string), amount (double), categoryName (string), accountName (string), peerContactName (string), description (string), note (string)")
        builder.append("Return ONLY the JSON object.")

        val fullPrompt = builder.toString()
        val estimatedTokens = countApproxTokens(fullPrompt)
        if (estimatedTokens <= MAX_PROMPT_TOKENS) return fullPrompt

        val maxChars = MAX_PROMPT_TOKENS * 4
        return fullPrompt.take(maxChars)
            .substringBeforeLast("\n")
            .plus("\nReturn ONLY the JSON object.")
    }

    /**
     * Focused prompt used when [PreExtraction] has already pinned amount, type, and date.
     * Tells the AI what is known so it only classifies category + resolves peer/account/description.
     * ~40% fewer tokens than [build] → faster inference, less hallucination surface.
     */
    fun buildFocused(
        rawText: String,
        context: PromptContext,
        pre: PreExtraction,
        narrowedCategories: List<com.moneymanager.domain.ai.CategoryEntry> = context.top20Categories,
    ): String {
        val builder = StringBuilder()
        builder.appendLine("You are a financial transaction classifier. Return ONLY a valid JSON object.")

        // Tell the model what deterministic extraction already established
        val knownParts = buildList {
            pre.amount?.let { add("amount=${it}") }
            pre.typeId?.let { add("typeId=${it}") }
            pre.merchantHint?.let { add("merchant=${it}") }
        }
        if (knownParts.isNotEmpty()) {
            builder.appendLine("Already extracted (do not re-derive): ${knownParts.joinToString(", ")}")
        }

        // Type hint only when not yet determined
        if (pre.typeId == null) {
            val typeHints = TransactionType.entries.joinToString(", ") { "${it.id}: ${it.promptHint}" }
            builder.appendLine("Transaction types: $typeHints")
        }

        builder.appendLine("Choose category from (type - name):")
        val parentMap = context.allCategories.ifEmpty { context.top20Categories }.associateBy { it.id }
        for (entry in narrowedCategories) {
            val safeType = entry.type.replace("\"", "'").trim()
            val safeName = entry.name.replace("\"", "'").trim()
            if (entry.parentId != null) {
                val parentName = parentMap[entry.parentId]?.name?.replace("\"", "'")?.trim() ?: ""
                builder.appendLine("$safeType - $parentName > $safeName")
            } else {
                builder.appendLine("$safeType - $safeName")
            }
        }

        if (context.accounts.isNotEmpty()) {
            builder.appendLine("Known accounts: ${context.accounts.joinToString(", ") { it.name.replace("\"", "'") }}")
        }
        if (context.peers.isNotEmpty()) {
            builder.appendLine("Known peers: ${context.peers.joinToString(", ") { it.name.replace("\"", "'") }}")
        }

        builder.appendLine("Text: $rawText")

        val schemaFields = buildList {
            if (pre.typeId == null) add("typeId (string)")
            add("categoryName (string)")
            add("accountName (string)")
            add("peerContactName (string)")
            add("description (string)")
        }.joinToString(", ")

        builder.appendLine("JSON fields: $schemaFields")
        builder.append("Return ONLY the JSON object.")

        val full = builder.toString()
        return if (countApproxTokens(full) <= MAX_PROMPT_TOKENS) full
        else full.take(MAX_PROMPT_TOKENS * 4).substringBeforeLast("\n").plus("\nReturn ONLY the JSON object.")
    }

    private fun countApproxTokens(text: String): Int {
        if (text.isEmpty()) return 0
        val words = text.split(Regex("\\s+")).size
        val punctuation = text.count { it in setOf('.', ',', '!', '?', ';', ':') }
        return (words * 1.3 + punctuation * 0.5).toInt().coerceAtLeast(1)
    }
}
