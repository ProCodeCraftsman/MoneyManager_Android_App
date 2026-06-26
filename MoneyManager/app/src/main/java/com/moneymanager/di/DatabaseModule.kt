package com.moneymanager.di

import android.content.Context
import androidx.room.Room
import com.moneymanager.data.MIGRATION_2_3
import com.moneymanager.data.MIGRATION_5_6
import com.moneymanager.data.MIGRATION_6_7
import com.moneymanager.data.MIGRATION_7_8
import com.moneymanager.data.MIGRATION_8_9
import com.moneymanager.data.MIGRATION_9_10
import com.moneymanager.data.MIGRATION_10_11
import com.moneymanager.data.MIGRATION_11_12
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
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
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
    fun providePeerContactDao(database: MoneyManagerDatabase): PeerContactDao {
        return database.peerContactDao()
    }

    @Provides
    fun provideAiConversationDao(database: MoneyManagerDatabase): AiConversationDao {
        return database.aiConversationDao()
    }

    @Provides
    fun provideMerchantCategoryMemoryDao(database: MoneyManagerDatabase): MerchantCategoryMemoryDao {
        return database.merchantCategoryMemoryDao()
    }
}