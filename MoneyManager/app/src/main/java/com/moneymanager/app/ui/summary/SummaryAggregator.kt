package com.moneymanager.app.ui.summary

import androidx.compose.ui.graphics.Color
import com.moneymanager.app.ui.components.PieChartEntry
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.BudgetEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.TransactionEntity
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat
import kotlin.math.abs

object SummaryAggregator {

    /** Drops split-child rows. ALWAYS call this first. */
    fun excludeSplitChildren(txs: List<TransactionEntity>): List<TransactionEntity> =
        txs.filter { !it.isSplitChild }

    /**
     * Fix for CR-02: Filters out legacy double-entry "reverse" transfers.
     * Legacy transfers had two entries: A->B and B->A on the same date/amount.
     * We only keep one for aggregation to avoid double-counting.
     */
    fun deduplicateTransfers(txs: List<TransactionEntity>): List<TransactionEntity> {
        val transfers = txs.filter { it.type == "transfer" }
        if (transfers.isEmpty()) return txs

        val toRemove = mutableSetOf<Long>()
        val seen = mutableSetOf<String>()

        transfers.forEach { tx ->
            if (tx.id in toRemove) return@forEach

            // Key for the inverse transfer
            val inverseKey = "${tx.toAccountId}_${tx.accountId}_${tx.amount}_${tx.date}"

            if (seen.contains(inverseKey)) {
                toRemove.add(tx.id)
            } else {
                val key = "${tx.accountId}_${tx.toAccountId}_${tx.amount}_${tx.date}"
                seen.add(key)
            }
        }

        return txs.filter { it.id !in toRemove }
    }

    fun sumByType(txs: List<TransactionEntity>, type: String): Double =
        txs.filter { it.type == type }.sumOf { it.amount }

    /**
     * PieChartEntry list of expense by category, sorted desc.
     * Top (topN-1) categories returned individually; the rest are combined as "Others".
     * Expects [txs] to already have split-children excluded.
     */
    fun expenseByCategory(
        txs: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        topN: Int = 6,
        othersColor: Color = Color(0xFF90A4AE),
        parseColor: (String?, Long) -> Color
    ): List<PieChartEntry> {
        val expenseTxs = txs.filter { it.type == "expense" }
        val total = expenseTxs.sumOf { it.amount }
        if (total <= 0.0) return emptyList()

        val categoryMap = categories.associateBy { it.id }

        val grouped = expenseTxs
            .groupBy { tx ->
                // Map to root category ID for main-category level summary
                var currentId = tx.categoryId
                while (currentId != null) {
                    val cat = categoryMap[currentId] ?: break
                    if (cat.parentId == null) break
                    currentId = cat.parentId
                }
                currentId
            }
            .map { (rootCategoryId, group) ->
                val category = categoryMap[rootCategoryId]
                val amount = group.sumOf { it.amount }
                PieChartEntry(
                    label = category?.name ?: "Uncategorized",
                    value = amount,
                    color = parseColor(category?.color, rootCategoryId ?: 0L),
                    percentage = amount / total * 100.0
                )
            }
            .sortedByDescending { it.value }

        return if (grouped.size <= topN) {
            grouped
        } else {
            val top = grouped.take(topN - 1)
            val othersAmount = grouped.drop(topN - 1).sumOf { it.value }
            val othersPercent = othersAmount / total * 100.0
            top + listOf(
                PieChartEntry(
                    label = "Others",
                    value = othersAmount,
                    color = othersColor,
                    percentage = othersPercent
                )
            )
        }
    }

    fun expenseByAccount(
        txs: List<TransactionEntity>,
        accounts: List<AccountEntity>,
        topN: Int = 6,
        othersColor: Color = Color(0xFF90A4AE),
        parseColor: (String?, Long) -> Color
    ): List<PieChartEntry> {
        val expenseTxs = txs.filter { it.type == "expense" }
        val total = expenseTxs.sumOf { it.amount }
        if (total <= 0.0) return emptyList()

        val grouped = expenseTxs
            .groupBy { it.accountId }
            .map { (accountId, group) ->
                val account = accounts.find { it.id == accountId }
                val amount = group.sumOf { it.amount }
                PieChartEntry(
                    label = account?.name ?: "Unknown Account",
                    value = amount,
                    color = parseColor(account?.color, accountId),
                    percentage = amount / total * 100.0
                )
            }
            .sortedByDescending { it.value }

        return if (grouped.size <= topN) {
            grouped
        } else {
            val top = grouped.take(topN - 1)
            val othersAmount = grouped.drop(topN - 1).sumOf { it.value }
            val othersPercent = othersAmount / total * 100.0
            top + listOf(
                PieChartEntry(
                    label = "Others",
                    value = othersAmount,
                    color = othersColor,
                    percentage = othersPercent
                )
            )
        }
    }

