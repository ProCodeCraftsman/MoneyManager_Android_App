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
    val filePath: String? = null
)

data class ImportResult(
    val success: Boolean,
    val message: String,
    val accountsImported: Int = 0,
    val transactionsImported: Int = 0,
    val categoriesImported: Int = 0,
    val budgetsImported: Int = 0,
    val goalsImported: Int = 0,
    val tagsImported: Int = 0
)

@Singleton
class ExportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val goalDao: GoalDao,
    private val tagDao: TagDao
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
            
            ExportResult(true, "Data exported successfully")
        } catch (e: Exception) {
            ExportResult(false, "Export failed: ${e.message}")
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
            }
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(csv)
                }
            }
            
            ExportResult(true, "${type.name} exported successfully")
        } catch (e: Exception) {
            ExportResult(false, "Export failed: ${e.message}")
        }
    }

    suspend fun importFromJson(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            } ?: return@withContext ImportResult(false, "Could not read file")

            val jsonObject = JSONObject(json)
            val result = ImportResult(true, "Import completed")
            
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
            if (jsonObject.has("accounts")) {
                accountsImported = importAccounts(jsonObject.getJSONArray("accounts"))
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
                tagsImported = tagsImported
            )
        } catch (e: Exception) {
            ImportResult(false, "Import failed: ${e.message}")
        }
    }

    suspend fun importFromCsv(uri: Uri, type: ExportType): ImportResult = withContext(Dispatchers.IO) {
        try {
            val csv = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            } ?: return@withContext ImportResult(false, "Could not read file")

            when (type) {
                ExportType.TRANSACTIONS -> {
                    val count = importTransactionsFromCsv(csv)
                    ImportResult(true, "$count transactions imported", transactionsImported = count)
                }
                ExportType.ACCOUNTS -> {
                    val count = importAccountsFromCsv(csv)
                    ImportResult(true, "$count accounts imported", accountsImported = count)
                }
                ExportType.CATEGORIES -> {
                    val count = importCategoriesFromCsv(csv)
                    ImportResult(true, "$count categories imported", categoriesImported = count)
                }
                else -> ImportResult(false, "CSV import not supported for ${type.name}")
            }
        } catch (e: Exception) {
            ImportResult(false, "Import failed: ${e.message}")
        }
    }

    private fun exportAccounts(): JSONArray {
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
            obj.put("icon", account.icon)
            obj.put("createdAt", account.createdAt)
            obj.put("updatedAt", account.updatedAt)
            array.put(obj)
        }
        return array
    }

    private fun exportTransactions(): JSONArray {
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

    private fun exportCategories(): JSONArray {
        val categories = categoryDao.getAllCategories().first()
        val array = JSONArray()
        categories.forEach { cat ->
            val obj = JSONObject()
            obj.put("id", cat.id)
            obj.put("name", cat.name)
            obj.put("type", cat.type)
            obj.put("icon", cat.icon)
            obj.put("color", cat.color)
            obj.put("isDefault", cat.isDefault)
            array.put(obj)
        }
        return array
    }

    private fun exportBudgets(): JSONArray {
        val budgets = budgetDao.getAllBudgets().first()
        val array = JSONArray()
        budgets.forEach { budget ->
            val obj = JSONObject()
            obj.put("id", budget.id)
            obj.put("categoryId", budget.categoryId)
            obj.put("amount", budget.amount)
            obj.put("period", budget.period)
            obj.put("startDate", budget.startDate)
            array.put(obj)
        }
        return array
    }

    private fun exportGoals(): JSONArray {
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

    private fun exportTags(): JSONArray {
        val tags = tagDao.getAllTags().first()
        val array = JSONArray()
        tags.forEach { tag ->
            val obj = JSONObject()
            obj.put("id", tag.id)
            obj.put("name", tag.name)
            obj.put("color", tag.color)
            obj.put("categoryId", tag.categoryId)
            array.put(obj)
        }
        return array
    }

    private fun exportTransactionsCsv(): String {
        val transactions = transactionDao.getAllTransactions().first()
        val sb = StringBuilder()
        sb.appendLine("date,amount,type,category_id,note,account_id,is_recurring")
        transactions.forEach { tx ->
            sb.appendLine("${dateFormat.format(Date(tx.date))},${tx.amount},${tx.type},${tx.categoryId ?: ""},\"${tx.note.replace("\"", "\"\"")}\",${tx.accountId},${tx.isRecurring}")
        }
        return sb.toString()
    }

    private fun exportAccountsCsv(): String {
        val accounts = accountDao.getAllAccounts().first()
        val sb = StringBuilder()
        sb.appendLine("name,type,balance,currency,color,icon")
        accounts.forEach { acc ->
            sb.appendLine("${acc.name},${acc.type},${acc.balance},${acc.currency},${acc.color ?: ""},${acc.icon ?: ""}")
        }
        return sb.toString()
    }

    private fun exportCategoriesCsv(): String {
        val categories = categoryDao.getAllCategories().first()
        val sb = StringBuilder()
        sb.appendLine("name,type,icon,color,is_default")
        categories.forEach { cat ->
            sb.appendLine("${cat.name},${cat.type},${cat.icon ?: ""},${cat.color ?: ""},${cat.isDefault}")
        }
        return sb.toString()
    }

    private fun exportBudgetsCsv(): String {
        val budgets = budgetDao.getAllBudgets().first()
        val sb = StringBuilder()
        sb.appendLine("category_id,amount,period,start_date")
        budgets.forEach { budget ->
            sb.appendLine("${budget.categoryId},${budget.amount},${budget.period},${dateFormat.format(Date(budget.startDate))}")
        }
        return sb.toString()
    }

    private fun exportGoalsCsv(): String {
        val goals = goalDao.getAllGoals().first()
        val sb = StringBuilder()
        sb.appendLine("name,emoji,target_amount,current_amount,deadline")
        goals.forEach { goal ->
            sb.appendLine("${goal.name},${goal.emoji},${goal.targetAmount},${goal.currentAmount},${goal.deadline?.let { dateFormat.format(Date(it)) } ?: ""}")
        }
        return sb.toString()
    }

    private fun exportAllCsv(): String {
        return buildString {
            appendLine("# ACCOUNTS")
            appendLine(exportAccountsCsv())
            appendLine()
            appendLine("# CATEGORIES")
            appendLine(exportCategoriesCsv())
            appendLine()
            appendLine("# TRANSACTIONS")
            appendLine(exportTransactionsCsv())
            appendLine()
            appendLine("# BUDGETS")
            appendLine(exportBudgetsCsv())
            appendLine()
            appendLine("# GOALS")
            appendLine(exportGoalsCsv())
        }
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
                currency = obj.optString("currency", "USD"),
                color = obj.optString("color"),
                icon = obj.optString("icon")
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
                isRecurring = obj.optBoolean("isRecurring", false)
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
                type = obj.getString("type"),
                icon = obj.optString("icon"),
                color = obj.optString("color"),
                isDefault = obj.optBoolean("isDefault", false)
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
                period = obj.optString("period", "monthly"),
                startDate = obj.optLong("startDate", System.currentTimeMillis())
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
                emoji = obj.optString("emoji", ""),
                targetAmount = obj.getDouble("targetAmount"),
                currentAmount = obj.optDouble("currentAmount", 0.0),
                deadline = if (obj.has("deadline") && !obj.isNull("deadline")) obj.getLong("deadline") else null
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
                color = obj.optString("color"),
                categoryId = obj.optLong("categoryId", 0)
            )
            tagDao.insertTag(tag)
            count++
        }
        return count
    }

    private suspend fun importTransactionsFromCsv(csv: String): Int {
        var count = 0
        val lines = csv.lines().drop(1)
        for (line in lines) {
            if (line.isBlank()) continue
            val parts = parseCsvLine(line)
            if (parts.size >= 5) {
                val transaction = TransactionEntity(
                    accountId = parts.getOrNull(5)?.toLongOrNull() ?: 0,
                    type = parts[2],
                    amount = parts[1].toDoubleOrNull() ?: continue,
                    categoryId = parts.getOrNull(3)?.toLongOrNull(),
                    note = parts.getOrNull(4)?.removeSurrounding("\"") ?: "",
                    isRecurring = parts.getOrNull(6)?.toBooleanStrictOrNull() ?: false
                )
                transactionDao.insertTransaction(transaction)
                count++
            }
        }
        return count
    }

    private suspend fun importAccountsFromCsv(csv: String): Int {
        var count = 0
        val lines = csv.lines().drop(1)
        for (line in lines) {
            if (line.isBlank()) continue
            val parts = parseCsvLine(line)
            if (parts.size >= 3) {
                val account = AccountEntity(
                    name = parts[0],
                    type = parts[1],
                    balance = parts[2].toDoubleOrNull() ?: 0.0,
                    currency = parts.getOrNull(3) ?: "USD"
                )
                accountDao.insertAccount(account)
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
                val category = CategoryEntity(
                    name = parts[0],
                    type = parts.getOrNull(1) ?: "expense"
                )
                categoryDao.insertCategory(category)
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
                char == ',' && !inQuotes -> {
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
    TRANSACTIONS, ACCOUNTS, CATEGORIES, BUDGETS, GOALS, ALL, TAGS
}
