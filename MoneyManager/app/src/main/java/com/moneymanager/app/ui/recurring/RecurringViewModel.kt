package com.moneymanager.app.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.RecurringEntity
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.repository.AccountRepository
import com.moneymanager.domain.repository.CategoryRepository
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
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    val uiState: StateFlow<RecurringUiState> = combine(
        recurringRepository.getAllRecurring(),
        accountRepository.getAllAccounts(),
        categoryRepository.getAllCategories(),
        preferencesManager.currency,
    ) { recurringList, accounts, categories, currencyCode ->
        RecurringUiState(
            recurringList = recurringList,
            accounts = accounts,
            categories = categories,
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
