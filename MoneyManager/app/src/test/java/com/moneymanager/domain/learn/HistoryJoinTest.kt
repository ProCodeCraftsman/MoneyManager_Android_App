package com.moneymanager.domain.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryJoinTest {

    private val day = 1_700_000_000_000L
    private val hdfc = "hdfc|BANK_ACCOUNT|1234"

    private fun spend(
        dayOffset: Int = 0,
        amount: Double = 250.0,
        typeId: String = "expense",
        merchant: String? = "SWIGGY",
        source: String? = hdfc,
    ) = SmsSpend(day + dayOffset * HistoryJoin.DAY_MS, amount, typeId, merchant, source)

    private fun row(
        id: Long,
        dayOffset: Int = 0,
        amount: Double = 250.0,
        typeId: String = "expense",
        categoryId: Long? = 7L,
        categoryName: String? = "Food",
        accountId: Long = 1L,
        accountName: String = "HDFC Bank",
        accountType: String = "bank",
    ) = SavedRow(id, typeId, amount, day + dayOffset * HistoryJoin.DAY_MS, categoryId, categoryName, accountId, accountName, accountType)

    @Test
    fun `two unique matches teach the merchant its category and the source its account`() {
        val spends = listOf(spend(0), spend(3))
        val saved = listOf(row(1, 0), row(2, 3, categoryId = 7L))

        val labels = HistoryJoin.join(spends, saved)

        val merchant = labels.merchants.single()
        assertEquals("swiggy", merchant.merchantKey)
        assertEquals("Food", merchant.categoryName)
        assertEquals(2, merchant.count)
        assertEquals(2, merchant.total)
        assertEquals(1L, labels.sources.single().accountId)
        assertEquals(2, labels.sources.single().count)
    }

    @Test
    fun `a single occurrence is not enough to label`() {
        val labels = HistoryJoin.join(listOf(spend(0)), listOf(row(1, 0)))

        assertTrue(labels.merchants.isEmpty())
        assertTrue(labels.sources.isEmpty())
        assertEquals(JoinCoverage(matched = 1, total = 1), labels.coverage)
    }

    @Test
    fun `a spend with two saved candidates is skipped`() {
        // Two identical tea purchases the same day: the SMS cannot say which saved row it is.
        val labels = HistoryJoin.join(
            listOf(spend(0, amount = 20.0, merchant = "CHAI POINT"), spend(0, amount = 20.0, merchant = "CHAI POINT")),
            listOf(row(1, 0, amount = 20.0, categoryName = "Tea"), row(2, 0, amount = 20.0, categoryName = "Tea")),
        )

        assertTrue(labels.merchants.isEmpty())
        assertEquals(0, labels.coverage.matched)
    }

    @Test
    fun `a saved row claimed by two spends is dropped from both`() {
        val labels = HistoryJoin.join(
            listOf(spend(0), spend(0)),
            listOf(row(1, 0)),
        )

        assertTrue(labels.merchants.isEmpty())
        assertEquals(0, labels.coverage.matched)
    }

    @Test
    fun `a merchant needs a clear majority to get a label`() {
        // Spaced three days apart so each spend has exactly one saved candidate.
        val spends = listOf(spend(0), spend(3), spend(6))
        val split = listOf(
            row(1, 0, categoryId = 7L, categoryName = "Food"),
            row(2, 3, categoryId = 8L, categoryName = "Fuel"),
            row(3, 6, categoryId = 7L, categoryName = "Food"),
        )
        // 2 of 3 is above the 0.6 share, so Food wins.
        assertEquals("Food", HistoryJoin.join(spends, split).merchants.single().categoryName)

        val tied = listOf(
            row(1, 0, categoryId = 7L, categoryName = "Food"),
            row(2, 3, categoryId = 8L, categoryName = "Fuel"),
        )
        assertTrue(HistoryJoin.join(listOf(spend(0), spend(3)), tied).merchants.isEmpty())
    }

    @Test
    fun `cash rows never teach a source account`() {
        val labels = HistoryJoin.join(
            listOf(spend(0), spend(3)),
            listOf(row(1, 0, accountType = "cash", accountId = 9L), row(2, 3, accountType = "cash", accountId = 9L)),
        )

        assertTrue(labels.sources.isEmpty())
        assertEquals(2, labels.merchants.single().count)
    }

    @Test
    fun `transfers do not teach merchant categories`() {
        val labels = HistoryJoin.join(
            listOf(spend(0, typeId = "transfer", merchant = "HDFC CARD"), spend(3, typeId = "transfer", merchant = "HDFC CARD")),
            listOf(row(1, 0, typeId = "transfer", categoryId = 7L), row(2, 3, typeId = "transfer", categoryId = 7L)),
        )

        assertTrue(labels.merchants.isEmpty())
    }

    @Test
    fun `amount must match to the paisa and date within one day`() {
        val labels = HistoryJoin.join(
            listOf(spend(0, amount = 250.0)),
            listOf(row(1, 0, amount = 250.5), row(2, 2, amount = 250.0)),
        )

        assertEquals(0, labels.coverage.matched)
        val onTheEdge = HistoryJoin.join(listOf(spend(0)), listOf(row(1, 1)))
        assertEquals(1, onTheEdge.coverage.matched)
    }

    @Test
    fun `coverage counts non-cash saved rows that had one SMS`() {
        val saved = listOf(
            row(1, 0),                                  // bank, matched
            row(2, 5),                                  // bank, not matched
            row(3, 6, accountType = "credit"),          // card, not matched
            row(4, 7, accountType = "cash"),            // cash: excluded from coverage
        )

        val labels = HistoryJoin.join(listOf(spend(0)), saved)

        assertEquals(JoinCoverage(matched = 1, total = 3), labels.coverage)
    }
}
