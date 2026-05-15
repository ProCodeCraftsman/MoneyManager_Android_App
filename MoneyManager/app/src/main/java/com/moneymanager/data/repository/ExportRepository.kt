package com.moneymanager.data.repository

import android.content.Context
import android.net.Uri
import com.moneymanager.data.dao.*
import com.moneymanager.data.entity.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

data class ExportResult(
    val success: Boolean,
    val message: String,
    val filePath: String? = null,
)

data class ImportResult(
    val success: Boolean,
    val message: String,
    val accountsImported: Int = 0,
    val transactionsImported: Int = 0,
    val categoriesImported: Int = 0,
    val budgetsImported: Int = 0,
    val goalsImported: Int = 0,
    val tagsImported: Int = 0,
    val peersImported: Int = 0,
    val recurringImported: Int = 0,
    val totalProcessed: Int = 0,
)

@Singleton
@Suppress("unused")
class ExportRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val goalDao: GoalDao,
    private val tagDao: TagDao,
    private val peerContactDao: com.moneymanager.data.dao.PeerContactDao,
    private val recurringDao: RecurringDao,
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    suspend fun exportToJson(uri: Uri): ExportResult = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
            
            json.put("appVersion", "1.0.0")
            json.put("dataVersion", 1)
            json.put("exportedAt", System.currentTimeMillis())
            
            json.put("accounts", exportAccounts())
            json.put("transactions", exportTransactions())
            json.put("categories", exportCategories())
            json.put("budgets", exportBudgets())
            json.put("goals", exportGoals())
            json.put("tags", exportTags())
            json.put("peers", exportPeers())
            json.put("recurring", exportRecurring())
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(json.toString(2))
                }
            }
            
            ExportResult(success = true, message = "Data exported successfully")
        } catch (e: Exception) {
            ExportResult(success = false, message = "Export failed: ${e.message}")
        }
    }

    suspend fun exportToCsv(uri: Uri, type: ExportType): ExportResult = withContext(Dispatchers.IO) {
        try {
            val csv = when (type) {
                ExportType.TRANSACTIONS -> exportTransactionsCsv()
                ExportType.ACCOUNTS -> exportAccountsCsv()
                ExportType.CATEGORIES -> exportCategoriesCsv()
                ExportType.BUDGETS -> exportBudgetsCsv()
                ExportType.GOALS -> exportGoalsCsv()
                ExportType.ALL -> exportAllCsv()
                ExportType.TAGS -> exportTagsCsv()
                ExportType.PEERS -> exportPeersCsv()
                ExportType.RECURRING -> exportRecurringCsv()
            }
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(csv)
                }
            }
            
            ExportResult(success = true, message = "${type.name} exported successfully")
        } catch (e: Exception) {
            ExportResult(success = false, message = "Export failed: ${e.message}")
        }
    }

    suspend fun importFromJson(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            } ?: return@withContext ImportResult(success = false, message = "Could not read file")

            val jsonObject = JSONObject(json)
            
            var accountsImported = 0
            var transactionsImported = 0
            var categoriesImported = 0
            var budgetsImported = 0
            var goalsImported = 0
            var tagsImported = 0
            var peersImported = 0
            var recurringImported = 0

            if (jsonObject.has("accounts")) {
                accountsImported = importAccounts(jsonObject.getJSONArray("accounts"))
            }
            if (jsonObject.has("categories")) {
                categoriesImported = importCategories(jsonObject.getJSONArray("categories"))
            }
            if (jsonObject.has("tags")) {
                tagsImported = importTags(jsonObject.getJSONArray("tags"))
            }
            if (jsonObject.has("transactions")) {
                transactionsImported = importTransactions(jsonObject.getJSONArray("transactions"))
            }
            if (jsonObject.has("budgets")) {
                budgetsImported = importBudgets(jsonObject.getJSONArray("budgets"))
            }
            if (jsonObject.has("goals")) {
                goalsImported = importGoals(jsonObject.getJSONArray("goals"))
            }
            if (jsonObject.has("peers")) {
                peersImported = importPeers(jsonObject.getJSONArray("peers"))
            }
            if (jsonObject.has("recurring")) {
                recurringImported = importRecurring(jsonObject.getJSONArray("recurring"))
            }

            ImportResult(
                success = true,
                message = "Import completed: $accountsImported accounts, $transactionsImported transactions, $peersImported peers, etc.",
                accountsImported = accountsImported,
                transactionsImported = transactionsImported,
                categoriesImported = categoriesImported,
                budgetsImported = budgetsImported,
                goalsImported = goalsImported,
                tagsImported = tagsImported,
                peersImported = peersImported,
                recurringImported = recurringImported,
            )
        } catch (e: Exception) {
            ImportResult(success = false, message = "Import failed: ${e.message}")
        }
    }

    suspend fun importFromCsv(uri: Uri, type: ExportType): ImportResult = withContext(Dispatchers.IO) {
        try {
            val csv = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            } ?: return@withContext ImportResult(success = false, message = "Could not read file")

            when (type) {
                ExportType.TRANSACTIONS -> {
                    val count = importTransactionsFromCsv(csv)
                    val total = csv.lines().size - 1
                    ImportResult(success = true, message = "$count transactions imported (total: $total)", transactionsImported = count, totalProcessed = total)
                }
                ExportType.ACCOUNTS -> {
                    val count = importAccountsFromCsv(csv)
                    val total = csv.lines().size - 1
                    ImportResult(success = true, message = "$count accounts imported (total: $total)", accountsImported = count, totalProcessed = total)
                }
                ExportType.CATEGORIES -> {
                    val count = importCategoriesFromCsv(csv)
                    val total = csv.lines().size - 1
                    ImportResult(success = true, message = "$count categories imported (total: $total)", categoriesImported = count, totalProcessed = total)
                }
                ExportType.TAGS -> {
                    val count = importTagsFromCsv(csv)
                    val total = csv.lines().size - 1
                    ImportResult(success = true, message = "$count tags imported (total: $total)", tagsImported = count, totalProcessed = total)
                }
                ExportType.PEERS -> {
                    val count = importPeersFromCsv(csv)
                    val total = csv.lines().size - 1
                    ImportResult(success = true, message = "$count peers imported (total: $total)", peersImported = count, totalProcessed = total)
                }
                ExportType.BUDGETS -> {
                    val count = importBudgetsFromCsv(csv)
                    val total = csv.lines().size - 1
                    ImportResult(success = true, message = "$count budgets imported (total: $total)", budgetsImported = count, totalProcessed = total)
                }
                ExportType.GOALS -> {
                    val count = importGoalsFromCsv(csv)
                    val total = csv.lines().size - 1
                    ImportResult(success = true, message = "$count goals imported (total: $total)", goalsImported = count, totalProcessed = total)
                }
                ExportType.RECURRING -> {
                    val count = importRecurringFromCsv(csv)
                    val total = csv.lines().size - 1
                    ImportResult(success = true, message = "$count recurring items imported (total: $total)", recurringImported = count, totalProcessed = total)
                }
                ExportType.ALL -> importAllFromCsv(csv)
                else -> ImportResult(success = false, message = "CSV import not supported for ${type.name}")
            }
        } catch (e: Exception) {
            ImportResult(success = false, message = "Import failed: ${e.message}")
        }
    }

    private suspend fun exportAccounts(): JSONArray {
        val accounts = accountDao.getAllAccounts().first()
        val array = JSONArray()
        accounts.forEach { account ->
            val obj = JSONObject()
            obj.put("id", account.id)
            obj.put("name", account.name)
            obj.put("type", account.type)
            obj.put("initialBalance", account.initialBalance)
            obj.put("balance", account.balance)
            obj.put("currency", account.currency)
            obj.put("emoji", account.emoji)
            obj.put("color", account.color)
            obj.put("peerContactId", account.peerContactId)
            obj.put("createdAt", account.createdAt)
            obj.put("updatedAt", account.updatedAt)
            array.put(obj)
        }
        return array
    }

    private suspend fun exportTransactions(): JSONArray {
        val transactions = transactionDao.getAllTransactions().first()
        val array = JSONArray()
        transactions.forEach { tx ->
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("accountId", tx.accountId)
            obj.put("type", tx.type)
            obj.put("amount", tx.amount)
            obj.put("categoryId", tx.categoryId)
            obj.put("tagIds", tx.tagIds)
            obj.put("date", tx.date)
            obj.put("note", tx.note)
            obj.put("isRecurring", tx.isRecurring)
            // Added missing TransactionEntity fields
            obj.put("subCategoryId", tx.subCategoryId)
            obj.put("goalId", tx.goalId)
            obj.put("peerContactId", tx.peerContactId)
            obj.put("description", tx.description)
            obj.put("receiptPath", tx.receiptPath)
            obj.put("recurringId", tx.recurringId)
            obj.put("splitData", tx.splitData)
            obj.put("investmentPlatform", tx.investmentPlatform)
            obj.put("expectedReturnDate", tx.expectedReturnDate)
            obj.put("createdAt", tx.createdAt)
            obj.put("isSplitParent", tx.isSplitParent)
            obj.put("isSplitChild", tx.isSplitChild)
            obj.put("parentTransactionId", tx.parentTransactionId)
            obj.put("isTransfer", tx.isTransfer)
            obj.put("toAccountId", tx.toAccountId)
            array.put(obj)
        }
        return array
    }

    private suspend fun exportCategories(): JSONArray {
        val categories = categoryDao.getAllCategoriesWithArchived().first()
        val array = JSONArray()
        categories.forEach { cat ->
            val obj = JSONObject()
            obj.put("id", cat.id)
            obj.put("name", cat.name)
            obj.put("emoji", cat.emoji)
            obj.put("iconType", cat.iconType)
            obj.put("type", cat.type)
            obj.put("parentId", cat.parentId)
            obj.put("color", cat.color)
            obj.put("isCustom", cat.isCustom)
            obj.put("isArchived", cat.isArchived)
            obj.put("createdAt", cat.createdAt)
            array.put(obj)
        }
        return array
    }

    private suspend fun exportBudgets(): JSONArray {
        val budgets = budgetDao.getAllBudgets().first()
        val array = JSONArray()
        budgets.forEach { budget ->
            val obj = JSONObject()
            obj.put("id", budget.id)
            obj.put("categoryId", budget.categoryId)
            obj.put("amount", budget.amount)
            obj.put("month", budget.month)
            obj.put("isSavingsTarget", budget.isSavingsTarget)
            obj.put("createdAt", budget.createdAt)
            array.put(obj)
        }
        return array
    }

    private suspend fun exportGoals(): JSONArray {
        val goals = goalDao.getAllGoals().first()
        val array = JSONArray()
        goals.forEach { goal ->
            val obj = JSONObject()
            obj.put("id", goal.id)
            obj.put("name", goal.name)
            obj.put("emoji", goal.emoji)
            obj.put("iconType", goal.iconType)
            obj.put("targetAmount", goal.targetAmount)
            obj.put("currentAmount", goal.currentAmount)
            obj.put("deadline", goal.deadline)
            obj.put("createdAt", goal.createdAt)
            obj.put("isCompleted", goal.isCompleted)
            array.put(obj)
        }
        return array
    }

    private suspend fun exportTags(): JSONArray {
        val tags = tagDao.getAllTags().first()
        val array = JSONArray()
        tags.forEach { tag ->
            val obj = JSONObject()
            obj.put("id", tag.id)
            obj.put("name", tag.name)
            obj.put("color", tag.color)
            array.put(obj)
        }
        return array
    }

    private suspend fun exportPeers(): JSONArray {
        val peers = peerContactDao.getAllPeers().first()
        val array = JSONArray()
        peers.forEach { peer ->
            val obj = JSONObject()
            obj.put("id", peer.id)
            obj.put("displayName", peer.displayName)
            obj.put("phoneNumber", peer.phoneNumber)
            obj.put("email", peer.email)
            obj.put("description", peer.description)
            obj.put("photoUri", peer.photoUri)
            obj.put("totalGiven", peer.totalGiven)
            obj.put("totalReceived", peer.totalReceived)
            obj.put("createdAt", peer.createdAt)
            obj.put("updatedAt", peer.updatedAt)
            array.put(obj)
        }
        return array
    }

    private suspend fun exportRecurring(): JSONArray {
        val recurring = recurringDao.getAllRecurring().first()
        val array = JSONArray()
        recurring.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("accountId", item.accountId)
            obj.put("type", item.type)
            obj.put("amount", item.amount)
            obj.put("categoryId", item.categoryId)
            obj.put("subCategoryId", item.subCategoryId)
            obj.put("goalId", item.goalId)
            obj.put("note", item.note)
            obj.put("frequency", item.frequency)
            obj.put("startDate", item.startDate)
            obj.put("nextDate", item.nextDate)
            obj.put("isActive", item.isActive)
            obj.put("reminderEnabled", item.reminderEnabled)
            obj.put("reminderDays", item.reminderDays)
            obj.put("investmentApp", item.investmentApp)
            obj.put("createdAt", item.createdAt)
            array.put(obj)
        }
        return array
    }

    private suspend fun exportTransactionsCsv(): String {
        val transactions = transactionDao.getAllTransactions().first()
        val accounts = accountDao.getAllAccounts().first().associateBy { it.id }
        val categories = categoryDao.getAllCategories().first().associateBy { it.id }
        
        val sb = StringBuilder()
        // Updated header with all TransactionEntity fields
        sb.appendLine("id,account_id,category_id,sub_category_id,goal_id,peer_contact_id,tag_ids,date,amount,type,note,description,receipt_path,recurring_id,split_data,investment_platform,expected_return_date,created_at,is_recurring,is_split_parent,is_split_child,parent_transaction_id,is_transfer,to_account_id")
        
        transactions.forEach { tx ->
            val accountName = accounts[tx.accountId]?.name ?: ""
            val categoryName = categories[tx.categoryId]?.name ?: ""
            
            // Format timestamp fields
            val expectedReturnDateStr = tx.expectedReturnDate?.let { dateFormat.format(Date(it)) } ?: ""
            val createdAtStr = dateFormat.format(Date(tx.createdAt))
            
            sb.appendLine(
                "${tx.id}," +
                "${tx.accountId}," +
                "${tx.categoryId ?: ""}," +
                "${tx.subCategoryId ?: ""}," +
                "${tx.goalId ?: ""}," +
                "${tx.peerContactId ?: ""}," +
                "\"${tx.tagIds}\"," +
                "${dateFormat.format(Date(tx.date))}," +
                "${tx.amount}," +
                "${tx.type}," +
                "\"${tx.note.replace("\"", "\"\"")}\"," +
                "\"${tx.description.replace("\"", "\"\"")}\"," +
                "\"${tx.receiptPath ?: ""}\"," +
                "${tx.recurringId ?: ""}," +
                "\"${tx.splitData ?: ""}\"," +
                "\"${tx.investmentPlatform ?: ""}\"," +
                "\"$expectedReturnDateStr\"," +
                "\"$createdAtStr\"," +
                "${tx.isRecurring}," +
                "${tx.isSplitParent}," +
                "${tx.isSplitChild}," +
                "${tx.parentTransactionId ?: ""}," +
                "${tx.isTransfer}," +
                "${tx.toAccountId ?: ""}"
            )
        }
        return sb.toString()
    }

    private suspend fun exportAccountsCsv(): String {
        val accounts = accountDao.getAllAccounts().first()
        val sb = StringBuilder()
        sb.appendLine("name,type,balance,currency,color")
        accounts.forEach { acc ->
            sb.appendLine("${acc.name},${acc.type},${acc.balance},${acc.currency},${acc.color}")
        }
        return sb.toString()
    }

    private suspend fun exportCategoriesCsv(): String {
        val categories = categoryDao.getAllCategoriesWithArchived().first()
        val sb = StringBuilder()
        sb.appendLine("name,type,emoji,icon_type,parent_id,is_custom")
        categories.forEach { cat ->
            sb.appendLine("${cat.name},${cat.type},${cat.emoji},${cat.iconType},${cat.parentId ?: ""},${cat.isCustom}")
        }
        return sb.toString()
    }

    private suspend fun exportBudgetsCsv(): String {
        val budgets = budgetDao.getAllBudgets().first()
        val sb = StringBuilder()
        sb.appendLine("category_id,amount,month,is_savings_target")
        budgets.forEach { budget ->
            sb.appendLine("${budget.categoryId},${budget.amount},${budget.month},${budget.isSavingsTarget}")
        }
        return sb.toString()
    }

    private suspend fun exportGoalsCsv(): String {
        val goals = goalDao.getAllGoals().first()
        val sb = StringBuilder()
        sb.appendLine("name,emoji,icon_type,target_amount,current_amount,deadline,is_completed")
        goals.forEach { goal ->
            sb.appendLine("${goal.name},${goal.emoji},${goal.iconType},${goal.targetAmount},${goal.currentAmount},${goal.deadline?.let { dateFormat.format(Date(it)) } ?: ""},${goal.isCompleted}")
        }
        return sb.toString()
    }

    private suspend fun exportPeersCsv(): String {
        val peers = peerContactDao.getAllPeers().first()
        return buildString {
            appendLine("displayName,phoneNumber,email,description,totalGiven,totalReceived")
            peers.forEach { peer ->
                appendLine("${peer.displayName},${peer.phoneNumber},${peer.email},${peer.description},${peer.totalGiven},${peer.totalReceived}")
            }
        }
    }

    private suspend fun exportRecurringCsv(): String {
        val recurring = recurringDao.getAllRecurring().first()
        val sb = StringBuilder()
        sb.appendLine("account_id,type,amount,category_id,sub_category_id,goal_id,note,frequency,start_date,next_date,is_active,reminder_enabled,reminder_days,investment_app")
        recurring.forEach { item ->
            val startDateStr = dateFormat.format(Date(item.startDate))
            val nextDateStr = dateFormat.format(Date(item.nextDate))
            sb.appendLine("${item.accountId},${item.type},${item.amount},${item.categoryId ?: ""},${item.subCategoryId ?: ""},${item.goalId ?: ""},\"${item.note.replace("\"", "\"\"")}\",${item.frequency},$startDateStr,$nextDateStr,${item.isActive},${item.reminderEnabled},${item.reminderDays},${item.investmentApp ?: ""}")
        }
        return sb.toString()
    }

    private suspend fun exportAllCsv(): String {
        return buildString {
            appendLine("# ACCOUNTS")
            appendLine(exportAccountsCsv())
            appendLine()
            appendLine("# CATEGORIES")
            appendLine(exportCategoriesCsv())
            appendLine()
            appendLine("# TRANSACTIONS")
            appendLine(generateCsvHeader())
            appendLine(generateCsvData())
            appendLine()
            appendLine("# BUDGETS")
            appendLine(exportBudgetsCsv())
            appendLine()
            appendLine("# GOALS")
            appendLine(exportGoalsCsv())
            appendLine()
            appendLine("# TAGS")
            appendLine(exportTagsCsv())
            appendLine()
            appendLine("# PEERS")
            appendLine(exportPeersCsv())
            appendLine()
            appendLine("# RECURRING")
            appendLine(exportRecurringCsv())
        }
    }
    
    private fun generateCsvHeader(): String = "id,account_id,category_id,sub_category_id,goal_id,peer_contact_id,tag_ids,date,amount,type,note,description,receipt_path,recurring_id,split_data,investment_platform,expected_return_date,created_at,is_recurring,is_split_parent,is_split_child,parent_transaction_id,is_transfer,to_account_id"
    
    private suspend fun generateCsvData(): String {
        val transactions = transactionDao.getAllTransactions().first()
        val accounts = accountDao.getAllAccounts().first().associateBy { it.id }
        val categories = categoryDao.getAllCategories().first().associateBy { it.id }
        
        return buildString {
            transactions.forEach { tx ->
                val expectedReturnDateStr = tx.expectedReturnDate?.let { dateFormat.format(Date(it)) } ?: ""
                val createdAtStr = dateFormat.format(Date(tx.createdAt))

                appendLine(
                    "${tx.id}," +
                    "${tx.accountId}," +
                    "${tx.categoryId ?: ""}," +
                    "${tx.subCategoryId ?: ""}," +
                    "${tx.goalId ?: ""}," +
                    "${tx.peerContactId ?: ""}," +
                    "\"${tx.tagIds}\"," +
                    "${dateFormat.format(Date(tx.date))}," +
                    "${tx.amount}," +
                    "${tx.type}," +
                    "\"${tx.note.replace("\"", "\"\"")}\"," +
                    "\"${tx.description.replace("\"", "\"\"")}\"," +
                    "\"${tx.receiptPath ?: ""}\"," +
                    "${tx.recurringId ?: ""}," +
                    "\"${tx.splitData ?: ""}\"," +
                    "\"${tx.investmentPlatform ?: ""}\"," +
                    "\"$expectedReturnDateStr\"," +
                    "\"$createdAtStr\"," +
                    "${tx.isRecurring}," +
                    "${tx.isSplitParent}," +
                    "${tx.isSplitChild}," +
                    "${tx.parentTransactionId ?: ""}," +
                    "${tx.isTransfer}," +
                    "${tx.toAccountId ?: ""}"
                )
            }
        }
    }

    private suspend fun exportTagsCsv(): String {
        val tags = tagDao.getAllTags().first()
        val sb = StringBuilder()
        sb.appendLine("id,name,color")
        tags.forEach { tag ->
            sb.appendLine("${tag.id},${tag.name},${tag.color}")
        }
        return sb.toString()
    }

    fun getStorageUsedKb(): Long {
        val dbFile = context.getDatabasePath("MoneyManager.db")
        return if (dbFile.exists()) dbFile.length() / 1024 else 0
    }

    private suspend fun importAccounts(array: JSONArray): Int {
        var count = 0
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val account = AccountEntity(
                id = obj.optLong("id", 0),
                name = obj.getString("name"),
                type = obj.getString("type"),
                initialBalance = obj.optDouble("initialBalance", 0.0),
                balance = obj.getDouble("balance"),
                currency = obj.optString("currency", "INR"),
                emoji = obj.optString("emoji", "🏦"),
                color = obj.optString("color", "#2a6049"),
                peerContactId = if (obj.has("peerContactId") && !obj.isNull("peerContactId")) obj.getLong("peerContactId") else null,
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
            )
            accountDao.insertAccount(account)
            count++
        }
        return count
    }

    private suspend fun importTransactions(array: JSONArray): Int {
        var count = 0
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val transaction = TransactionEntity(
                id = obj.optLong("id", 0),
                accountId = obj.getLong("accountId"),
                type = obj.getString("type"),
                amount = obj.getDouble("amount"),
                categoryId = if (obj.has("categoryId") && !obj.isNull("categoryId")) obj.getLong("categoryId") else null,
                subCategoryId = if (obj.has("subCategoryId") && !obj.isNull("subCategoryId")) obj.getLong("subCategoryId") else null,
                goalId = if (obj.has("goalId") && !obj.isNull("goalId")) obj.getLong("goalId") else null,
                peerContactId = if (obj.has("peerContactId") && !obj.isNull("peerContactId")) obj.getLong("peerContactId") else null,
                tagIds = obj.optString("tagIds", ""),
                date = obj.optLong("date", System.currentTimeMillis()),
                description = obj.optString("description", ""),
                note = obj.optString("note", ""),
                receiptPath = if (obj.has("receiptPath") && !obj.isNull("receiptPath")) obj.getString("receiptPath") else null,
                isRecurring = obj.optBoolean("isRecurring", false),
                recurringId = if (obj.has("recurringId") && !obj.isNull("recurringId")) obj.getLong("recurringId") else null,
                splitData = if (obj.has("splitData") && !obj.isNull("splitData")) obj.getString("splitData") else null,
                isSplitParent = obj.optBoolean("isSplitParent", false),
                isSplitChild = obj.optBoolean("isSplitChild", false),
                parentTransactionId = if (obj.has("parentTransactionId") && !obj.isNull("parentTransactionId")) obj.getLong("parentTransactionId") else null,
                isTransfer = obj.optBoolean("isTransfer", false),
                toAccountId = if (obj.has("toAccountId") && !obj.isNull("toAccountId")) obj.getLong("toAccountId") else null,
                investmentPlatform = if (obj.has("investmentPlatform") && !obj.isNull("investmentPlatform")) obj.getString("investmentPlatform") else null,
                expectedReturnDate = if (obj.has("expectedReturnDate") && !obj.isNull("expectedReturnDate")) obj.getLong("expectedReturnDate") else null,
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            )
            transactionDao.insertTransaction(transaction)
            count++
        }
        return count
    }

    private suspend fun importCategories(array: JSONArray): Int {
        var count = 0
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val category = CategoryEntity(
                id = obj.optLong("id", 0),
                name = obj.getString("name"),
                emoji = obj.optString("emoji", "📁"),
                color = obj.optString("color", "#90A4AE"),
                type = obj.getString("type"),
                parentId = if (obj.has("parentId") && !obj.isNull("parentId")) obj.getLong("parentId") else null,
                isCustom = obj.optBoolean("isCustom", false),
                isArchived = obj.optBoolean("isArchived", false),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            )
            categoryDao.insertCategory(category)
            count++
        }
        return count
    }

    private suspend fun importBudgets(array: JSONArray): Int {
        var count = 0
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val budget = BudgetEntity(
                id = obj.optLong("id", 0),
                categoryId = obj.getLong("categoryId"),
                amount = obj.getDouble("amount"),
                month = obj.optString("month", "2024-01"),
                isSavingsTarget = obj.optBoolean("isSavingsTarget", false),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            )
            budgetDao.insertBudget(budget)
            count++
        }
        return count
    }

    private suspend fun importGoals(array: JSONArray): Int {
        var count = 0
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val goal = GoalEntity(
                id = obj.optLong("id", 0),
                name = obj.getString("name"),
                emoji = obj.optString("emoji", "🎯"),
                targetAmount = obj.getDouble("targetAmount"),
                currentAmount = obj.optDouble("currentAmount", 0.0),
                deadline = if (obj.has("deadline") && !obj.isNull("deadline")) obj.getLong("deadline") else null,
                isCompleted = obj.optBoolean("isCompleted", false),
            )
            goalDao.insertGoal(goal)
            count++
        }
        return count
    }

    private suspend fun importTags(array: JSONArray): Int {
        var count = 0
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val tag = TagEntity(
                id = obj.optLong("id", 0),
                name = obj.getString("name"),
                color = obj.optString("color", "#c8420a"),
            )
            tagDao.insertTag(tag)
            count++
        }
        return count
    }

    private suspend fun importPeers(array: JSONArray): Int {
        var count = 0
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val peer = com.moneymanager.data.entity.PeerContact(
                id = obj.optLong("id", 0),
                displayName = obj.getString("displayName"),
                phoneNumber = obj.optString("phoneNumber", ""),
                email = obj.optString("email", ""),
                description = obj.optString("description", ""),
                photoUri = if (obj.has("photoUri") && !obj.isNull("photoUri")) obj.getString("photoUri") else null,
                totalGiven = obj.optDouble("totalGiven", 0.0),
                totalReceived = obj.optDouble("totalReceived", 0.0),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
            )
            peerContactDao.insertPeer(peer)
            count++
        }
        return count
    }

    private suspend fun importRecurring(array: JSONArray): Int {
        var count = 0
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val item = RecurringEntity(
                id = obj.optLong("id", 0),
                accountId = obj.getLong("accountId"),
                type = obj.getString("type"),
                amount = obj.getDouble("amount"),
                categoryId = if (obj.has("categoryId") && !obj.isNull("categoryId")) obj.getLong("categoryId") else null,
                subCategoryId = if (obj.has("subCategoryId") && !obj.isNull("subCategoryId")) obj.getLong("subCategoryId") else null,
                goalId = if (obj.has("goalId") && !obj.isNull("goalId")) obj.getLong("goalId") else null,
                note = obj.optString("note", ""),
                frequency = obj.getString("frequency"),
                startDate = obj.optLong("startDate", System.currentTimeMillis()),
                nextDate = obj.getLong("nextDate"),
                isActive = obj.optBoolean("isActive", true),
                reminderEnabled = obj.optBoolean("reminderEnabled", false),
                reminderDays = obj.optInt("reminderDays", 0),
                investmentApp = if (obj.has("investmentApp") && !obj.isNull("investmentApp")) obj.getString("investmentApp") else null,
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            )
            recurringDao.insertRecurring(item)
            count++
        }
        return count
    }

    private suspend fun importPeersFromCsv(csv: String): Int {
        var count = 0
        val lines = csv.lines().drop(1)
        for (line in lines) {
            if (line.isBlank()) continue
            val parts = parseCsvLine(line)
            if (parts.size >= 2) {
                val peer = com.moneymanager.data.entity.PeerContact(
                    displayName = parts[0],
                    phoneNumber = parts.getOrNull(1) ?: "",
                    email = parts.getOrNull(2) ?: "",
                    description = parts.getOrNull(3) ?: "",
                    totalGiven = parts.getOrNull(4)?.toDoubleOrNull() ?: 0.0,
                    totalReceived = parts.getOrNull(5)?.toDoubleOrNull() ?: 0.0,
                )
                peerContactDao.insertPeer(peer)
                count++
            }
        }
        return count
    }

    private suspend fun importTransactionsFromCsv(csv: String): Int {
        var count = 0
        val lines = csv.lines().drop(1)

        // Build name→id lookup maps so the CSV can use names instead of numeric IDs
        val accountsByName = accountDao.getAllAccounts().first().associateBy { it.name }
        val categoriesByName = categoryDao.getAllCategories().first().associateBy { it.name }

        for (line in lines) {
            if (line.isBlank()) continue
            val parts = parseCsvLine(line)
            if (parts.size < 22) continue  // Minimum 22 columns required (0-21), new format has 24

            val id = parts[0].toLongOrNull() ?: 0

            // Accept either a numeric ID or an account name
            val accountIdNullable = parts[1].toLongOrNull() ?: accountsByName[parts[1]]?.id
            if (accountIdNullable == null) continue
            val accountId = accountIdNullable!!

            // Accept either a numeric ID, a category name, or blank (null category)
            val categoryId = parts[2].toLongOrNull()
                ?: categoriesByName[parts[2]]?.id

            // New fields from Task 3
            val subCategoryId = parts[3].toLongOrNull()
            val goalId = parts[4].toLongOrNull()
            val peerContactId = parts[5].toLongOrNull()
            val tagIds = parts[6]

            // Core transaction fields (updated indices)
            val dateStr = parts[7]
            val amount = parts[8].toDoubleOrNull() ?: 0.0
            val type = parts[9]
            val note = parts[10].removeSurrounding("\"")
            val description = parts[11].removeSurrounding("\"")

            // More new fields
            val receiptPath = parts[12].ifBlank { null }
            val recurringId = parts[13].toLongOrNull()
            val splitData = parts[14].ifBlank { null }
            val investmentPlatform = parts[15].ifBlank { null }

            // Parse timestamp fields
            val expectedReturnDate = if (parts[16].isNotBlank()) {
                try { dateFormat.parse(parts[16])?.time } catch (e: Exception) { null }
            } else null

            val createdAt = if (parts[17].isNotBlank()) {
                try { dateFormat.parse(parts[17])?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
            } else System.currentTimeMillis()

            // Existing boolean fields (updated indices)
            val isRecurring = parts[18].toBooleanStrictOrNull() ?: false
            val isSplitParent = parts[19].toBooleanStrictOrNull() ?: false
            val isSplitChild = parts[20].toBooleanStrictOrNull() ?: false
            val parentTransactionId = parts[21].toLongOrNull()

            // New fields (indices 22-23)
            val isTransfer = if (parts.size > 22) {
                parts[22].toBooleanStrictOrNull() ?: (type == "transfer")
            } else {
                type == "transfer"
            }
            val toAccountId = parts.getOrNull(23)?.toLongOrNull()

            val date = try {
                dateFormat.parse(dateStr.trim())?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }

            val existing = if (id != 0L) transactionDao.getTransactionById(id) else null
            if (existing != null) {
                transactionDao.updateTransaction(existing.copy(
                    accountId = accountId,
                    type = type,
                    amount = amount,
                    categoryId = categoryId,
                    subCategoryId = subCategoryId,
                    goalId = goalId,
                    peerContactId = peerContactId,
                    tagIds = tagIds,
                    note = note,
                    description = description,
                    receiptPath = receiptPath,
                    date = date,
                    isRecurring = isRecurring,
                    recurringId = recurringId,
                    splitData = splitData,
                    investmentPlatform = investmentPlatform,
                    expectedReturnDate = expectedReturnDate,
                    createdAt = createdAt,
                    isSplitParent = isSplitParent,
                    isSplitChild = isSplitChild,
                    parentTransactionId = parentTransactionId,
                    isTransfer = isTransfer,
                    toAccountId = toAccountId,
                ))
            } else {
                transactionDao.insertTransaction(TransactionEntity(
                    id = id,
                    accountId = accountId,
                    type = type,
                    amount = amount,
                    categoryId = categoryId,
                    subCategoryId = subCategoryId,
                    goalId = goalId,
                    peerContactId = peerContactId,
                    tagIds = tagIds,
                    note = note,
                    description = description,
                    receiptPath = receiptPath,
                    date = date,
                    isRecurring = isRecurring,
                    recurringId = recurringId,
                    splitData = splitData,
                    investmentPlatform = investmentPlatform,
                    expectedReturnDate = expectedReturnDate,
                    createdAt = createdAt,
                    isSplitParent = isSplitParent,
                    isSplitChild = isSplitChild,
                    parentTransactionId = parentTransactionId,
                    isTransfer = isTransfer,
                    toAccountId = toAccountId,
                ))
            }
            count++
        }
        return count
    }

    private suspend fun importAccountsFromCsv(csv: String): Int {
        var count = 0
        val lines = csv.lines().drop(1)
        val existingNames = accountDao.getAllAccounts().first().map { it.name }.toHashSet()
        for (line in lines) {
            if (line.isBlank()) continue
            val parts = parseCsvLine(line)
            if (parts.size >= 3) {
                val name = parts[0]
                if (existingNames.contains(name)) continue
                val account = AccountEntity(
                    name = name,
                    type = parts[1],
                    balance = parts[2].toDoubleOrNull() ?: 0.0,
                    currency = parts.getOrNull(3) ?: "INR",
                    color = parts.getOrNull(4) ?: "#2a6049",
                )
                accountDao.insertAccount(account)
                existingNames.add(name)
                count++
            }
        }
        return count
    }

    private suspend fun importCategoriesFromCsv(csv: String): Int {
        var count = 0
        val lines = csv.lines().drop(1)
        for (line in lines) {
            if (line.isBlank()) continue
            val parts = parseCsvLine(line)
            if (parts.isNotEmpty()) {
                val name = parts[0]
                val type = parts.getOrNull(1) ?: "expense"
                if (categoryDao.getCategoryByName(name, type) != null) continue
                val category = CategoryEntity(
                    name = name,
                    type = type,
                    emoji = parts.getOrNull(2) ?: "📁",
                    parentId = parts.getOrNull(3)?.toLongOrNull(),
                    isCustom = parts.getOrNull(4)?.toBooleanStrictOrNull() ?: false,
                )
                categoryDao.insertCategory(category)
                count++
            }
        }
        return count
    }

    private suspend fun importTagsFromCsv(csv: String): Int {
        var count = 0
        val lines = csv.lines().drop(1)
        for (line in lines) {
            if (line.isBlank()) continue
            val parts = parseCsvLine(line)
            if (parts.isNotEmpty()) {
                val tag = TagEntity(
                    name = parts[0],
                    color = parts.getOrNull(1) ?: "#c8420a",
                )
                tagDao.insertTag(tag)
                count++
            }
        }
        return count
    }

    private suspend fun importBudgetsFromCsv(csv: String): Int {
        var count = 0
        val lines = csv.lines().drop(1)
        for (line in lines) {
            if (line.isBlank()) continue
            val parts = parseCsvLine(line)
            if (parts.size >= 3) {
                val budget = BudgetEntity(
                    categoryId = parts[0].toLongOrNull() ?: continue,
                    amount = parts[1].toDoubleOrNull() ?: 0.0,
                    month = parts[2],
                    isSavingsTarget = parts.getOrNull(3)?.toBooleanStrictOrNull() ?: false,
                )
                budgetDao.insertBudget(budget)
                count++
            }
        }
        return count
    }

    private suspend fun importGoalsFromCsv(csv: String): Int {
        var count = 0
        val lines = csv.lines().drop(1)
        for (line in lines) {
            if (line.isBlank()) continue
            val parts = parseCsvLine(line)
            if (parts.size >= 4) {
                val goal = GoalEntity(
                    name = parts[0],
                    emoji = parts.getOrNull(1) ?: "🎯",
                    targetAmount = parts[2].toDoubleOrNull() ?: 0.0,
                    currentAmount = parts[3].toDoubleOrNull() ?: 0.0,
                    deadline = parts.getOrNull(4)?.let {
                        try { dateFormat.parse(it)?.time } catch (e: Exception) { null }
                    },
                    isCompleted = parts.getOrNull(5)?.toBooleanStrictOrNull() ?: false,
                )
                goalDao.insertGoal(goal)
                count++
            }
        }
        return count
    }

    private suspend fun importRecurringFromCsv(csv: String): Int {
        var count = 0
        val lines = csv.lines().drop(1)
        for (line in lines) {
            if (line.isBlank()) continue
            val parts = parseCsvLine(line)
            if (parts.size >= 11) {
                val startDate = try { dateFormat.parse(parts[8])?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
                val nextDate = try { dateFormat.parse(parts[9])?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
                val item = RecurringEntity(
                    accountId = parts[0].toLongOrNull() ?: continue,
                    type = parts[1],
                    amount = parts[2].toDoubleOrNull() ?: 0.0,
                    categoryId = parts[3].toLongOrNull(),
                    subCategoryId = parts[4].toLongOrNull(),
                    goalId = parts[5].toLongOrNull(),
                    note = parts[6].removeSurrounding("\""),
                    frequency = parts[7],
                    startDate = startDate,
                    nextDate = nextDate,
                    isActive = parts[10].toBooleanStrictOrNull() ?: true,
                    reminderEnabled = parts.getOrNull(11)?.toBooleanStrictOrNull() ?: false,
                    reminderDays = parts.getOrNull(12)?.toIntOrNull() ?: 0,
                    investmentApp = parts.getOrNull(13)?.ifBlank { null },
                )
                recurringDao.insertRecurring(item)
                count++
            }
        }
        return count
    }

    private suspend fun importAllFromCsv(csv: String): ImportResult {
        var accountsCount = 0
        var categoriesCount = 0
        var transactionsCount = 0
        var budgetsCount = 0
        var goalsCount = 0
        var tagsCount = 0
        var peersCount = 0
        var recurringCount = 0

        val sections = csv.split(Regex("\n(?=# )"))
        for (section in sections) {
            val trimmed = section.trim()
            if (trimmed.isBlank()) continue
            val lines = trimmed.lines()
            if (lines.isEmpty()) continue
            val header = lines.first().trim()
            val body = lines.drop(1).joinToString("\n")

            when {
                header.startsWith("# ACCOUNTS") -> accountsCount = importAccountsFromCsv(body)
                header.startsWith("# CATEGORIES") -> categoriesCount = importCategoriesFromCsv(body)
                header.startsWith("# TRANSACTIONS") -> transactionsCount = importTransactionsFromCsv(body)
                header.startsWith("# BUDGETS") -> budgetsCount = importBudgetsFromCsv(body)
                header.startsWith("# GOALS") -> goalsCount = importGoalsFromCsv(body)
                header.startsWith("# TAGS") -> tagsCount = importTagsFromCsv(body)
                header.startsWith("# PEERS") -> peersCount = importPeersFromCsv(body)
                header.startsWith("# RECURRING") -> recurringCount = importRecurringFromCsv(body)
            }
        }

        val totalProcessed = accountsCount + categoriesCount + transactionsCount +
            budgetsCount + goalsCount + tagsCount + peersCount + recurringCount
        return ImportResult(
            success = true,
            message = "All data imported: $totalProcessed records processed",
            accountsImported = accountsCount,
            categoriesImported = categoriesCount,
            transactionsImported = transactionsCount,
            budgetsImported = budgetsCount,
            goalsImported = goalsCount,
            tagsImported = tagsCount,
            peersImported = peersCount,
            recurringImported = recurringCount,
            totalProcessed = totalProcessed,
        )
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        
        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                (char == ',') && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        return result
    }
}

enum class ExportType {
    TRANSACTIONS, ACCOUNTS, CATEGORIES, BUDGETS, GOALS, ALL, TAGS, PEERS, RECURRING
}
