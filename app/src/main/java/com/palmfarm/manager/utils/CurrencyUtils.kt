package com.palmfarm.manager.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

/**
 * Utility functions for currency formatting (XAF)
 */
object CurrencyUtils {

    private val decimalFormat: DecimalFormat
    private val decimalFormatWithDecimals: DecimalFormat

    init {
        val symbols = DecimalFormatSymbols(Locale.ENGLISH).apply {
            groupingSeparator = ' '
            decimalSeparator = '.'
        }

        // Format without decimals for whole numbers
        decimalFormat = DecimalFormat("#,##0", symbols)

        // Format with 2 decimal places
        decimalFormatWithDecimals = DecimalFormat("#,##0.00", symbols)
    }

    /**
     * Format amount as XAF currency
     * Example: 1500000.0 -> "1 500 000 XAF"
     */
    fun formatAmount(amount: Double): String {
        val formatted = if (amount % 1.0 == 0.0) {
            // Whole number - no decimals
            decimalFormat.format(amount)
        } else {
            // Has decimals
            decimalFormatWithDecimals.format(amount)
        }
        return "$formatted ${Constants.CURRENCY_CODE}"
    }

    /**
     * Format amount without currency symbol
     * Example: 1500000.0 -> "1 500 000"
     */
    fun formatAmountWithoutSymbol(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            decimalFormat.format(amount)
        } else {
            decimalFormatWithDecimals.format(amount)
        }
    }

    /**
     * Format amount with custom decimal places
     */
    fun formatAmount(amount: Double, decimalPlaces: Int): String {
        val pattern = if (decimalPlaces == 0) {
            "#,##0"
        } else {
            "#,##0." + "0".repeat(decimalPlaces)
        }

        val symbols = DecimalFormatSymbols(Locale.ENGLISH).apply {
            groupingSeparator = ' '
            decimalSeparator = '.'
        }

        val format = DecimalFormat(pattern, symbols)
        return "${format.format(amount)} ${Constants.CURRENCY_CODE}"
    }

    /**
     * Parse formatted amount string back to double
     * Example: "1 500 000 XAF" -> 1500000.0
     */
    fun parseAmount(formattedAmount: String): Double? {
        return try {
            val cleaned = formattedAmount
                .replace(Constants.CURRENCY_CODE, "")
                .replace(" ", "")
                .trim()
            cleaned.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Format percentage
     * Example: 75.5 -> "75.5%"
     */
    fun formatPercentage(value: Double): String {
        return if (value % 1.0 == 0.0) {
            "${value.toInt()}%"
        } else {
            String.format(Locale.ENGLISH, "%.1f%%", value)
        }
    }

    /**
     * Format quantity (non-currency numbers)
     * Example: 1500.0 -> "1 500"
     */
    fun formatQuantity(quantity: Double): String {
        return if (quantity % 1.0 == 0.0) {
            decimalFormat.format(quantity)
        } else {
            decimalFormatWithDecimals.format(quantity)
        }
    }

    /**
     * Format quantity with unit
     * Example: (150.0, "bunches") -> "150 bunches"
     */
    fun formatQuantityWithUnit(quantity: Double, unit: String): String {
        return "${formatQuantity(quantity)} $unit"
    }

    /**
     * Calculate percentage
     * Example: (450, 600) -> 75.0
     */
    fun calculatePercentage(part: Double, total: Double): Double {
        return if (total == 0.0) {
            0.0
        } else {
            (part / total) * 100.0
        }
    }

    /**
     * Calculate percentage change
     * Example: (current=500, previous=400) -> 25.0 (25% increase)
     */
    fun calculatePercentageChange(current: Double, previous: Double): Double {
        return if (previous == 0.0) {
            if (current > 0.0) 100.0 else 0.0
        } else {
            ((current - previous) / previous) * 100.0
        }
    }

    /**
     * Format percentage change with + or - sign
     * Example: 25.5 -> "+25.5%", -10.2 -> "-10.2%"
     */
    fun formatPercentageChange(change: Double): String {
        val sign = if (change >= 0) "+" else ""
        return "$sign${formatPercentage(change)}"
    }

    /**
     * Round to 2 decimal places
     */
    fun roundToTwoDecimals(value: Double): Double {
        return String.format(Locale.ENGLISH, "%.2f", value).toDouble()
    }

    /**
     * Format amount in abbreviated form for display
     * Example: 1500000 -> "1.5M", 15000 -> "15K", 500 -> "500"
     */
    fun formatAmountAbbreviated(amount: Double): String {
        val absoluteAmount = kotlin.math.abs(amount)
        val sign = if (amount < 0) "-" else ""
        
        return when {
            absoluteAmount >= 1_000_000_000 -> {
                val value = absoluteAmount / 1_000_000_000
                String.format(Locale.ENGLISH, "%s%.1fB", sign, value)
            }
            absoluteAmount >= 1_000_000 -> {
                val value = absoluteAmount / 1_000_000
                String.format(Locale.ENGLISH, "%s%.1fM", sign, value)
            }
            absoluteAmount >= 1_000 -> {
                val value = absoluteAmount / 1_000
                if (value % 1.0 == 0.0) {
                    String.format(Locale.ENGLISH, "%s%dK", sign, value.toInt())
                } else {
                    String.format(Locale.ENGLISH, "%s%.1fK", sign, value)
                }
            }
            else -> {
                if (absoluteAmount % 1.0 == 0.0) {
                    String.format(Locale.ENGLISH, "%s%d", sign, absoluteAmount.toInt())
                } else {
                    String.format(Locale.ENGLISH, "%s%.1f", sign, absoluteAmount)
                }
            }
        }
    }
}
