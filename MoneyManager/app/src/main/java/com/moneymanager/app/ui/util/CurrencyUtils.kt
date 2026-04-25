package com.moneymanager.app.ui.util

import java.text.NumberFormat
import java.util.*

object CurrencyUtils {
    fun getCurrencyFormat(currencyCode: String): NumberFormat {
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
        try {
            format.currency = Currency.getInstance(currencyCode)
        } catch (e: Exception) {
            // Fallback to default if currency code is invalid
        }
        return format
    }

    fun getCurrencySymbol(currencyCode: String): String {
        return try {
            Currency.getInstance(currencyCode).getSymbol(Locale.getDefault())
        } catch (e: Exception) {
            currencyCode
        }
    }
}
