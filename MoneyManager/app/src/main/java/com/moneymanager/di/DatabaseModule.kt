package com.moneymanager.di

import android.content.Context
import androidx.room.Room
import com.moneymanager.data.MoneyManagerDatabase
import com.moneymanager.data.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MoneyManagerDatabase {
        return Room.databaseBuilder(
            context,
            MoneyManagerDatabase::class.java,
            "moneymanager.db"
        ).fallbackToDestructiveMigration(true).build()
    }

    @Provides
    fun provideAccountDao(database: MoneyManagerDatabase): AccountDao {
        return database.accountDao()
    }

    @Provides
    fun provideTransactionDao(database: MoneyManagerDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideCategoryDao(database: MoneyManagerDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideTagDao(database: MoneyManagerDatabase): TagDao {
        return database.tagDao()
    }

    @Provides
    fun provideBudgetDao(database: MoneyManagerDatabase): BudgetDao {
        return database.budgetDao()
    }

    @Provides
    fun provideGoalDao(database: MoneyManagerDatabase): GoalDao {
        return database.goalDao()
    }

    @Provides
    fun provideRecurringDao(database: MoneyManagerDatabase): RecurringDao {
        return database.recurringDao()
    }

    @Provides
    fun provideTemplateDao(database: MoneyManagerDatabase): TemplateDao {
        return database.templateDao()
    }
}