    /**
     * Top N budget utilization rows for the given budgets.
     * Categories with no budget are included if they have spending.
     * Expects [txs] to already have split-children excluded.
     */
    fun topBudgetUtilization(
        txs: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        categories: List<CategoryEntity>,
        topN: Int = 5,
        parseColor: (String?) -> Color
    ): List<BudgetUtilizationRow> {
        val expenseTxs = txs.filter { it.type == "expense" }
        val totalExpense = expenseTxs.sumOf { it.amount }

        val allCategoryIds = (expenseTxs.map { it.categoryId } + budgets.map { it.categoryId })
            .distinct()
            .filterNotNull()

        return allCategoryIds.mapNotNull { categoryId ->
            val category = categories.find { it.id == categoryId }
            val budget = budgets.find { it.categoryId == categoryId }
            val spent = expenseTxs.filter { it.categoryId == categoryId }.sumOf { it.amount }

            val budgetAmount = budget?.amount ?: 0.0

            if (spent <= 0.0 && budgetAmount <= 0.0) return@mapNotNull null

            val utilization = if (budgetAmount > 0) (spent / budgetAmount * 100.0).toFloat() else 0f
            val percentOfTotal = if (totalExpense > 0) (spent / totalExpense * 100.0).toFloat() else 0f

            BudgetUtilizationRow(
                categoryId = categoryId,
                categoryName = category?.name ?: "Uncategorized",
                budgetLimit = budgetAmount,
                spent = spent,
                utilizationPercent = utilization,
                percentOfTotalExpense = percentOfTotal,
                color = parseColor(category?.color),
                emoji = category?.emoji ?: "📁",
                iconType = category?.iconType ?: "emoji",
                colorIndex = category?.colorIndex ?: 0
            )
        }
        .sortedByDescending { it.spent }
        .take(topN)
    }

    /**
     * Top N income category spends with percent of total income.
     */
    fun incomeByCategory(
        txs: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        topN: Int = 10,
        parseColor: (String?, Long) -> Color
    ): List<CategorySpend> {
        val incomeTxs = txs.filter { it.type == "income" }
        val total = incomeTxs.sumOf { it.amount }
        if (total <= 0.0) return emptyList()

        return incomeTxs
            .groupBy { it.categoryId }
            .map { (categoryId, group) ->
                val category = categories.find { it.id == categoryId }
                val amount = group.sumOf { it.amount }
                CategorySpend(
                    categoryId = categoryId,
                    name = category?.name ?: "Uncategorized",
                    amount = amount,
                    percentOfTotal = (amount / total * 100.0).toFloat(),
                    color = parseColor(category?.color, categoryId ?: 0L),
                    emoji = category?.emoji ?: "💰",
                    iconType = category?.iconType ?: "emoji",
                    colorIndex = category?.colorIndex ?: 0
                )
            }
            .sortedByDescending { it.amount }
            .take(topN)
    }

    fun savingsByCategorySpend(
        txs: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        topN: Int = 10,
        parseColor: (String?, Long) -> Color
    ): List<CategorySpend> {
        val savingsTxs = txs.filter { it.type == "savings" }
        val total = savingsTxs.sumOf { it.amount }
        if (total <= 0.0) return emptyList()

        return savingsTxs
            .groupBy { it.categoryId }
            .map { (categoryId, group) ->
                val category = categories.find { it.id == categoryId }
                val amount = group.sumOf { it.amount }
                CategorySpend(
                    categoryId = categoryId,
                    name = category?.name ?: "Uncategorized",
                    amount = amount,
                    percentOfTotal = (amount / total * 100.0).toFloat(),
                    color = parseColor(category?.color, categoryId ?: 0L),
                    emoji = category?.emoji ?: "💰",
                    iconType = category?.iconType ?: "emoji",
                    colorIndex = category?.colorIndex ?: 0
                )
            }
            .sortedByDescending { it.amount }
            .take(topN)
    }

