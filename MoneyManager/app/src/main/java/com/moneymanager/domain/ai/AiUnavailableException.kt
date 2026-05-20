package com.moneymanager.domain.ai

sealed class AiError {
    data object LocalModelNotDownloaded : AiError()
    data object NoBackendAvailable : AiError()
    data object AiCallTimedOut : AiError()
    data object DraftParseFailed : AiError()
    data object InsufficientInformation : AiError()
    data class Generic(val message: String) : AiError()

    fun userMessage(): String = when (this) {
        LocalModelNotDownloaded -> "Local AI model not downloaded yet. Go to Settings to download."
        NoBackendAvailable -> "AI is not available on this device."
        AiCallTimedOut -> "AI took too long to respond. Please try again."
        DraftParseFailed -> "AI generated an unrecognizable response. Please enter details manually."
        InsufficientInformation -> "Not enough information to create a draft — try adding the amount, type (expense/income), or merchant name."
        is Generic -> message
    }
}

class AiUnavailableException(message: String = "AI client is not available") : Exception(message)
