package com.moneymanager.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.moneymanager.data.dao.*
import com.moneymanager.data.entity.*

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        RecurringEntity::class,
        TemplateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MoneyManagerDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun recurringDao(): RecurringDao
    abstract fun templateDao(): TemplateDao
}