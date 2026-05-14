package com.moneymanager.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.entity.TemplateEntity
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.repository.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TemplatesUiState(
    val templates: List<TemplateEntity> = emptyList(),
    val currencyCode: String = "INR",
    val isLoading: Boolean = true
)

@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val templateRepository: TemplateRepository,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    val uiState: StateFlow<TemplatesUiState> = combine(
        templateRepository.getAllTemplates(),
        preferencesManager.currency
    ) { templates, currencyCode ->
        TemplatesUiState(
            templates = templates,
            currencyCode = currencyCode,
            isLoading = false
        )
    }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TemplatesUiState()
        )

    fun addTemplate(name: String, type: String, amount: Double, categoryId: Long?, note: String) {
        viewModelScope.launch {
            templateRepository.insertTemplate(
                TemplateEntity(
                    name = name,
                    type = type,
                    amount = amount,
                    categoryId = categoryId,
                    note = note
                )
            )
        }
    }

    fun deleteTemplate(template: TemplateEntity) {
        viewModelScope.launch {
            templateRepository.deleteTemplate(template)
        }
    }
}