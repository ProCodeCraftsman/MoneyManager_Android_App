package com.moneymanager.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.moneymanager.data.dao.*
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.EmiEntity
import com.moneymanager.data.entity.RecurringEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ExportRepositoryTest {

    @Mock lateinit var mockContext: Context
    @Mock lateinit var accountDao: AccountDao
    @Mock lateinit var transactionDao: TransactionDao
    @Mock lateinit var categoryDao: CategoryDao
    @Mock lateinit var budgetDao: BudgetDao
    @Mock lateinit var goalDao: GoalDao
    @Mock lateinit var tagDao: TagDao
    @Mock lateinit var peerContactDao: PeerContactDao
    @Mock lateinit var recurringDao: RecurringDao
    @Mock lateinit var emiDao: EmiDao

    private lateinit var repository: ExportRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = ExportRepository(
            mockContext, accountDao, transactionDao, categoryDao, 
            budgetDao, goalDao, tagDao, peerContactDao, recurringDao, emiDao
        )
    }

    @Test
    fun `getLatestTimestampFromBackup correctly identifies latest timestamp`() {
        val json = JSONObject().apply {
            put("accounts", JSONArray().put(JSONObject().apply { 
                put("createdAt", 1000L)
                put("updatedAt", 2000L) 
            }))
            put("transactions", JSONArray().put(JSONObject().apply { 
                put("createdAt", 3000L) 
            }))
        }.toString()
        
        val result = repository.getLatestTimestampFromBackup(json.toByteArray())
        assertEquals(3000L, result)
    }

    @Test
    fun `getLatestTimestampFromDb correctly aggregates latest timestamps`() = runBlocking {
        `when`(accountDao.getLatestTimestamp()).thenReturn(5000L)
        `when`(transactionDao.getLatestTimestamp()).thenReturn(6000L)
        `when`(categoryDao.getLatestTimestamp()).thenReturn(null)
        `when`(budgetDao.getLatestTimestamp()).thenReturn(2000L)
        `when`(goalDao.getLatestTimestamp()).thenReturn(0L)
        `when`(peerContactDao.getLatestTimestamp()).thenReturn(100L)
        `when`(recurringDao.getLatestTimestamp()).thenReturn(0L)
        
        val result = repository.getLatestTimestampFromDb()
        assertEquals(6000L, result)
    }

    /** Stubs every DAO's getAll*() flow to empty so exportToJsonBytes()/exportAllCsv() don't NPE on unrelated entities. */
    private fun stubEmptyExportSources() {
        `when`(accountDao.getAllAccounts()).thenReturn(flowOf(emptyList()))
        `when`(transactionDao.getAllTransactions()).thenReturn(flowOf(emptyList()))
        `when`(categoryDao.getAllCategoriesWithArchived()).thenReturn(flowOf(emptyList()))
        `when`(categoryDao.getAllCategories()).thenReturn(flowOf(emptyList()))
        `when`(budgetDao.getAllBudgets()).thenReturn(flowOf(emptyList()))
        `when`(goalDao.getAllGoals()).thenReturn(flowOf(emptyList()))
        `when`(tagDao.getAllTags()).thenReturn(flowOf(emptyList()))
        `when`(peerContactDao.getAllPeers()).thenReturn(flowOf(emptyList()))
        `when`(recurringDao.getAllRecurring()).thenReturn(flowOf(emptyList()))
        `when`(emiDao.getAllEmis()).thenReturn(flowOf(emptyList()))
    }

    @Test
    fun `recurring JSON round-trip preserves peer, tags, description, endDate, toAccountId, receiptPath`() = runBlocking {
        stubEmptyExportSources()
        val original = RecurringEntity(
            id = 7L,
            accountId = 1L,
            type = "lend",
            amount = 500.0,
            peerContactId = 42L,
            tagIds = "1,2,3",
            description = "Lent to Alex",
            note = "for rent",
            frequency = "monthly",
            nextDate = 1_700_000_000_000L,
            endDate = 1_800_000_000_000L,
            toAccountId = 9L,
            receiptPath = "/receipts/lend.jpg",
        )
        `when`(recurringDao.getAllRecurring()).thenReturn(flowOf(listOf(original)))
        var imported: RecurringEntity? = null
        `when`(recurringDao.insertRecurring(any())).thenAnswer { invocation ->
            imported = invocation.arguments[0] as RecurringEntity
            1L
        }

        val bytes = repository.exportToJsonBytes()
        repository.importFromJsonBytes(bytes)

        val result = requireNotNull(imported) { "insertRecurring was never called" }
        assertEquals(original.peerContactId, result.peerContactId)
        assertEquals(original.tagIds, result.tagIds)
        assertEquals(original.description, result.description)
        assertEquals(original.endDate, result.endDate)
        assertEquals(original.toAccountId, result.toAccountId)
        assertEquals(original.receiptPath, result.receiptPath)
    }

    @Test
    fun `account JSON round-trip preserves isArchived and archivedAt`() = runBlocking {
        stubEmptyExportSources()
        val original = AccountEntity(
            id = 3L,
            name = "Old Wallet",
            type = "cash",
            balance = 0.0,
            isArchived = true,
            archivedAt = 1_650_000_000_000L,
        )
        `when`(accountDao.getAllAccounts()).thenReturn(flowOf(listOf(original)))
        var imported: AccountEntity? = null
        `when`(accountDao.insertAccount(any())).thenAnswer { invocation ->
            imported = invocation.arguments[0] as AccountEntity
            1L
        }

        val bytes = repository.exportToJsonBytes()
        repository.importFromJsonBytes(bytes)

        val result = requireNotNull(imported) { "insertAccount was never called" }
        assertEquals(true, result.isArchived)
        assertEquals(original.archivedAt, result.archivedAt)
    }

    @Test
    fun `CSV ALL bundle export and import includes EMIs`() = runBlocking {
        stubEmptyExportSources()
        val emi = EmiEntity(
            id = 5L,
            title = "Laptop EMI",
            accountId = 1L,
            totalAmount = 60000.0,
            tenureMonths = 12,
            monthlyAmount = 5000.0,
            startDate = 1_700_000_000_000L,
        )
        `when`(emiDao.getAllEmis()).thenReturn(flowOf(listOf(emi)))
        `when`(mockContext.contentResolver).thenReturn(org.mockito.Mockito.mock(ContentResolver::class.java))

        val outputStream = ByteArrayOutputStream()
        val uri = org.mockito.Mockito.mock(Uri::class.java)
        `when`(mockContext.contentResolver.openOutputStream(uri)).thenReturn(outputStream)

        repository.exportToCsv(uri, ExportType.ALL)
        val csvText = outputStream.toString(Charsets.UTF_8.name())
        assertTrue("Expected '# EMIS' section in ALL CSV export", csvText.contains("# EMIS"))
        assertTrue("Expected EMI title to be present in ALL CSV export", csvText.contains("Laptop EMI"))

        `when`(mockContext.contentResolver.openInputStream(uri)).thenReturn(ByteArrayInputStream(csvText.toByteArray()))
        val result = repository.importFromCsv(uri, ExportType.ALL)

        assertTrue("Expected at least one EMI imported from ALL CSV bundle", result.emisImported >= 1)
    }
}