    fun incomeByCategoryPie(
        txs: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        topN: Int = 6,
        othersColor: Color = Color(0xFF90A4AE),
        parseColor: (String?, Long) -> Color
    ): List<PieChartEntry> {
        val incomeTxs = txs.filter { it.type == "income" }
        val total = incomeTxs.sumOf { it.amount }
        if (total <= 0.0) return emptyList()

        val categoryMap = categories.associateBy { it.id }

        val grouped = incomeTxs
            .groupBy { tx ->
                // Map to root category ID for main-category level summary
                var currentId = tx.categoryId
                while (currentId != null) {
                    val cat = categoryMap[currentId] ?: break
                    if (cat.parentId == null) break
                    currentId = cat.parentId
                }
                currentId
            }
            .map { (rootCategoryId, group) ->
                val category = categoryMap[rootCategoryId]
                val amount = group.sumOf { it.amount }
                PieChartEntry(
                    label = category?.name ?: "Uncategorized",
                    value = amount,
                    color = parseColor(category?.color, rootCategoryId ?: 0L),
                    percentage = amount / total * 100.0
                )
            }
            .sortedByDescending { it.value }

        return if (grouped.size <= topN) {
            grouped
        } else {
            val top = grouped.take(topN - 1)
            val othersAmount = grouped.drop(topN - 1).sumOf { it.value }
            val othersPercent = othersAmount / total * 100.0
            top + listOf(
                PieChartEntry(
                    label = "Others",
                    value = othersAmount,
                    color = othersColor,
                    percentage = othersPercent
                )
            )
        }
    }

    fun incomeByAccount(
        txs: List<TransactionEntity>,
        accounts: List<AccountEntity>,
        topN: Int = 6,
        othersColor: Color = Color(0xFF90A4AE),
        parseColor: (String?, Long) -> Color
    ): List<PieChartEntry> {
        val incomeTxs = txs.filter { it.type == "income" }
        val total = incomeTxs.sumOf { it.amount }
        if (total <= 0.0) return emptyList()

        val grouped = incomeTxs
            .groupBy { it.accountId }
            .map { (accountId, group) ->
                val account = accounts.find { it.id == accountId }
                val amount = group.sumOf { it.amount }
                PieChartEntry(
                    label = account?.name ?: "Unknown Account",
                    value = amount,
                    color = parseColor(account?.color, accountId),
                    percentage = amount / total * 100.0
                )
            }
            .sortedByDescending { it.value }

        return if (grouped.size <= topN) {
            grouped
        } else {
            val top = grouped.take(topN - 1)
            val othersAmount = grouped.drop(topN - 1).sumOf { it.value }
            val othersPercent = othersAmount / total * 100.0
            top + listOf(
                PieChartEntry(
                    label = "Others",
                    value = othersAmount,
                    color = othersColor,
                    percentage = othersPercent
                )
            )
        }
    }

    fun netBalance(txs: List<TransactionEntity>): Double =
        sumByType(txs, "income") - sumByType(txs, "expense")

    fun lendingPeople(
        txs: List<TransactionEntity>,
        peers: List<com.moneymanager.data.entity.PeerContact>,
        topN: Int = 10
    ): List<LendingPerson> {
        // We want to calculate the balance for each peer based on ALL transactions related to them
        // But if we only have txs for the period, we can only show activity in that period.
        // Usually, the "Lending" tab shows current outstanding balances.
        // For now, let's aggregate based on the transactions provided (which might be period-limited or all).
        
        return txs.filter { it.peerContactId != null }
            .groupBy { it.peerContactId }
            .mapNotNull { (peerId, peerTxs) ->
                val peer = peers.find { it.id == peerId } ?: return@mapNotNull null
                
                // Balance calculation: 
                // (Lend - Receive) -> What they owe me
                // (Borrow - Repay) -> What I owe them
                val lent = peerTxs.filter { it.type == "lend" }.sumOf { it.amount }
                val borrowed = peerTxs.filter { it.type == "borrow" }.sumOf { it.amount }
                
                val net = lent - borrowed
                
                if (net == 0.0) return@mapNotNull null
                
                LendingPerson(
                    id = peer.id,
                    name = peer.effectiveDisplayName,
                    amount = Math.abs(net),
                    isOwed = net > 0,
                    avatar = peer.photoUri
                )
            }
            .sortedByDescending { it.amount }
            .take(topN)
    }

