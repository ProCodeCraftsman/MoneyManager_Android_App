package com.moneymanager.data.repository

import android.content.Context
import com.moneymanager.data.dao.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

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

    private lateinit var repository: ExportRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = ExportRepository(
            mockContext, accountDao, transactionDao, categoryDao, 
            budgetDao, goalDao, tagDao, peerContactDao, recurringDao
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
}
