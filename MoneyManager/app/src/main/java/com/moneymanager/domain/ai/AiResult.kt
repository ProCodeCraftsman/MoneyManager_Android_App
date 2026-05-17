package com.moneymanager.domain.ai

sealed interface AiResult<out T> {
    data object Loading : AiResult<Nothing>
    data class Success<T>(val data: T) : AiResult<T>
    data class Error(val exception: Throwable) : AiResult<Nothing>
}
