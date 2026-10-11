package com.moneymanager.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.moneymanager.data.dao.*
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CapturedMessageEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.EmiEntity
import com.moneymanager.data.entity.MerchantCategoryMemoryEntity
import com.moneymanager.data.entity.RecurringEntity
import com.moneymanager.data.preferences.PreferencesManager
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
    @Mock lateinit var preferencesManager: PreferencesManager
    @Mock lateinit var merchantMemoryDao: MerchantCategoryMemoryDao
    @Mock lateinit var capturedMessageDao: CapturedMessageDao

    private lateinit var repository: ExportRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = ExportRepository(
            mockContext, accountDao, transactionDao, categoryDao,
            budgetDao, goalDao, tagDao, peerContactDao, recurringDao, emiDao, preferencesManager,
            merchantMemoryDao, capturedMessageDao,
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
        runBlocking {
            `when`(preferencesManager.getDefaultAccounts()).thenReturn(emptyMap())
            `when`(preferencesManager.getSmsAccountLinks()).thenReturn(emptyMap())
            `when`(merchantMemoryDao.getAll()).thenReturn(emptyList())
            `when`(capturedMessageDao.getAll()).thenReturn(emptyList())
        }
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

    @Test
    fun `JSON export includes default accounts under settings`() = runBlocking {
        stubEmptyExportSources()
        `when`(preferencesManager.getDefaultAccounts()).thenReturn(mapOf("expense" to 3L, "transfer_to" to 4L))

        val json = JSONObject(String(repository.exportToJsonBytes(), Charsets.UTF_8))

        val defaults = json.getJSONObject("settings").getJSONObject("defaultAccounts")
        assertEquals(3L, defaults.getLong("expense"))
        assertEquals(4L, defaults.getLong("transfer_to"))
    }

    @Test
    fun `JSON import restores valid default accounts and skips unknown slots and missing accounts`() = runBlocking {
        stubEmptyExportSources()
        `when`(accountDao.getAccountById(3L)).thenReturn(AccountEntity(id = 3L, name = "Bank", type = "bank"))
        `when`(accountDao.getAccountById(99L)).thenReturn(null)
        val json = JSONObject().apply {
            put("settings", JSONObject().apply {
                put("defaultAccounts", JSONObject().apply {
                    put("expense", 3L)
                    put("income", 99L)
                    put("bogus_slot", 3L)
                })
            })
        }.toString()

        val result = repository.importFromJsonBytes(json.toByteArray())

        assertTrue(result.success)
        org.mockito.Mockito.verify(preferencesManager).setDefaultAccount("expense", 3L)
        org.mockito.Mockito.verify(preferencesManager, org.mockito.Mockito.never()).setDefaultAccount("income", 99L)
        org.mockito.Mockito.verify(preferencesManager, org.mockito.Mockito.never()).setDefaultAccount("bogus_slot", 3L)
    }

    @Test
    fun `JSON import of an older backup without settings still succeeds and leaves defaults alone`() = runBlocking {
        stubEmptyExportSources()

        val result = repository.importFromJsonBytes("""{"accounts":[],"transactions":[]}""".toByteArray())

        assertTrue(result.success)
        org.mockito.Mockito.verify(preferencesManager, org.mockito.Mockito.never()).setDefaultAccount(any(), any())
    }

    @Test
    fun `CSV ALL bundle carries default accounts by name`() = runBlocking {
        stubEmptyExportSources()
        val bank = AccountEntity(id = 7L, name = "HDFC \"Main\", Savings", type = "bank")
        `when`(accountDao.getAllAccounts()).thenReturn(flowOf(listOf(bank)))
        `when`(preferencesManager.getDefaultAccounts()).thenReturn(mapOf("expense" to 7L, "income" to 404L))
        `when`(mockContext.contentResolver).thenReturn(org.mockito.Mockito.mock(ContentResolver::class.java))
        val outputStream = ByteArrayOutputStream()
        val uri = org.mockito.Mockito.mock(Uri::class.java)
        `when`(mockContext.contentResolver.openOutputStream(uri)).thenReturn(outputStream)

        repository.exportToCsv(uri, ExportType.ALL)
        val csvText = outputStream.toString(Charsets.UTF_8.name())
        assertTrue(csvText.contains("# DEFAULT_ACCOUNTS"))
        assertTrue("Defaults whose account no longer exists are dropped", !csvText.contains("income,"))

        `when`(mockContext.contentResolver.openInputStream(uri)).thenReturn(ByteArrayInputStream(csvText.toByteArray()))
        val result = repository.importFromCsv(uri, ExportType.ALL)

        assertTrue(result.success)
        org.mockito.Mockito.verify(preferencesManager).setDefaultAccount("expense", 7L)
    }

    private val groceries = CategoryEntity(id = 11L, name = "Groceries", type = "expense")
    private val ledgerSaved = CapturedMessageEntity(
        id = 5L, messageHash = "abc123", refHash = "ref456", amount = 250.0, messageDate = 1_700_000_000_000L,
        sourceKey = "hdfc|BANK_ACCOUNT|1234", status = CapturedMessageEntity.STATUS_SAVED, transactionId = 77L,
    )
    private val ledgerPending = CapturedMessageEntity(
        id = 6L, messageHash = "def789", amount = 99.5, messageDate = 1_700_000_100_000L,
        status = CapturedMessageEntity.STATUS_PENDING, draftJson = """{"amount":99.5}""",
    )

    @Test
    fun `JSON round-trip restores sms links, merchant memory and the captured ledger`() = runBlocking {
        stubEmptyExportSources()
        `when`(preferencesManager.getSmsAccountLinks()).thenReturn(mapOf("hdfc|BANK_ACCOUNT|1234" to 3L, "gone|OTHER|" to 404L))
        `when`(accountDao.getAccountById(3L)).thenReturn(AccountEntity(id = 3L, name = "Bank", type = "bank"))
        `when`(merchantMemoryDao.getAll()).thenReturn(listOf(
            MerchantCategoryMemoryEntity("swiggy", 11L, "Groceries", "expense", 4, 1_700_000_000_000L),
            MerchantCategoryMemoryEntity("orphan", 99L, "Deleted", "expense"),
        ))
        `when`(categoryDao.getCategoryById(11L)).thenReturn(groceries)
        `when`(capturedMessageDao.getAll()).thenReturn(listOf(ledgerSaved, ledgerPending))

        val bytes = repository.exportToJsonBytes()
        val result = repository.importFromJsonBytes(bytes)

        assertTrue(result.success)
        org.mockito.Mockito.verify(preferencesManager).setSmsAccountLink("hdfc|BANK_ACCOUNT|1234", 3L)
        org.mockito.Mockito.verify(preferencesManager, org.mockito.Mockito.never()).setSmsAccountLink("gone|OTHER|", 404L)
        val memory = org.mockito.kotlin.argumentCaptor<MerchantCategoryMemoryEntity>()
        org.mockito.Mockito.verify(merchantMemoryDao).upsert(memory.capture())
        assertEquals("swiggy", memory.firstValue.merchantKey)
        assertEquals(4, memory.firstValue.hitCount)
        val ledger = org.mockito.kotlin.argumentCaptor<CapturedMessageEntity>()
        org.mockito.Mockito.verify(capturedMessageDao, org.mockito.Mockito.times(2)).upsert(ledger.capture())
        assertEquals(ledgerSaved, ledger.firstValue)
        assertEquals(ledgerPending, ledger.secondValue)
    }

    @Test
    fun `JSON import of an older backup without learned data leaves it alone`() = runBlocking {
        stubEmptyExportSources()

        repository.importFromJsonBytes("""{"accounts":[],"transactions":[]}""".toByteArray())

        org.mockito.Mockito.verify(merchantMemoryDao, org.mockito.Mockito.never()).upsert(any())
        org.mockito.Mockito.verify(capturedMessageDao, org.mockito.Mockito.never()).upsert(any())
        org.mockito.Mockito.verify(preferencesManager, org.mockito.Mockito.never()).setSmsAccountLink(any(), any())
    }

    @Test
    fun `CSV ALL bundle carries sms links by name, merchant memory by category and decided ledger rows only`() = runBlocking {
        stubEmptyExportSources()
        val bank = AccountEntity(id = 7L, name = "HDFC \"Main\", Savings", type = "bank")
        `when`(accountDao.getAllAccounts()).thenReturn(flowOf(listOf(bank)))
        `when`(preferencesManager.getSmsAccountLinks()).thenReturn(mapOf("hdfc|BANK_ACCOUNT|1234" to 7L, "gone|OTHER|" to 404L))
        `when`(categoryDao.getAllCategoriesWithArchived()).thenReturn(flowOf(listOf(groceries)))
        `when`(categoryDao.getCategoryByName("Groceries", "expense")).thenReturn(groceries)
        `when`(merchantMemoryDao.getAll()).thenReturn(listOf(
            MerchantCategoryMemoryEntity("swiggy", 11L, "Groceries", "expense", 4, 1_700_000_000_000L),
            MerchantCategoryMemoryEntity("orphan", 99L, "Deleted", "expense"),
        ))
        `when`(capturedMessageDao.getAll()).thenReturn(listOf(ledgerSaved, ledgerPending))
        `when`(mockContext.contentResolver).thenReturn(org.mockito.Mockito.mock(ContentResolver::class.java))
        val outputStream = ByteArrayOutputStream()
        val uri = org.mockito.Mockito.mock(Uri::class.java)
        `when`(mockContext.contentResolver.openOutputStream(uri)).thenReturn(outputStream)

        repository.exportToCsv(uri, ExportType.ALL)
        val csvText = outputStream.toString(Charsets.UTF_8.name())
        assertTrue(csvText.contains("# SMS_ACCOUNT_LINKS") && csvText.contains("# MERCHANT_MEMORY") && csvText.contains("# CAPTURED_MESSAGES"))
        assertTrue("A link to a vanished account is dropped", !csvText.contains("gone|OTHER"))
        assertTrue("Memory pointing at a vanished category is dropped", !csvText.contains("orphan"))
        assertTrue("Pending drafts are not in the CSV bundle", !csvText.contains("def789"))

        `when`(mockContext.contentResolver.openInputStream(uri)).thenReturn(ByteArrayInputStream(csvText.toByteArray()))
        val result = repository.importFromCsv(uri, ExportType.ALL)

        assertTrue(result.success)
        org.mockito.Mockito.verify(preferencesManager).setSmsAccountLink("hdfc|BANK_ACCOUNT|1234", 7L)
        val memory = org.mockito.kotlin.argumentCaptor<MerchantCategoryMemoryEntity>()
        org.mockito.Mockito.verify(merchantMemoryDao).upsert(memory.capture())
        assertEquals(11L, memory.firstValue.categoryId)
        assertEquals(4, memory.firstValue.hitCount)
        val ledger = org.mockito.kotlin.argumentCaptor<CapturedMessageEntity>()
        org.mockito.Mockito.verify(capturedMessageDao).insertIfNew(ledger.capture())
        assertEquals("abc123", ledger.firstValue.messageHash)
        assertEquals("ref456", ledger.firstValue.refHash)
        assertEquals(250.0, ledger.firstValue.amount!!, 0.0)
        assertEquals(CapturedMessageEntity.STATUS_SAVED, ledger.firstValue.status)
        assertEquals(null, ledger.firstValue.transactionId)
    }

    @Test
    fun `CSV ALL bundle without a defaults section imports cleanly`() = runBlocking {
        stubEmptyExportSources()
        `when`(mockContext.contentResolver).thenReturn(org.mockito.Mockito.mock(ContentResolver::class.java))
        val uri = org.mockito.Mockito.mock(Uri::class.java)
        val oldCsv = "# ACCOUNTS\nid,name,type\n"
        `when`(mockContext.contentResolver.openInputStream(uri)).thenReturn(ByteArrayInputStream(oldCsv.toByteArray()))

        val result = repository.importFromCsv(uri, ExportType.ALL)

        assertTrue(result.success)
        org.mockito.Mockito.verify(preferencesManager, org.mockito.Mockito.never()).setDefaultAccount(any(), any())
    }
}
