package com.moneymanager.app.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.data.repository.DraftEditStatsRepository
import com.moneymanager.domain.capture.ApprovedCapture
import com.moneymanager.domain.capture.CaptureApproval
import com.moneymanager.domain.capture.CaptureFlags
import com.moneymanager.domain.capture.CaptureReview
import com.moneymanager.domain.capture.DraftEdits
import com.moneymanager.domain.capture.InboxRules
import com.moneymanager.domain.capture.PendingCapture
import com.moneymanager.domain.capture.ScanInboxUseCase
import com.moneymanager.domain.repository.CategoryRepository
import com.moneymanager.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CaptureInboxUiState(
    val rows: List<PendingCapture> = emptyList(),
    val scanning: Boolean = false,
    val permissionDenied: Boolean = false,
    val message: String? = null,
    /** Bumps on every new message so the same text can be shown twice. */
    val messageId: Int = 0,
    val canUndo: Boolean = false,
    val currency: String = "INR",
) {
    val readyCount: Int get() = rows.count { InboxRules.isReadyToApprove(it.draft) }
}

/**
 * Drives the SMS draft inbox (AI-056). Nothing here touches a balance except an explicit approve, and an approve
 * can be undone until the next action. Only fully deterministic rows are approved in bulk; everything else is the
 * user's call, row by row.
 */
@HiltViewModel
class CaptureInboxViewModel @Inject constructor(
    private val review: CaptureReview,
    private val approval: CaptureApproval,
    private val scanInbox: ScanInboxUseCase,
    private val categories: CategoryRepository,
    private val transactions: TransactionRepository,
    private val draftEditStats: DraftEditStatsRepository,
    private val preferences: PreferencesManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureInboxUiState())
    val uiState: StateFlow<CaptureInboxUiState> = _uiState.asStateFlow()

    /** The last approve action, kept so undo can take it back. */
    private var lastApproved: List<ApprovedCapture> = emptyList()

    /** Fields the user changed inline before approving, by ledger id. Names only, for edit tracking (AI-014). */
    private val inlineEdits = mutableMapOf<Long, MutableSet<String>>()

    init {
        viewModelScope.launch {
            review.observePending().collect { rows -> _uiState.update { it.copy(rows = rows) } }
        }
        viewModelScope.launch {
            preferences.currency.collect { code -> _uiState.update { it.copy(currency = code) } }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) scan() else _uiState.update { it.copy(permissionDenied = true) }
    }

    /** Reads the inbox and writes new drafts. Runs the same rule-only scan as the app-open trigger. */
    fun scan() {
        if (_uiState.value.scanning) return
        _uiState.update { it.copy(scanning = true, permissionDenied = false) }
        viewModelScope.launch {
            try {
                val report = scanInbox()
                _uiState.update { it.copy(scanning = false) }
                announce("${report.newDrafts} new, ${report.duplicates} already recorded")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(scanning = false) }
                announce("Scan failed. Try again.")
            }
        }
    }

    fun approveReady() {
        val ready = _uiState.value.rows.filter { InboxRules.isReadyToApprove(it.draft) }
        if (ready.isEmpty()) return
        viewModelScope.launch { approveAll(ready, "Saved ${ready.size}") }
    }

    fun approve(row: PendingCapture) {
        viewModelScope.launch { approveAll(listOf(row), "Saved") }
    }

    fun undo() {
        val undoing = lastApproved
        if (undoing.isEmpty()) return
        viewModelScope.launch {
            undoing.forEach { approval.undo(it) }
            lastApproved = emptyList()
            _uiState.update { it.copy(canUndo = false) }
            announce("Undone")
        }
    }

    fun reject(row: PendingCapture) {
        viewModelScope.launch {
            review.markRejected(row.ledgerId)
            inlineEdits.remove(row.ledgerId)
            lastApproved = emptyList()
            _uiState.update { it.copy(canUndo = false) }
            announce("Rejected")
        }
    }

    /** "Merge" on a suspected duplicate: the SMS is the same spend as the saved transaction, so it closes without a new one. */
    fun merge(row: PendingCapture) {
        val existing = row.maybeDuplicateOf ?: return
        viewModelScope.launch {
            review.markSaved(row.ledgerId, existing)
            announce("Merged")
        }
    }

    /** "Keep both": the saved transaction and this SMS are different spends, so the flag is dropped. */
    fun keepBoth(row: PendingCapture) {
        viewModelScope.launch {
            review.updateDraft(row.ledgerId, row.draft.copy(flags = CaptureFlags.withoutMaybeDuplicate(row.draft.flags)))
        }
    }

    fun setCategory(row: PendingCapture, category: CategoryEntity) {
        viewModelScope.launch {
            review.updateDraft(row.ledgerId, row.draft.copy(categoryId = category.id, categoryName = category.name))
            inlineEdits.getOrPut(row.ledgerId) { mutableSetOf() }.add(DraftEdits.CATEGORY)
        }
    }

    /** Five categories of this type, most used first: the inline fix shows these, not the full list. */
    suspend fun categoryChoices(typeId: String?): List<CategoryEntity> {
        val usage = transactions.getAllTransactions().first()
            .mapNotNull { it.categoryId }
            .groupingBy { it }
            .eachCount()
        return categories.getAllCategories().first()
            .filter { it.type == typeId }
            .sortedByDescending { usage[it.id] ?: 0 }
            .take(CHOICE_COUNT)
    }

    private suspend fun approveAll(rows: List<PendingCapture>, message: String) {
        val approved = rows.map { row ->
            val changed = inlineEdits.remove(row.ledgerId).orEmpty().toList()
            val result = approval.approve(row)
            draftEditStats.record(row.draft.sourceType ?: DEFAULT_SOURCE, changed)
            result
        }
        lastApproved = approved
        _uiState.update { it.copy(canUndo = true) }
        announce(message)
    }

    private fun announce(text: String) {
        _uiState.update { it.copy(message = text, messageId = it.messageId + 1) }
    }

    private companion object {
        const val CHOICE_COUNT = 5
        const val DEFAULT_SOURCE = "SMS"
    }
}
