package com.moneymanager.data.repository

import com.moneymanager.data.dao.MerchantCategoryMemoryDao
import com.moneymanager.data.dao.TransactionDao
import com.moneymanager.data.entity.MerchantCategoryMemoryEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class MerchantCategoryMemoryRepositoryTest {

    private lateinit var dao: MerchantCategoryMemoryDao
    private lateinit var repo: MerchantCategoryMemoryRepository

    @Before
    fun setUp() {
        dao = mock()
        repo = MerchantCategoryMemoryRepository(dao, mock<TransactionDao>())
    }

    @Test
    fun `lookup reads the canonical key first`() = runTest {
        val entry = MerchantCategoryMemoryEntity("swiggy", 1L, "Food", "expense")
        whenever(dao.findByKey("swiggy")).thenReturn(entry)

        assertEquals(entry, repo.lookup("POS 123456 SWIGGY BLR"))
    }

    @Test
    fun `lookup falls back to the legacy key written before the normaliser`() = runTest {
        val legacy = MerchantCategoryMemoryEntity("pos123456swiggyblr", 1L, "Food", "expense")
        whenever(dao.findByKey("pos123456swiggyblr")).thenReturn(legacy)

        assertEquals(legacy, repo.lookup("POS 123456 SWIGGY BLR"))
    }

    @Test
    fun `unknown merchant gives nothing and too short keys never query`() = runTest {
        assertNull(repo.lookup("Unknown Store"))
        assertNull(repo.lookup("AB"))
        verify(dao, never()).findByKey("ab")
    }

    @Test
    fun `record writes the canonical key`() = runTest {
        whenever(dao.findByKey("swiggy")).thenReturn(null)

        repo.record("POS 123456 SWIGGY BLR", 1L, "Food", "expense")

        val saved = argumentCaptor<MerchantCategoryMemoryEntity>()
        verify(dao).upsert(saved.capture())
        assertEquals("swiggy", saved.firstValue.merchantKey)
    }
}
