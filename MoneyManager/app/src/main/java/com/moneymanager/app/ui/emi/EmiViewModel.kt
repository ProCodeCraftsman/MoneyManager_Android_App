package com.moneymanager.app.ui.emi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.moneymanager.data.MoneyManagerDatabase
import com.moneymanager.data.dao.EmiDao
import com.moneymanager.data.dao.TransactionDao
import com.moneymanager.data.entity.EmiEntity
import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.repository.AccountRepository
import com.moneymanager.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class EmiViewModel @Inject constructor(
    private val database: MoneyManagerDatabase,
    private val emiDao: EmiDao,
    private val transactionDao: TransactionDao,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    preferencesManager: PreferencesManager,
) : ViewModel() {

    val uiState: StateFlow<EmiUiState> = combine(
        emiDao.getAllEmis(),
        accountRepository.getAllAccounts(),
        categoryRepository.getAllCategories(),
        preferencesManager.currency,
    ) { emis, accounts, categories, currencyCode ->
        EmiUiState(
            emis = emis,
            accounts = accounts,
            categories = categories,
            currencyCode = currencyCode,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EmiUiState()
    )

    fun getInstallments(emiId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByEmi(emiId)

    /**
     * Cancels every installment that hasn't come due yet (postedToBalance == false, so it
     * never touched the account balance) and marks the EMI FORECLOSED. Posted installments
     * are left in transaction history untouched. Wrapped in a DB transaction so the delete
     * and the status flip can't be observed half-done.
     */
    suspend fun forecloseEmi(emi: EmiEntity) {
        database.withTransaction {
            transactionDao.deleteUnpostedInstallmentsByEmi(emi.id)
            emiDao.updateEmi(emi.copy(status = "FORECLOSED"))
        }
    }
}
