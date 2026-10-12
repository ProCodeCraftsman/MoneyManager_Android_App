package com.moneymanager.data.audit

import com.moneymanager.domain.audit.AuditCheck
import com.moneymanager.domain.audit.AuditFinding
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Round-trip for the findings the inbox shows. The worker writes them after each run and the inbox reads them back,
 * so opening the inbox never has to recompute. Unreadable JSON reads as no findings, never as a crash.
 */
internal object AuditStore {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(StoredFinding.serializer())

    fun encode(findings: List<AuditFinding>): String =
        json.encodeToString(serializer, findings.map { it.toStored() })

    fun decode(raw: String?): List<AuditFinding> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(serializer, raw).mapNotNull { it.toFinding() } }
            .getOrDefault(emptyList())
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    private data class StoredFinding(
        val check: String,
        val fingerprint: String,
        val score: Double,
        val txnId: Long? = null,
        val otherTxnId: Long? = null,
        val accountId: Long? = null,
        val suggestedCategoryId: Long? = null,
        val suggestedAccountId: Long? = null,
        val expected: Double? = null,
    )

    private fun AuditFinding.toStored() = StoredFinding(
        check = check.name,
        fingerprint = fingerprint,
        score = score,
        txnId = txnId,
        otherTxnId = otherTxnId,
        accountId = accountId,
        suggestedCategoryId = suggestedCategoryId,
        suggestedAccountId = suggestedAccountId,
        expected = expected,
    )

    /** An unknown check name (a newer build wrote it) is dropped rather than shown with no meaning. */
    private fun StoredFinding.toFinding(): AuditFinding? {
        val parsed = AuditCheck.entries.firstOrNull { it.name == check } ?: return null
        return AuditFinding(
            check = parsed,
            fingerprint = fingerprint,
            score = score,
            txnId = txnId,
            otherTxnId = otherTxnId,
            accountId = accountId,
            suggestedCategoryId = suggestedCategoryId,
            suggestedAccountId = suggestedAccountId,
            expected = expected,
        )
    }
}