    fun accountTransferSummary(
        txs: List<TransactionEntity>,
        accounts: List<AccountEntity>
    ): List<AccountTransferInfo> {
        val transferTxs = txs.filter { it.type == "transfer" }

        return accounts.map { account ->
            val outTxs = transferTxs.filter { it.accountId == account.id }
            val inTxs = transferTxs.filter { it.toAccountId == account.id }

            val outAmount = outTxs.sumOf { it.amount }
            val inAmount = inTxs.sumOf { it.amount }
            val count = outTxs.size + inTxs.size

            AccountTransferInfo(
                accountId = account.id,
                accountName = account.name,
                accountNumber = "**** " + account.id.toString().takeLast(4).padStart(4, '0'),
                accountType = account.type.replaceFirstChar { it.uppercase() } + " Account",
                rawType = account.type,
                balance = account.balance,
                transferCount = count,
                inAmount = inAmount,
                outAmount = outAmount,
                emoji = account.emoji,
                iconType = account.iconType,
                color = account.color,
                colorIndex = (account.id % 40).toInt()
            )
        }.sortedByDescending { it.transferCount }
    }

    fun savingsSummary(
        accounts: List<AccountEntity>
    ): List<SavingsAccountRow> {
        return accounts.filter { it.type == "savings" }
            .map { account ->
                SavingsAccountRow(
                    id = account.id,
                    name = account.name,
                    accountNumber = "XXXX " + account.id.toString().takeLast(4).padStart(4, '0'),
                    balance = account.balance,
                    type = account.type,
                    emoji = account.emoji,
                    iconType = account.iconType,
                    color = account.color,
                    colorIndex = (account.id % 40).toInt()
                )
            }.sortedByDescending { it.balance }
    }

    fun savingsInflow(
        txs: List<TransactionEntity>,
        savingsAccountIds: Set<Long>
    ): Double {
        return txs.filter {
            it.type == "income" && it.accountId in savingsAccountIds ||
            it.type == "savings" ||
            it.type == "transfer" && it.toAccountId in savingsAccountIds
        }.sumOf { it.amount }
    }

    fun savingsGoals(
        goals: List<com.moneymanager.data.entity.GoalEntity>,
        linkedAmounts: Map<Long, Double> = emptyMap(),
        parseColor: (String?, Long) -> Color
    ): List<SavingsGoalRow> {
        return goals.map { goal ->
            val linked = linkedAmounts[goal.id] ?: 0.0
            val totalCurrent = goal.currentAmount + linked
            val progress = if (goal.targetAmount > 0) (totalCurrent / goal.targetAmount * 100.0).toFloat() else 0f
            SavingsGoalRow(
                id = goal.id,
                name = goal.name,
                targetAmount = goal.targetAmount,
                currentAmount = totalCurrent,
                linkedAmount = linked,
                progressPercent = progress,
                emoji = goal.emoji,
                iconType = goal.iconType,
                color = parseColor(null, goal.id),
                colorIndex = (goal.id % 40).toInt()
            )
        }.sortedByDescending { it.progressPercent }
    }

    fun savingsByCategory(
        txs: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        topN: Int = 6,
        othersColor: Color = Color(0xFF90A4AE),
        parseColor: (String?, Long) -> Color
    ): List<PieChartEntry> {
        val savingsTxs = txs.filter { it.type == "savings" }
        val total = savingsTxs.sumOf { it.amount }
        if (total <= 0.0) return emptyList()

        val categoryMap = categories.associateBy { it.id }

        val grouped = savingsTxs
            .groupBy { tx ->
                var currentId = tx.categoryId
                while (currentId != null) {
                    val cat = categoryMap[currentId] ?: break
                    if (cat.parentId == null) break
                    currentId = cat.parentId
                }
                currentId
            }
            .map { (rootCategoryId, group) ->
                val category = categoryMap[rootCategoryId]
                val amount = group.sumOf { it.amount }
                PieChartEntry(
                    label = category?.name ?: "Uncategorized",
                    value = amount,
                    color = parseColor(category?.color, rootCategoryId ?: 0L),
                    percentage = amount / total * 100.0
                )
            }
            .sortedByDescending { it.value }

        return if (grouped.size <= topN) {
            grouped
        } else {
            val top = grouped.take(topN - 1)
            val othersAmount = grouped.drop(topN - 1).sumOf { it.value }
            val othersPercent = othersAmount / total * 100.0
            top + listOf(
                PieChartEntry(
                    label = "Others",
                    value = othersAmount,
                    color = othersColor,
                    percentage = othersPercent
                )
            )
        }
    }

