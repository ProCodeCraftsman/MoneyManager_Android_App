package com.moneymanager.app.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.GoalEntity
import com.moneymanager.data.entity.PeerContact
import com.moneymanager.data.entity.RecurringEntity
import com.moneymanager.data.entity.TagEntity
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.repository.AccountRepository
import com.moneymanager.domain.repository.CategoryRepository
import com.moneymanager.domain.repository.GoalRepository
import com.moneymanager.domain.repository.PeerContactRepository
import com.moneymanager.domain.repository.RecurringRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val recurringRepository: RecurringRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val goalRepository: GoalRepository,
    private val peerContactRepository: PeerContactRepository,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    val uiState: StateFlow<RecurringUiState> = combine(
        recurringRepository.getAllRecurring(),
        accountRepository.getActiveAccounts(),
        categoryRepository.getAllCategories(),
        categoryRepository.getAllTags(),
        peerContactRepository.getAllPeers(),
        goalRepository.getAllGoals(),
        preferencesManager.currency,
    ) { flows ->
        val recurringList = flows[0] as List<RecurringEntity>
        val accounts = flows[1] as List<AccountEntity>
        val categories = flows[2] as List<CategoryEntity>
        val tags = flows[3] as List<TagEntity>
        val peers = flows[4] as List<PeerContact>
        val goals = flows[5] as List<GoalEntity>
        val currencyCode = flows[6] as String
        
        RecurringUiState(
            recurringList = recurringList,
            accounts = accounts,
            categories = categories,
            tags = tags,
            peers = peers,
            goals = goals,
            currencyCode = currencyCode,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecurringUiState()
    )

    suspend fun saveRecurring(recurring: RecurringEntity) {
        if (recurring.id == 0L) {
            recurringRepository.insertRecurring(recurring)
        } else {
            recurringRepository.updateRecurring(recurring)
        }
    }

    suspend fun deleteRecurring(recurring: RecurringEntity) {
        recurringRepository.deleteRecurring(recurring)
    }

    suspend fun toggleActive(recurring: RecurringEntity) {
        val updated = recurring.copy(isActive = !recurring.isActive)
        recurringRepository.updateRecurring(updated)
    }

    suspend fun getRecurringById(id: Long): RecurringEntity? {
        return recurringRepository.getRecurringById(id)
    }
}
