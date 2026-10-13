package com.moneymanager.domain.capture

import com.moneymanager.data.entity.CapturedMessageEntity
import com.moneymanager.domain.ai.AccountEntry
import com.moneymanager.domain.ai.SmsRuleOutcome
import com.moneymanager.domain.ai.TransactionDraft
import com.moneymanager.domain.sms.NotATransactionException
import com.moneymanager.domain.sms.ParsedSms
import com.moneymanager.domain.sms.SmsAccountLinks
import com.moneymanager.domain.sms.SmsCategory
import com.moneymanager.domain.sms.TxnDirection
import com.moneymanager.domain.sms.TxnNature
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InboxScannerTest {

    private val now = 100 * DAY
    private val accounts = listOf(AccountEntry(1, "HDFC Bank", "bank"))

    private class FakeInbox(private val messages: List<SmsInboxMessage>) : SmsInbox {
        override val isAvailable = true
        override fun hasPermission() = true
        var lastSince: Long? = null
        var lastNewestFirst: Boolean? = null

        // Behaves like the SMS provider: date filter, sort and limit.
        override suspend fun read(sinceMillis: Long, newestFirst: Boolean, limit: Int): List<SmsInboxMessage> {
            lastSince = sinceMillis
            lastNewestFirst = newestFirst
            val sorted = messages.filter { it.date >= sinceMillis }.sortedBy { it.date }
            return (if (newestFirst) sorted.reversed() else sorted).take(limit)
        }
    }

    private class FakeStore(
        val captured: MutableSet<String> = mutableSetOf(),
        var cursor: ScanCursor = ScanCursor(null, null),
        val savedNear: MutableSet<Long> = mutableSetOf(),
        val savedCandidates: List<CaptureCandidate> = emptyList(),
        val pendingCandidates: List<CaptureCandidate> = emptyList(),
        val refMatches: Map<String, LedgerMatch> = emptyMap(),
    ) : CaptureStore {
        val rows = mutableListOf<CapturedMessageEntity>()

        override suspend fun ledgerRowWithRef(refHash: String) = refMatches[refHash]

        override suspend fun savedCandidatesNear(amount: Double, date: Long) = savedCandidates

        override suspend fun pendingCandidatesNear(amount: Double, date: Long) = pendingCandidates

        override suspend fun isCaptured(messageHash: String) = messageHash in captured

        override suspend fun insertRow(message: CapturedMessageEntity): Boolean {
            if (message.messageHash in captured) return false
            captured += message.messageHash
            rows += message
            return true
        }

        override suspend fun cursor() = cursor

        override suspend fun saveCursor(lastScanned: Long) {
            cursor = cursor.copy(lastScanned = lastScanned)
        }

        override suspend fun hasSavedTransactionNear(amount: Double, date: Long) =
            savedNear.any { kotlin.math.abs(it - date) <= DAY }

        override suspend fun activeAccounts() = listOf(AccountEntry(1, "HDFC Bank", "bank"))

        override fun observePendingCount() = kotlinx.coroutines.flow.flowOf(0)
    }

    private fun draftOutcome(amount: Double = 250.0, date: Long = 0L) = SmsRuleOutcome.Draft(
        TransactionDraft(typeId = "expense", amount = amount, date = date, merchantHint = "SWIGGY", accountLink = "hdfc|BANK_ACCOUNT|1234"),
        reference = "123456789012",
    )

    private val notATransaction = SmsRuleOutcome.NotATransaction(NotATransactionException(SmsCategory.OTP))

    private fun unmatched(amount: Double?) = SmsRuleOutcome.Unmatched(
        ParsedSms(
            category = SmsCategory.TRANSACTION, direction = TxnDirection.DEBIT, amount = amount, currency = "INR",
            balance = null, accountHint = null, counterparty = null, reference = null, dateMillis = null,
            nature = TxnNature.NORMAL, ruleId = null, matched = false,
        ),
        SmsAccountLinks.EMPTY,
    )

    private fun sms(date: Long, body: String = "body $date") = SmsInboxMessage("AD-HDFCBK-S", body, date)

    private fun scanner(
        inbox: SmsInbox,
        store: CaptureStore,
        outcome: (SmsInboxMessage) -> SmsRuleOutcome,
        calls: MutableList<SmsInboxMessage> = mutableListOf(),
    ) = InboxScanner(inbox, store) { message, accountList ->
        assertEquals(accounts, accountList)
        calls += message
        outcome(message)
    }

    @Test
    fun `verified rule match is written as a pending row with the draft`() = runTest {
        val message = sms(now - HOUR)
        val store = FakeStore()
        val report = scanner(FakeInbox(listOf(message)), store, { draftOutcome() }).scan(now)

        assertEquals(1, report.newDrafts)
        val row = store.rows.single()
        assertEquals(CaptureHash.message(message.sender, message.body, message.date), row.messageHash)
        assertEquals(CaptureHash.reference("123456789012"), row.refHash)
        assertEquals(250.0, row.amount!!, 0.001)
        assertEquals("hdfc|BANK_ACCOUNT|1234", row.sourceKey)
        assertEquals(CapturedMessageEntity.STATUS_PENDING, row.status)
        assertTrue(row.draftJson!!.contains("\"sourceType\":\"SMS\""))
        assertEquals(message.date, store.cursor.lastScanned)
    }

    @Test
    fun `message already in the ledger is not classified again`() = runTest {
        val message = sms(now - HOUR)
        val store = FakeStore(captured = mutableSetOf(CaptureHash.message(message.sender, message.body, message.date)))
        val calls = mutableListOf<SmsInboxMessage>()
        val report = scanner(FakeInbox(listOf(message)), store, { draftOutcome() }, calls).scan(now)

        assertEquals(0, report.newDrafts)
        assertEquals(1, report.alreadyCaptured)
        assertTrue(calls.isEmpty())
        assertTrue(store.rows.isEmpty())
    }

    @Test
    fun `non-transactions and unmatched messages write nothing but still move the cursor`() = runTest {
        val otp = sms(now - 3 * HOUR, "123456 is your OTP")
        val generic = sms(now - 2 * HOUR, "Rs 40 debited somewhere")
        val store = FakeStore()
        val report = scanner(FakeInbox(listOf(otp, generic)), store, {
            if (it === otp) notATransaction else unmatched(40.0)
        }).scan(now)

        assertEquals(0, report.newDrafts)
        assertEquals(1, report.notTransactions)
        assertEquals(1, report.unmatched)
        assertTrue(store.rows.isEmpty())
        assertEquals(generic.date, store.cursor.lastScanned)
    }

    @Test
    fun `window uses the cursor overlap and a late message inside it is still picked up`() = runTest {
        val cursorTime = now - 5 * HOUR
        val late = sms(cursorTime - HOUR, "delivered late")
        val inbox = FakeInbox(listOf(late))
        val store = FakeStore(cursor = ScanCursor(lastScanned = cursorTime, startDate = null))

        val report = scanner(inbox, store, { draftOutcome() }).scan(now)

        assertEquals(cursorTime - CaptureScanPlanner.OVERLAP_MS, inbox.lastSince)
        assertEquals(false, inbox.lastNewestFirst)
        assertEquals(1, report.newDrafts)
        assertEquals(cursorTime, store.cursor.lastScanned) // the cursor never moves backwards
    }

    @Test
    fun `capped cursor window stops at the last message read so the next scan continues from there`() = runTest {
        val messages = (1..CaptureScanPlanner.MAX_MESSAGES + 20).map { sms(now - (it * 60_000L)) }
        val store = FakeStore(cursor = ScanCursor(lastScanned = now - DAY, startDate = null))
        val report = scanner(FakeInbox(messages), store, { draftOutcome() }).scan(now)

        assertTrue(report.capped)
        assertEquals(CaptureScanPlanner.MAX_MESSAGES, report.newDrafts)
        // Oldest first: the 500 read are the oldest in the window, so the cursor is the newest of those.
        val processed = messages.sortedBy { it.date }.take(CaptureScanPlanner.MAX_MESSAGES)
        assertEquals(processed.maxOf { it.date }, store.cursor.lastScanned)
    }

    @Test
    fun `anchor scan without a cursor walks back and stops after three known spends`() = runTest {
        val newest = (0 until 6).map { sms(now - HOUR * (it + 1)) }      // newest first in the list below
        val known = newest.take(3)                                         // the three newest are already in the ledger
        val store = FakeStore(captured = known.map { CaptureHash.message(it.sender, it.body, it.date) }.toMutableSet())
        val calls = mutableListOf<SmsInboxMessage>()
        val inbox = FakeInbox(newest)

        val report = scanner(inbox, store, { draftOutcome() }, calls).scan(now)

        assertEquals(true, inbox.lastNewestFirst)
        assertEquals(now - CaptureScanPlanner.MAX_WINDOW_MS, inbox.lastSince)
        assertEquals(3, report.alreadyCaptured)
        assertTrue(calls.isEmpty())
        assertEquals(0, report.newDrafts)
        assertEquals(known.first().date, store.cursor.lastScanned)
    }

    @Test
    fun `anchor scan counts a saved transaction entered by hand as known`() = runTest {
        val newest = (0 until 5).map { sms(now - HOUR * (it + 1)) }
        val store = FakeStore(savedNear = newest.take(3).map { it.date }.toMutableSet())
        val calls = mutableListOf<SmsInboxMessage>()

        val report = scanner(FakeInbox(newest), store, { draftOutcome() }, calls).scan(now)

        // Three drafts are written (their spends are not yet in the ledger) but each is already saved by hand.
        assertEquals(3, calls.size)
        assertEquals(3, report.newDrafts)
        assertEquals(newest.first().date, store.cursor.lastScanned)
    }

    @Test
    fun `planner clamps the floor to the 31-day cap and honours a later start date`() {
        val cap = CaptureScanPlanner.plan(lastScanned = null, startDate = null, now = now)
        assertTrue(cap.anchorMode)
        assertEquals(now - CaptureScanPlanner.MAX_WINDOW_MS, cap.lowerBound)

        val later = CaptureScanPlanner.plan(lastScanned = null, startDate = now - DAY, now = now)
        assertEquals(now - DAY, later.lowerBound)

        val earlier = CaptureScanPlanner.plan(lastScanned = null, startDate = now - 60 * DAY, now = now)
        assertEquals(now - CaptureScanPlanner.MAX_WINDOW_MS, earlier.lowerBound)

        val cursor = CaptureScanPlanner.plan(lastScanned = now - 2 * DAY, startDate = null, now = now)
        assertFalse(cursor.anchorMode)
        assertEquals(now - 2 * DAY - CaptureScanPlanner.OVERLAP_MS, cursor.lowerBound)
    }

    @Test
    fun `hashes are stable and never equal for different messages`() {
        assertEquals(CaptureHash.message("A", "x", 1L), CaptureHash.message("A", "x", 1L))
        assertFalse(CaptureHash.message("A", "x", 1L) == CaptureHash.message("A", "x", 2L))
        assertFalse(CaptureHash.message(null, "x", 1L) == CaptureHash.message("A", "x", 1L))
        assertEquals(CaptureHash.reference(" abc12 "), CaptureHash.reference("ABC12"))
    }

    @Test
    fun `same spend as a saved transaction is stored as saved and not shown as a draft`() = runTest {
        val message = sms(now - HOUR)
        val store = FakeStore(savedCandidates = listOf(CaptureCandidate(77, "expense", 250.0, now - HOUR, 1, "Swiggy order")))
        val report = scanner(FakeInbox(listOf(message)), store, { draftOutcome(date = now - HOUR) }).scan(now)

        assertEquals(0, report.newDrafts)
        assertEquals(1, report.duplicates)
        val row = store.rows.single()
        assertEquals(CapturedMessageEntity.STATUS_SAVED, row.status)
        assertEquals(77L, row.transactionId)
        assertEquals(null, row.draftJson)
    }

    @Test
    fun `same spend as a pending draft is stored as merged into it`() = runTest {
        val message = sms(now - HOUR)
        val store = FakeStore(pendingCandidates = listOf(CaptureCandidate(12, "expense", 250.0, now - HOUR, 1, "SWIGGY")))
        val report = scanner(FakeInbox(listOf(message)), store, { draftOutcome(date = now - HOUR) }).scan(now)

        assertEquals(0, report.newDrafts)
        assertEquals(1, report.duplicates)
        val row = store.rows.single()
        assertEquals(CapturedMessageEntity.STATUS_MERGED, row.status)
        assertEquals(12L, row.mergedIntoId)
    }

    @Test
    fun `uncertain match stays in the inbox with a maybe-duplicate flag`() = runTest {
        val message = sms(now - HOUR)
        // Same amount and window, but no merchant match: shown, never hidden.
        val store = FakeStore(savedCandidates = listOf(CaptureCandidate(77, "expense", 250.0, now - HOUR, 1, "")))
        val report = scanner(FakeInbox(listOf(message)), store, { draftOutcome(date = now - HOUR) }).scan(now)

        assertEquals(1, report.newDrafts)
        assertEquals(0, report.duplicates)
        val row = store.rows.single()
        assertEquals(CapturedMessageEntity.STATUS_PENDING, row.status)
        assertTrue(row.draftJson!!.contains("maybe_dup:77"))
    }

    @Test
    fun `reference number already in the ledger wins over everything else`() = runTest {
        val message = sms(now - HOUR)
        val refHash = CaptureHash.reference("123456789012")
        val store = FakeStore(refMatches = mapOf(refHash to LedgerMatch(ledgerId = 5, transactionId = 99)))
        val report = scanner(FakeInbox(listOf(message)), store, { draftOutcome(date = now - HOUR) }).scan(now)

        assertEquals(1, report.duplicates)
        assertEquals(99L, store.rows.single().transactionId)
    }

    private companion object {
        const val HOUR = 60 * 60 * 1000L
        const val DAY = 24 * HOUR
    }
}
