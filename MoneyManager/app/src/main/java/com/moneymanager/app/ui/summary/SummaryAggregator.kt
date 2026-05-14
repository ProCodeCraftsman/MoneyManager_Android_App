package com.moneymanager.app.ui.summary

import androidx.compose.ui.graphics.Color
import com.moneymanager.app.ui.components.PieChartEntry
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.BudgetEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.TransactionEntity

object SummaryAggregator {

    /** Drops split-child rows. ALWAYS call this first. */
    fun excludeSplitChildren(txs: List<TransactionEntity>): List<TransactionEntity> =
        txs.filter { !it.isSplitChild }

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

        val grouped = expenseTxs
            .groupBy { it.categoryId }
            .map { (categoryId, group) ->
                val category = categories.find { it.id == categoryId }
                val amount = group.sumOf { it.amount }
                PieChartEntry(
                    label = category?.name ?: "Uncategorized",
                    value = amount,
                    color = parseColor(category?.color, categoryId ?: 0L),
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
                iconType = category?.iconType ?: "emoji"
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
                    iconType = category?.iconType ?: "emoji"
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

        val grouped = incomeTxs
            .groupBy { it.categoryId }
            .map { (categoryId, group) ->
                val category = categories.find { it.id == categoryId }
                val amount = group.sumOf { it.amount }
                PieChartEntry(
                    label = category?.name ?: "Uncategorized",
                    value = amount,
                    color = parseColor(category?.color, categoryId ?: 0L),
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
                val received = peerTxs.filter { it.type == "receive" }.sumOf { it.amount }
                val borrowed = peerTxs.filter { it.type == "borrow" }.sumOf { it.amount }
                val repaid = peerTxs.filter { it.type == "repay" }.sumOf { it.amount }
                
                val myOwed = lent - received
                val theyOwed = borrowed - repaid
                
                val net = myOwed - theyOwed
                
                if (net == 0.0) return@mapNotNull null
                
                LendingPerson(
                    id = peer.id,
                    name = peer.displayName,
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
                balance = account.balance,
                transferCount = count,
                inAmount = inAmount,
                outAmount = outAmount,
                emoji = account.emoji,
                iconType = account.iconType,
                color = account.color
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
                    emoji = account.emoji,
                    iconType = account.iconType,
                    color = account.color
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
                color = parseColor(null, goal.id)
            )
        }.sortedByDescending { it.progressPercent }
    }
}