    fun savingsByAccount(
        txs: List<TransactionEntity>,
        accounts: List<AccountEntity>,
        topN: Int = 6,
        othersColor: Color = Color(0xFF90A4AE),
        parseColor: (String?, Long) -> Color
    ): List<PieChartEntry> {
        val savingsTxs = txs.filter { it.type == "savings" }
        val total = savingsTxs.sumOf { it.amount }
        if (total <= 0.0) return emptyList()

        val grouped = savingsTxs
            .groupBy { it.accountId }
            .map { (accountId, group) ->
                val account = accounts.find { it.id == accountId }
                val amount = group.sumOf { it.amount }
                PieChartEntry(
                    label = account?.name ?: "Unknown Account",
                    value = amount,
                    color = parseColor(account?.color, accountId),
                    percentage = amount / total * 100.0
                )
            }
            .sortedByDescending { it.value }

        return if (grouped.size <= topN) {
            grouped
        } else {
            val top = grouped.take(topN - 1)
            val othersAmount = grouped.drop(topN - 1).sumOf { it.value }
            val othersPercent = othersAmount / total * 100.0
            top + listOf(
                PieChartEntry(
                    label = "Others",
                    value = othersAmount,
                    color = othersColor,
                    percentage = othersPercent
                )
            )
        }
    }

    fun calculateTrend(
        allTxs: List<TransactionEntity>,
        type: TrendType,
        timeFilter: TrendTimeFilter
    ): Pair<List<TrendDataPoint>, TrendStats> {
        val txType = when (type) {
            TrendType.INCOME -> "income"
            TrendType.EXPENSE -> "expense"
            TrendType.LENDING -> "lend"
            TrendType.SAVINGS -> "savings"
            TrendType.OVERALL -> "overall"
        }

        val now = Calendar.getInstance()
        val limit = if (timeFilter == TrendTimeFilter.YEAR_1) {
            val cal = Calendar.getInstance()
            cal.set(Calendar.MONTH, Calendar.JANUARY)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        } else 0L

        val filteredTxs = allTxs.filter { it.date >= limit }

        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val shortMonthFormat = SimpleDateFormat("MMM", Locale.getDefault())

        val groupedByMonth = filteredTxs.groupBy {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.date
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }

        val monthsToInclude = mutableListOf<Long>()
        if (timeFilter == TrendTimeFilter.YEAR_1) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = limit
            for (i in 0..11) {
                monthsToInclude.add(cal.timeInMillis)
                cal.add(Calendar.MONTH, 1)
            }
        } else {
            if (groupedByMonth.isNotEmpty()) {
                val minMonth = groupedByMonth.keys.minOrNull() ?: 0L
                val cal = Calendar.getInstance()
                cal.timeInMillis = minMonth
                while (cal.timeInMillis <= now.timeInMillis) {
                    monthsToInclude.add(cal.timeInMillis)
                    cal.add(Calendar.MONTH, 1)
                }
            } else {
                return calculateTrend(allTxs, type, TrendTimeFilter.YEAR_1)
            }
        }

        val dataPoints = monthsToInclude.map { timestamp ->
            val monthTxs = groupedByMonth[timestamp] ?: emptyList()
            val amount = when (type) {
                TrendType.INCOME -> monthTxs.filter { it.type == "income" }.sumOf { it.amount }
                TrendType.EXPENSE -> monthTxs.filter { it.type == "expense" }.sumOf { it.amount }
                TrendType.LENDING -> monthTxs.filter { it.type == "lend" }.sumOf { it.amount } - monthTxs.filter { it.type == "borrow" }.sumOf { it.amount }
                TrendType.SAVINGS -> monthTxs.filter { it.type == "savings" }.sumOf { it.amount }
                TrendType.OVERALL -> {
                    val inc = monthTxs.filter { it.type == "income" }.sumOf { it.amount }
                    val exp = monthTxs.filter { it.type == "expense" }.sumOf { it.amount }
                    val lendNet = monthTxs.filter { it.type == "lend" }.sumOf { it.amount } - monthTxs.filter { it.type == "borrow" }.sumOf { it.amount }
                    val sav = monthTxs.filter { it.type == "savings" }.sumOf { it.amount }
                    inc - (exp + sav + lendNet)
                }
            }

            val cal = Calendar.getInstance()
            cal.timeInMillis = timestamp
            val label = if (timeFilter == TrendTimeFilter.YEAR_1) shortMonthFormat.format(cal.time) else monthFormat.format(cal.time)

            TrendDataPoint(label, timestamp, amount)
        }

