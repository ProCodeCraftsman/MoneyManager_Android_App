package com.moneymanager.data.backup

import android.content.Context
import com.moneymanager.data.repository.ExportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exportRepository: ExportRepository
) {
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * Performs a local JSON backup.
     * Saves the file to the app's external files directory under 'backups'.
     * Keeps only the last 30 days of backups.
     */
    suspend fun performLocalBackup(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val backupDir = File(context.getExternalFilesDir(null), "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            // 1. Export data to JSON
            val jsonBytes = exportRepository.exportToJsonBytes()
            val fileName = "backup_${dateFormat.format(Date())}.json"
            val backupFile = File(backupDir, fileName)
            
            backupFile.writeBytes(jsonBytes)

            // 2. Cleanup old backups (older than 30 days)
            cleanupOldBackups(backupDir)

            Result.success(backupFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanupOldBackups(directory: File) {
        val now = System.currentTimeMillis()
        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
        
        directory.listFiles { file -> file.isFile && file.name.startsWith("backup_") && file.name.endsWith(".json") }
            ?.forEach { file ->
                if (now - file.lastModified() > thirtyDaysInMillis) {
                    file.delete()
                }
            }
    }
    
    fun getBackupDirectory(): File {
        return File(context.getExternalFilesDir(null), "backups")
    }
}
