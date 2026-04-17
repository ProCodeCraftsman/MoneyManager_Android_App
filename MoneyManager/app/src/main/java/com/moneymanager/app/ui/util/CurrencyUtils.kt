package com.moneymanager.app.ui.util

import java.text.NumberFormat
import java.util.*

object CurrencyUtils {
    fun getCurrencyFormat(currencyCode: String): NumberFormat {
        val locale = when (currencyCode) {
            "INR" -> Locale("en", "IN")
            "GBP" -> Locale.UK
            "EUR" -> Locale.GERMANY
            "JPY" -> Locale.JAPAN
            "CAD" -> Locale.CANADA
            "AUD" -> Locale("en", "AU")
            else -> Locale.US
        }
        return NumberFormat.getCurrencyInstance(locale)
    }
}