        // Stats calculation
        if (dataPoints.isEmpty()) return Pair(emptyList(), TrendStats())

        val currentMonthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val currentPoint = dataPoints.find { it.timestamp == currentMonthStart }
        val currentAmount = currentPoint?.amount ?: 0.0
        val currentLabel = currentPoint?.date ?: ""

        val nonZeroPoints = dataPoints.filter { it.amount != 0.0 }
        val highestPoint = dataPoints.maxByOrNull { it.amount }
        val lowestPoint = if (nonZeroPoints.isNotEmpty()) nonZeroPoints.minByOrNull { it.amount } else dataPoints.minByOrNull { it.amount }

        val total = dataPoints.sumOf { it.amount }
        val average = if (dataPoints.isNotEmpty()) total / dataPoints.size else 0.0

        val sortedAmounts = dataPoints.map { it.amount }.sorted()
        val median = if (sortedAmounts.isNotEmpty()) {
            if (sortedAmounts.size % 2 == 0) {
                (sortedAmounts[sortedAmounts.size / 2 - 1] + sortedAmounts[sortedAmounts.size / 2]) / 2.0
            } else {
                sortedAmounts[sortedAmounts.size / 2]
            }
        } else 0.0

        // Growth vs last year
        val prev12MonthsStart = Calendar.getInstance().apply {
            timeInMillis = if (limit > 0) limit else {
                val minTs = monthsToInclude.minOrNull() ?: 0L
                if (minTs > 0) minTs else now.timeInMillis
            }
            add(Calendar.YEAR, -1)
        }.timeInMillis
        
        val limitForPrev = if (limit > 0) limit else {
             monthsToInclude.minOrNull() ?: 0L
        }

        val prev12MonthsTxs = allTxs.filter { it.date >= prev12MonthsStart && it.date < limitForPrev }
        val prev12MonthsTotal = when (type) {
            TrendType.INCOME -> prev12MonthsTxs.filter { it.type == "income" }.sumOf { it.amount }
            TrendType.EXPENSE -> prev12MonthsTxs.filter { it.type == "expense" }.sumOf { it.amount }
            TrendType.LENDING -> prev12MonthsTxs.filter { it.type == "lend" }.sumOf { it.amount } - prev12MonthsTxs.filter { it.type == "borrow" }.sumOf { it.amount }
            TrendType.SAVINGS -> prev12MonthsTxs.filter { it.type == "savings" }.sumOf { it.amount }
            TrendType.OVERALL -> {
                val inc = prev12MonthsTxs.filter { it.type == "income" }.sumOf { it.amount }
                val exp = prev12MonthsTxs.filter { it.type == "expense" }.sumOf { it.amount }
                val lendNet = prev12MonthsTxs.filter { it.type == "lend" }.sumOf { it.amount } - prev12MonthsTxs.filter { it.type == "borrow" }.sumOf { it.amount }
                val sav = prev12MonthsTxs.filter { it.type == "savings" }.sumOf { it.amount }
                inc - (exp + sav + lendNet)
            }
        }

        val growthPercent = if (prev12MonthsTotal != 0.0) {
            ((total - prev12MonthsTotal) / abs(prev12MonthsTotal)) * 100.0
        } else if (total != 0.0) 100.0 else 0.0

        val stats = TrendStats(
            current = currentAmount,
            currentLabel = currentLabel,
            highest = highestPoint?.amount ?: 0.0,
            highestMonth = highestPoint?.let { monthFormat.format(java.util.Date(it.timestamp)) } ?: "",
            average = average,
            growthPercent = growthPercent,
            lowest = lowestPoint?.amount ?: 0.0,
            lowestMonth = lowestPoint?.let { monthFormat.format(java.util.Date(it.timestamp)) } ?: "",
            total = total,
            median = median,
            lastYearTotal = prev12MonthsTotal
        )

        return Pair(dataPoints, stats)
    }
}
