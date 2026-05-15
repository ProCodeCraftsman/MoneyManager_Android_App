package com.moneymanager.domain.ai

enum class TransactionType(
    val id: String,
    val displayName: String,
    val promptHint: String,
    val requiresCategory: Boolean,
    val requiresPeer: Boolean
) {
    INCOME(
        id = "income",
        displayName = "Income",
        promptHint = "money received (salary, business income)",
        requiresCategory = true,
        requiresPeer = false
    ),
    EXPENSE(
        id = "expense",
        displayName = "Expense",
        promptHint = "money spent (bills, shopping, food)",
        requiresCategory = true,
        requiresPeer = false
    ),
    SAVINGS(
        id = "savings",
        displayName = "Savings",
        promptHint = "money saved or invested",
        requiresCategory = true,
        requiresPeer = false
    ),
    TRANSFER(
        id = "transfer",
        displayName = "Transfer",
        promptHint = "money moved between own accounts",
        requiresCategory = false,
        requiresPeer = false
    ),
    LEND(
        id = "lend",
        displayName = "Lend",
        promptHint = "money lent to someone",
        requiresCategory = false,
        requiresPeer = true
    ),
    RECEIVE(
        id = "receive",
        displayName = "Receive",
        promptHint = "money received back from someone you lent to",
        requiresCategory = false,
        requiresPeer = true
    ),
    BORROW(
        id = "borrow",
        displayName = "Borrow",
        promptHint = "money borrowed from someone",
        requiresCategory = false,
        requiresPeer = true
    ),
    REPAY(
        id = "repay",
        displayName = "Repay",
        promptHint = "repayment of borrowed money",
        requiresCategory = false,
        requiresPeer = true
    );

    companion object {
        fun allIds(): List<String> = entries.map { it.id }
        fun fromId(id: String): TransactionType? = entries.firstOrNull { it.id == id }
    }
}
