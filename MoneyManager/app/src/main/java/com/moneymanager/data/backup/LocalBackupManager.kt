package com.moneymanager.data.backup

import android.content.Context
import com.moneymanager.data.repository.ExportRepository
import com.moneymanager.data.repository.ImportResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class LocalBackupItem(
    val file: File,
    val fileName: String,
    val formattedDate: String,
    val sizeBytes: Long,
    val timestamp: Long
)

@Singleton
class LocalBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exportRepository: ExportRepository
) {
    private val fileNameFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.ROOT)

    /**
     * Performs a local JSON backup.
     * Saves the file to the app's external files directory under 'backups'.
     * Keeps at least 10 recent backups, and deletes others older than 30 days.
     */
    suspend fun performLocalBackup(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val backupDir = File(context.getExternalFilesDir(null), "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            // 1. Export data to JSON
            val jsonBytes = exportRepository.exportToJsonBytes()
            val timestamp = LocalDateTime.now().format(fileNameFormatter)
            val fileName = "backup_${timestamp}.json"
            val backupFile = File(backupDir, fileName)
            
            backupFile.writeBytes(jsonBytes)

            // 2. Cleanup old backups
            cleanupOldBackups(backupDir)

            Result.success(backupFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLocalBackups(): List<LocalBackupItem> = withContext(Dispatchers.IO) {
        val backupDir = getBackupDirectory()
        if (!backupDir.exists()) return@withContext emptyList()

        val files = backupDir.listFiles { file ->
            file.isFile && file.name.startsWith("backup_") && file.name.endsWith(".json")
        } ?: return@withContext emptyList()

        val dateFormatter = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        files.map { file ->
            val millis = file.lastModified()
            LocalBackupItem(
                file = file,
                fileName = file.name,
                formattedDate = dateFormatter.format(java.util.Date(millis)),
                sizeBytes = file.length(),
                timestamp = millis
            )
        }.sortedByDescending { it.timestamp }
    }

    suspend fun restoreLocalBackup(file: File): ImportResult = withContext(Dispatchers.IO) {
        val bytes = file.readBytes()
        exportRepository.importFromJsonBytes(bytes)
    }

    suspend fun deleteLocalBackup(file: File): Boolean = withContext(Dispatchers.IO) {
        if (file.exists()) file.delete() else false
    }

    @androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PRIVATE)
    internal fun cleanupOldBackups(directory: File) {
        val minKeepCount = 10
        val retentionDays = 30L
        val now = LocalDateTime.now()

        val files = directory.listFiles { file -> 
            file.isFile && file.name.startsWith("backup_") && file.name.endsWith(".json") 
        } ?: return

        // Parse date from filename and sort by date descending (newest first)
        val backupsWithDates = files.mapNotNull { file ->
            parseDateFromFileName(file.name)?.let { date -> file to date }
        }.sortedByDescending { it.second }

        // Always keep the N most recent backups
        if (backupsWithDates.size <= minKeepCount) return

        // For the rest, delete if older than retention period
        val olderBackups = backupsWithDates.drop(minKeepCount)
        val cutoffDate = now.minusDays(retentionDays)

        olderBackups.forEach { (file, date) ->
            if (date.isBefore(cutoffDate)) {
                file.delete()
            }
        }
    }

    @androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PRIVATE)
    internal fun parseDateFromFileName(fileName: String): LocalDateTime? {
        return try {
            // Expected format: backup_yyyyMMdd_HHmmss.json
            val dateString = fileName.substringAfter("backup_").substringBefore(".json")
            LocalDateTime.parse(dateString, fileNameFormatter)
        } catch (e: Exception) {
            // Malformed filenames are skipped from deletion
            null
        }
    }
    
    fun getBackupDirectory(): File {
        return File(context.getExternalFilesDir(null), "backups")
    }
}
