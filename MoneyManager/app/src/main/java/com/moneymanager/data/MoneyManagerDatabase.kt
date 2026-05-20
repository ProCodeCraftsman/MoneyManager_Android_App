package com.moneymanager.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.moneymanager.data.dao.*
import com.moneymanager.data.entity.*
import com.moneymanager.data.entity.MerchantCategoryMemoryEntity

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN isSplitParent INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN isSplitChild INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN parentTransactionId INTEGER")
        db.execSQL("ALTER TABLE transactions ADD COLUMN isTransfer INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN toAccountId INTEGER")
        db.execSQL("ALTER TABLE transactions ADD COLUMN investmentPlatform TEXT")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE categories ADD COLUMN iconType TEXT NOT NULL DEFAULT 'emoji'")
        db.execSQL("ALTER TABLE accounts ADD COLUMN iconType TEXT NOT NULL DEFAULT 'emoji'")
        db.execSQL("ALTER TABLE goals ADD COLUMN iconType TEXT NOT NULL DEFAULT 'emoji'")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create peer_contacts table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS peer_contacts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                displayName TEXT NOT NULL,
                phoneNumber TEXT NOT NULL,
                photoUri TEXT,
                totalGiven REAL NOT NULL DEFAULT 0.0,
                totalReceived REAL NOT NULL DEFAULT 0.0,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """.trimIndent())
        
        // Add peerContactId to transactions
        db.execSQL("ALTER TABLE transactions ADD COLUMN peerContactId INTEGER")
        
        // Add peerContactId to accounts
        db.execSQL("ALTER TABLE accounts ADD COLUMN peerContactId INTEGER")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS merchant_category_memory (
                merchantKey TEXT NOT NULL PRIMARY KEY,
                categoryId INTEGER NOT NULL,
                categoryName TEXT NOT NULL,
                typeId TEXT,
                hitCount INTEGER NOT NULL DEFAULT 1,
                lastUsedAt INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS ai_conversations (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                rawText TEXT NOT NULL,
                sourceType TEXT NOT NULL,
                sourceSender TEXT,
                prompt TEXT NOT NULL,
                response TEXT NOT NULL,
                parsedDraftJson TEXT,
                success INTEGER NOT NULL,
                errorMessage TEXT,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE peer_contacts ADD COLUMN lookupKey TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE peer_contacts ADD COLUMN contactDeleted INTEGER NOT NULL DEFAULT 0")
        // email and description may already exist from prior schema; catch duplicates gracefully
        try { db.execSQL("ALTER TABLE peer_contacts ADD COLUMN email TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
        try { db.execSQL("ALTER TABLE peer_contacts ADD COLUMN description TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
    }
}

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        RecurringEntity::class,
        PeerContact::class,
        AiConversationEntity::class,
        MerchantCategoryMemoryEntity::class,
    ],
    version = 10,
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
    abstract fun peerContactDao(): PeerContactDao
    abstract fun aiConversationDao(): AiConversationDao
    abstract fun merchantCategoryMemoryDao(): MerchantCategoryMemoryDao
}
