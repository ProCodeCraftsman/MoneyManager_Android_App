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

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE categories ADD COLUMN colorIndex INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE recurring ADD COLUMN endDate INTEGER")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create new table with updated schema
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `recurring_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `accountId` INTEGER NOT NULL, 
                `type` TEXT NOT NULL, 
                `amount` REAL NOT NULL, 
                `categoryId` INTEGER, 
                `subCategoryId` INTEGER, 
                `goalId` INTEGER, 
                `peerContactId` INTEGER, 
                `tagIds` TEXT NOT NULL DEFAULT '', 
                `description` TEXT NOT NULL DEFAULT '', 
                `note` TEXT NOT NULL, 
                `frequency` TEXT NOT NULL, 
                `startDate` INTEGER NOT NULL, 
                `nextDate` INTEGER NOT NULL, 
                `endDate` INTEGER, 
                `isActive` INTEGER NOT NULL, 
                `reminderEnabled` INTEGER NOT NULL, 
                `reminderDays` INTEGER NOT NULL, 
                `toAccountId` INTEGER, 
                `investmentPlatform` TEXT, 
                `receiptPath` TEXT, 
                `createdAt` INTEGER NOT NULL, 
                FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
        """.trimIndent())

        // Copy data from old table to new table
        // Note: investmentApp is renamed to investmentPlatform
        db.execSQL("""
            INSERT INTO `recurring_new` (
                id, accountId, type, amount, categoryId, subCategoryId, goalId, 
                note, frequency, startDate, nextDate, endDate, isActive, 
                reminderEnabled, reminderDays, investmentPlatform, createdAt
            )
            SELECT 
                id, accountId, type, amount, categoryId, subCategoryId, goalId, 
                note, frequency, startDate, nextDate, endDate, isActive, 
                reminderEnabled, reminderDays, investmentApp, createdAt 
            FROM recurring
        """.trimIndent())

        // Drop old table
        db.execSQL("DROP TABLE recurring")

        // Rename new table to original name
        db.execSQL("ALTER TABLE recurring_new RENAME TO recurring")

        // Re-create indices
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_accountId` ON `recurring` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_categoryId` ON `recurring` (`categoryId`)")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create `emis` table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `emis` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `accountId` INTEGER NOT NULL,
                `categoryId` INTEGER,
                `totalAmount` REAL NOT NULL,
                `tenureMonths` INTEGER NOT NULL,
                `monthlyAmount` REAL NOT NULL,
                `annualInterestRate` REAL NOT NULL DEFAULT 0.0,
                `isNoCost` INTEGER NOT NULL DEFAULT 0,
                `processingFee` REAL NOT NULL DEFAULT 0.0,
                `startDate` INTEGER NOT NULL,
                `status` TEXT NOT NULL DEFAULT 'ACTIVE',
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_emis_accountId` ON `emis` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_emis_categoryId` ON `emis` (`categoryId`)")

        // 2. Add EMI columns to transactions table
        db.execSQL("ALTER TABLE transactions ADD COLUMN emiId INTEGER")
        db.execSQL("ALTER TABLE transactions ADD COLUMN emiInstallmentNumber INTEGER")

        // 3. Migrate legacy `type = 'savings'` transactions: Ensure toAccountId is set for destination platform account
        val cursor = db.query("SELECT id, accountId, investmentPlatform FROM transactions WHERE type = 'savings' AND (toAccountId IS NULL OR toAccountId = 0)")
        while (cursor.moveToNext()) {
            val txId = cursor.getLong(0)
            val platformName = cursor.getString(2)
            if (!platformName.isNull_or_blank()) {
                // Check if platform account exists
                val accCursor = db.query("SELECT id FROM accounts WHERE name = ? AND type = 'savings'", arrayOf(platformName))
                var platformAccountId: Long? = null
                if (accCursor.moveToFirst()) {
                    platformAccountId = accCursor.getLong(0)
                }
                accCursor.close()

                // If not found, create new platform account
                if (platformAccountId == null) {
                    val now = System.currentTimeMillis()
                    db.execSQL(
                        "INSERT INTO accounts (name, type, initialBalance, balance, currency, emoji, iconType, color, createdAt, updatedAt) VALUES (?, 'savings', 0.0, 0.0, 'INR', '📈', 'emoji', '#2a6049', ?, ?)",
                        arrayOf(platformName, now, now)
                    )
                    val idCursor = db.query("SELECT last_insert_rowid()")
                    if (idCursor.moveToFirst()) {
                        platformAccountId = idCursor.getLong(0)
                    }
                    idCursor.close()
                }

                if (platformAccountId != null) {
                    db.execSQL("UPDATE transactions SET toAccountId = ? WHERE id = ?", arrayOf(platformAccountId, txId))
                }
            }
        }
        cursor.close()
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()

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
        EmiEntity::class,
    ],
    version = 14,
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
    abstract fun emiDao(): EmiDao
}
