package com.moneymanager.domain.transaction

import com.moneymanager.app.ui.dialogs.SplitRowData
import com.moneymanager.data.entity.TransactionEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Builds split-child [TransactionEntity] rows from a set of split rows.
 *
 * Shared by the manual entry save path ([com.moneymanager.app.ui.dialogs.AddEditTransactionDialog])
 * and [com.moneymanager.data.worker.RecurringGenerationWorker] so a recurring split template
 * generates children the same way a manually-entered split transaction does.
 */
object SplitTransactionFactory {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class SplitRowTemplate(
        val categoryId: Long? = null,
        val subCategoryId: Long? = null,
        val description: String = "",
        val amount: String = "",
    )

    fun buildSplitChildren(
        parentId: Long,
        accountId: Long,
        type: String,
        date: Long,
        rows: List<SplitRowData>,
    ): List<TransactionEntity> {
        return rows.mapNotNull { row ->
            val rowAmt = row.amount.toDoubleOrNull() ?: return@mapNotNull null
            if (rowAmt <= 0) return@mapNotNull null
            TransactionEntity(
                accountId = accountId,
                type = type,
                amount = rowAmt,
                categoryId = row.categoryId,
                subCategoryId = row.subCategoryId,
                tagIds = "",
                date = date,
                note = row.description,
                description = row.description,
                isSplitChild = true,
                isTransfer = type == "transfer",
                parentTransactionId = parentId
            )
        }
    }

    /** Encodes split rows into the JSON form stored in `RecurringEntity.splitData`. */
    fun encodeSplitTemplate(rows: List<SplitRowData>): String {
        return json.encodeToString(
            SplitRowTemplate.serializer().let { kotlinx.serialization.builtins.ListSerializer(it) },
            rows.map { SplitRowTemplate(it.categoryId, it.subCategoryId, it.description, it.amount) }
        )
    }

    /** Decodes a `RecurringEntity.splitData` JSON string back into editable [SplitRowData] rows. */
    fun decodeSplitTemplate(splitData: String?): List<SplitRowData> {
        if (splitData.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(SplitRowTemplate.serializer()),
                splitData
            ).mapIndexed { index, template ->
                SplitRowData(
                    localId = index,
                    categoryId = template.categoryId,
                    subCategoryId = template.subCategoryId,
                    description = template.description,
                    amount = template.amount,
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Builds split-child rows directly from a stored `RecurringEntity.splitData` template. */
    fun buildSplitChildrenFromTemplate(
        parentId: Long,
        accountId: Long,
        type: String,
        date: Long,
        splitData: String?,
    ): List<TransactionEntity> = buildSplitChildren(parentId, accountId, type, date, decodeSplitTemplate(splitData))
}
