package com.moneymanager.data

import android.content.Context
import android.util.Log
import net.sqlcipher.database.SQLiteDatabase as SQLCipherDatabase
import java.io.File

/**
 * One-time, in-place migration of the plaintext Room database to a SQLCipher-encrypted one.
 *
 * Runs before Room ever opens the database. Safe by construction: the encrypted copy is
 * written to a separate temp file and integrity-checked (table list + total row count must
 * match the original) before the original plaintext file is touched. If anything goes wrong,
 * the original file is left untouched and migration is retried on the next launch - the app
 * keeps working on the plaintext DB in the meantime rather than being bricked. Once the
 * encrypted copy passes its integrity check, the original is kept (not deleted) as
 * "<name>.premigration.bak" so a real device with real data has a manual recovery path even
 * after a successful-looking migration - delete that file once the app has been confirmed
 * stable on the encrypted DB.
 */
object DatabaseEncryptionMigrator {
    private const val TAG = "DbEncryptionMigrator"

    /** Encrypts [dbFileName] in place with [passphrase] if it exists and isn't already encrypted. */
    fun migrateToEncryptedIfNeeded(context: Context, dbFileName: String, passphrase: String) {
        val dbFile = context.getDatabasePath(dbFileName)
        if (!dbFile.exists()) return

        SQLCipherDatabase.loadLibs(context)

        if (canOpenWithPassphrase(dbFile, passphrase)) {
            return // already encrypted with this passphrase
        }

        val tempEncrypted = File(dbFile.parentFile, "$dbFileName.migrating")
        deleteDbFiles(tempEncrypted)

        try {
            val originalCheck = tableRowCounts(dbFile, passphrase = "")
            exportToEncrypted(dbFile, tempEncrypted, passphrase)
            val migratedCheck = tableRowCounts(tempEncrypted, passphrase)

            require(originalCheck == migratedCheck) {
                "Row-count mismatch after encryption: original=$originalCheck migrated=$migratedCheck"
            }

            replaceWithEncrypted(dbFile, tempEncrypted)
            Log.i(TAG, "Database encryption migration succeeded")
        } catch (e: Exception) {
            Log.e(TAG, "Database encryption migration failed, will retry on next launch", e)
            deleteDbFiles(tempEncrypted)
        }
    }

    private fun canOpenWithPassphrase(dbFile: File, passphrase: String): Boolean = try {
        SQLCipherDatabase.openDatabase(dbFile.path, passphrase, null, SQLCipherDatabase.OPEN_READONLY)
            .close()
        true
    } catch (e: Exception) {
        false
    }

    private fun exportToEncrypted(source: File, dest: File, passphrase: String) {
        // This library's ATTACH DATABASE cannot create a nonexistent file (a documented
        // upstream limitation), so the destination must be pre-created. Android's
        // SQLiteDatabase auto-creates an android_metadata table in it, which then collides
        // with the source's own android_metadata table during sqlcipher_export - drop it first.
        SQLCipherDatabase.openOrCreateDatabase(dest.path, passphrase, null).use { destDb ->
            destDb.execSQL("DROP TABLE IF EXISTS android_metadata;")
        }

        val sourceDb = SQLCipherDatabase.openDatabase(source.path, "", null, SQLCipherDatabase.OPEN_READWRITE)
        try {
            sourceDb.execSQL(
                "ATTACH DATABASE ? AS encrypted KEY ?;",
                arrayOf<Any>(dest.path, passphrase)
            )
            sourceDb.rawExecSQL("SELECT sqlcipher_export('encrypted');")
            sourceDb.execSQL("DETACH DATABASE encrypted;")
        } finally {
            sourceDb.close()
        }
    }

    /** Maps table name -> row count, for every user table, as an integrity fingerprint. */
    private fun tableRowCounts(dbFile: File, passphrase: String): Map<String, Long> {
        val db = SQLCipherDatabase.openDatabase(dbFile.path, passphrase, null, SQLCipherDatabase.OPEN_READONLY)
        try {
            val tableNames = mutableListOf<String>()
            db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_metadata'",
                null
            ).use { cursor ->
                while (cursor.moveToNext()) tableNames.add(cursor.getString(0))
            }
            return tableNames.associateWith { name ->
                db.rawQuery("SELECT count(*) FROM \"$name\"", null).use { cursor ->
                    cursor.moveToFirst()
                    cursor.getLong(0)
                }
            }
        } finally {
            db.close()
        }
    }

    private fun replaceWithEncrypted(dbFile: File, tempEncrypted: File) {
        // Drop the plaintext WAL/SHM siblings only - their contents are already checkpointed
        // into dbFile itself, which is kept as a recovery backup rather than deleted.
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
        File(dbFile.path + "-journal").delete()

        val backup = File(dbFile.parentFile, "${dbFile.name}.premigration.bak")
        backup.delete()
        require(dbFile.renameTo(backup)) { "Failed to move original DB to backup path" }

        if (!tempEncrypted.renameTo(dbFile)) {
            check(backup.renameTo(dbFile)) {
                "Failed to move encrypted DB into place, and failed to roll back the original - " +
                    "original is at ${backup.path}"
            }
            error("Failed to move encrypted DB into place; rolled back to the original plaintext DB")
        }
    }

    private fun deleteDbFiles(dbFile: File) {
        dbFile.delete()
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
        File(dbFile.path + "-journal").delete()
    }
}
