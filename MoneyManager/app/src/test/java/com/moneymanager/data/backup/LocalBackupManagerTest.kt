package com.moneymanager.data.backup

import android.content.Context
import com.moneymanager.data.repository.ExportRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class LocalBackupManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Mock
    lateinit var mockContext: Context

    @Mock
    lateinit var mockExportRepository: ExportRepository

    @Mock
    lateinit var mockDeviceBackupKeyStore: DeviceBackupKeyStore

    private val encryptionHelper = EncryptionHelper()

    private lateinit var localBackupManager: LocalBackupManager
    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.ROOT)

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(mockDeviceBackupKeyStore.getOrCreatePassphrase()).thenReturn("test-device-passphrase")
        localBackupManager = LocalBackupManager(mockContext, mockExportRepository, encryptionHelper, mockDeviceBackupKeyStore)
    }

    @Test
    fun `restoreLocalBackup decrypts current enc format`() = runBlocking {
        val dir = tempFolder.newFolder("backups")
        val plainJson = """{"accounts":[]}"""
        val encrypted = encryptionHelper.encrypt(plainJson.toByteArray(), "test-device-passphrase")
        val file = File(dir, "backup_20260101_120000.enc")
        file.writeBytes(encrypted)

        `when`(mockExportRepository.importFromJsonBytes(plainJson.toByteArray())).thenReturn(
            com.moneymanager.data.repository.ImportResult(success = true, message = "ok")
        )

        val result = localBackupManager.restoreLocalBackup(file)
        assertTrue(result.success)
    }

    @Test
    fun `restoreLocalBackup still reads legacy plaintext json backups`() = runBlocking {
        val dir = tempFolder.newFolder("backups")
        val plainJson = """{"accounts":[]}"""
        val file = File(dir, "backup_20230101_120000.json")
        file.writeText(plainJson)

        `when`(mockExportRepository.importFromJsonBytes(plainJson.toByteArray())).thenReturn(
            com.moneymanager.data.repository.ImportResult(success = true, message = "ok")
        )

        val result = localBackupManager.restoreLocalBackup(file)
        assertTrue(result.success)
    }

    @Test
    fun `parseDateFromFileName correctly parses valid filename`() {
        val fileName = "backup_20231027_120000.json"
        val expected = LocalDateTime.of(2023, 10, 27, 12, 0, 0)
        val result = localBackupManager.parseDateFromFileName(fileName)
        assertEquals(expected, result)
    }

    @Test
    fun `parseDateFromFileName correctly parses valid enc filename`() {
        val fileName = "backup_20231027_120000.enc"
        val expected = LocalDateTime.of(2023, 10, 27, 12, 0, 0)
        val result = localBackupManager.parseDateFromFileName(fileName)
        assertEquals(expected, result)
    }

    @Test
    fun `parseDateFromFileName returns null for malformed filename`() {
        assertNull(localBackupManager.parseDateFromFileName("not_a_backup.txt"))
        assertNull(localBackupManager.parseDateFromFileName("backup_invalid_date.json"))
    }

    @Test
    fun `cleanupOldBackups retains minimum 10 backups even if old`() {
        val dir = tempFolder.newFolder("backups")
        val oldDate = LocalDateTime.now().minusDays(40)
        
        // Create 12 old backups
        for (i in 1..12) {
            val date = oldDate.plusHours(i.toLong())
            val fileName = "backup_${date.format(formatter)}.json"
            File(dir, fileName).writeText("test")
        }

        localBackupManager.cleanupOldBackups(dir)

        val remainingFiles = dir.listFiles()
        assertEquals(10, remainingFiles?.size ?: 0)
    }

    @Test
    fun `cleanupOldBackups deletes files older than 30 days beyond the minimum 10`() {
        val dir = tempFolder.newFolder("backups")
        val now = LocalDateTime.now()
        val oldDate = now.minusDays(40)
        
        // Create 10 new backups (within 30 days)
        for (i in 1..10) {
            val date = now.minusMinutes(i.toLong())
            val fileName = "backup_${date.format(formatter)}.json"
            File(dir, fileName).writeText("test")
        }

        // Create 5 old backups (older than 30 days)
        for (i in 1..5) {
            val date = oldDate.minusDays(i.toLong())
            val fileName = "backup_${date.format(formatter)}.json"
            File(dir, fileName).writeText("test")
        }

        localBackupManager.cleanupOldBackups(dir)

        val remainingFiles = dir.listFiles()
        // Should keep the 10 new ones, and delete the 5 old ones
        assertEquals(10, remainingFiles?.size ?: 0)
        
        // Verify that all remaining files are the new ones
        remainingFiles?.forEach { file ->
            val date = localBackupManager.parseDateFromFileName(file.name)
            assertNotNull("Date should be parseable for $file", date)
            assertTrue("File $file should be within 30 days", date!!.isAfter(now.minusDays(31)))
        }
    }

    @Test
    fun `cleanupOldBackups skips malformed filenames`() {
        val dir = tempFolder.newFolder("backups")
        val malformedFile = File(dir, "manual_backup.json")
        malformedFile.writeText("test")
        
        val now = LocalDateTime.now()
        // Create 15 old valid backups
        val oldDate = now.minusDays(40)
        for (i in 1..15) {
            val date = oldDate.plusHours(i.toLong())
            val fileName = "backup_${date.format(formatter)}.json"
            File(dir, fileName).writeText("test")
        }

        localBackupManager.cleanupOldBackups(dir)

        val remainingFiles = dir.listFiles()
        // Should keep 10 valid ones + 1 malformed one = 11
        assertEquals(11, remainingFiles?.size ?: 0)
        assertTrue("Malformed file should still exist", malformedFile.exists())
    }
}
