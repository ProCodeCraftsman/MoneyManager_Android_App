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

            ImportResult(
                success = true,
                message = "Import completed: $accountsImported accounts, $transactionsImported transactions, etc.",
                accountsImported = accountsImported,
                transactionsImported = transactionsImported,
                categoriesImported = categoriesImported,
                budgetsImported = budgetsImported,
                goalsImported = goalsImported,
                tagsImported = tagsImported,
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
            obj.put("balance", account.balance)
            obj.put("currency", account.currency)
            obj.put("color", account.color)
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
            array.put(obj)
        }
        return array
    }

    private suspend fun exportCategories(): JSONArray {
        val categories = categoryDao.getAllCategories().first()
        val array = JSONArray()
        categories.forEach { cat ->
            val obj = JSONObject()
            obj.put("id", cat.id)
            obj.put("name", cat.name)
            obj.put("emoji", cat.emoji)
            obj.put("type", cat.type)
            obj.put("parentId", cat.parentId)
            obj.put("isCustom", cat.isCustom)
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
            obj.put("targetAmount", goal.targetAmount)
            obj.put("currentAmount", goal.currentAmount)
            obj.put("deadline", goal.deadline)
            obj.put("createdAt", goal.createdAt)
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

    private suspend fun exportTransactionsCsv(): String {
        val transactions = transactionDao.getAllTransactions().first()
        val accounts = accountDao.getAllAccounts().first().associateBy { it.id }
        val categories = categoryDao.getAllCategories().first().associateBy { it.id }
        
        val sb = StringBuilder()
        sb.appendLine("id,account_id,category_id,date,amount,type,note,is_recurring,is_split_parent,is_split_child,parent_transaction_id")
        
        transactions.forEach { tx ->
            val accountName = accounts[tx.accountId]?.name ?: ""
            val categoryName = categories[tx.categoryId]?.name ?: ""
            
            sb.appendLine("${tx.id},${tx.accountId},${tx.categoryId ?: ""},${dateFormat.format(Date(tx.date))},${tx.amount},${tx.type},\"${tx.note.replace("\"", "\"\"")}\",${tx.isRecurring},${tx.isSplitParent},${tx.isSplitChild},${tx.parentTransactionId ?: ""}")
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
        val categories = categoryDao.getAllCategories().first()
        val sb = StringBuilder()
        sb.appendLine("name,type,emoji,parent_id,is_custom")
        categories.forEach { cat ->
            sb.appendLine("${cat.name},${cat.type},${cat.emoji},${cat.parentId ?: ""},${cat.isCustom}")
        }
        return sb.toString()
    }

    private suspend fun exportBudgetsCsv(): String {
        val budgets = budgetDao.getAllBudgets().first()
        val sb = StringBuilder()
        sb.appendLine("category_id,amount,month")
        budgets.forEach { budget ->
            sb.appendLine("${budget.categoryId},${budget.amount},${budget.month}")
        }
        return sb.toString()
    }

    private suspend fun exportGoalsCsv(): String {
        val goals = goalDao.getAllGoals().first()
        val sb = StringBuilder()
        sb.appendLine("name,emoji,target_amount,current_amount,deadline")
        goals.forEach { goal ->
            sb.appendLine("${goal.name},${goal.emoji},${goal.targetAmount},${goal.currentAmount},${goal.deadline?.let { dateFormat.format(Date(it)) } ?: ""}")
        }
        return sb.toString()
    }

    private suspend fun exportPeersCsv(): String {
        val peers = peerContactDao.getAllPeers().first()
        return buildString {
            appendLine("displayName,phoneNumber,totalGiven,totalReceived,outstandingBalance")
            peers.forEach { peer ->
                appendLine("${peer.displayName},${peer.phoneNumber},${peer.totalGiven},${peer.totalReceived},${peer.outstandingBalance}")
            }
        }
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
        }
    }
    
    private fun generateCsvHeader(): String = "id,account_id,category_id,date,amount,type,note,is_recurring,is_split_parent,is_split_child,parent_transaction_id"
    
    private suspend fun generateCsvData(): String {
        val transactions = transactionDao.getAllTransactions().first()
        val accounts = accountDao.getAllAccounts().first().associateBy { it.id }
        val categories = categoryDao.getAllCategories().first().associateBy { it.id }
        
        return buildString {
            transactions.forEach { tx ->
                appendLine("${tx.id},${tx.accountId},${tx.categoryId ?: ""},${dateFormat.format(Date(tx.date))},${tx.amount},${tx.type},\"${tx.note.replace("\"", "\"\"")}\",${tx.isRecurring},${tx.isSplitParent},${tx.isSplitChild},${tx.parentTransactionId ?: ""}")
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
                balance = obj.getDouble("balance"),
                currency = obj.optString("currency", "INR"),
                color = obj.optString("color", "#2a6049"),
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
                tagIds = obj.optString("tagIds", ""),
                date = obj.optLong("date", System.currentTimeMillis()),
                note = obj.optString("note", ""),
                isRecurring = obj.optBoolean("isRecurring", false),
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
                type = obj.getString("type"),
                parentId = if (obj.has("parentId") && !obj.isNull("parentId")) obj.getLong("parentId") else null,
                isCustom = obj.optBoolean("isCustom", false),
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

    private suspend fun importTransactionsFromCsv(csv: String): Int {
        var count = 0
        val lines = csv.lines().drop(1)

        // Build name→id lookup maps so the CSV can use names instead of numeric IDs
        val accountsByName = accountDao.getAllAccounts().first().associateBy { it.name }
        val categoriesByName = categoryDao.getAllCategories().first().associateBy { it.name }

        for (line in lines) {
            if (line.isBlank()) continue
            val parts = parseCsvLine(line)
            if (parts.size < 8) continue

            val id = parts[0].toLongOrNull() ?: 0

            // Accept either a numeric ID or an account name
            val accountId = parts[1].toLongOrNull()
                ?: accountsByName[parts[1]]?.id
                ?: continue

            // Accept either a numeric ID, a category name, or blank (null category)
            val categoryId = parts[2].toLongOrNull()
                ?: categoriesByName[parts[2]]?.id

            val dateStr = parts[3]
            val amount = parts[4].toDoubleOrNull() ?: 0.0
            val type = parts[5]
            val note = parts[6].removeSurrounding("\"")
            val isRecurring = parts[7].toBooleanStrictOrNull() ?: false
            val isSplitParent = parts.getOrNull(8)?.toBooleanStrictOrNull() ?: false
            val isSplitChild = parts.getOrNull(9)?.toBooleanStrictOrNull() ?: false
            val parentTransactionId = parts.getOrNull(10)?.toLongOrNull()

            // Accept either a numeric ID or an account name for transfer destination
            val toAccountIdRaw = parts.getOrNull(11)?.trim()
            val toAccountId = toAccountIdRaw?.toLongOrNull()
                ?: accountsByName[toAccountIdRaw]?.id

            val date = try {
                dateFormat.parse(dateStr.trim())?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }

            val isTransfer = type == "transfer"

            val existing = if (id != 0L) transactionDao.getTransactionById(id) else null
            if (existing != null) {
                transactionDao.updateTransaction(existing.copy(
                    accountId = accountId,
                    type = type,
                    amount = amount,
                    categoryId = categoryId,
                    note = note,
                    date = date,
                    isRecurring = isRecurring,
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
                    note = note,
                    date = date,
                    isRecurring = isRecurring,
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
                if (existingNames.contains(name)) continue  // skip duplicates
                val account = AccountEntity(
                    name = name,
                    type = parts[1],
                    balance = parts[2].toDoubleOrNull() ?: 0.0,
                    currency = parts.getOrNull(3) ?: "INR",
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
                if (categoryDao.getCategoryByName(name, type) != null) continue  // skip duplicates
                val category = CategoryEntity(
                    name = name,
                    type = type,
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
    TRANSACTIONS, ACCOUNTS, CATEGORIES, BUDGETS, GOALS, ALL, TAGS, PEERS
}
