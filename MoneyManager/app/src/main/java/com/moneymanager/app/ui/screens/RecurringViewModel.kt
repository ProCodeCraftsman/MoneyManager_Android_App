package com.moneymanager.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.RecurringEntity
import com.moneymanager.domain.repository.AccountRepository
import com.moneymanager.domain.repository.CategoryRepository
import com.moneymanager.domain.repository.RecurringRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class RecurringUiState(
    val recurringList: List<RecurringEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val currentDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
)

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val recurringRepository: RecurringRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    val uiState: StateFlow<RecurringUiState> = combine(
        recurringRepository.getAllRecurring(),
        accountRepository.getAllAccounts(),
        categoryRepository.getAllCategories(),
    ) { recurringList, accounts, categories ->
        RecurringUiState(
            recurringList = recurringList,
            accounts = accounts,
            categories = categories,
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