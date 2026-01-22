package com.palmfarm.manager.utils

import android.util.Patterns
import com.palmfarm.manager.data.database.entities.Harvest
import com.palmfarm.manager.data.database.entities.LooseNutsPicking
import com.palmfarm.manager.data.database.entities.Milling

/**
 * Utility functions for input validation
 */
object ValidationUtils {

    /**
     * Validate required field (not empty or blank)
     */
    fun isNotEmpty(value: String?): Boolean {
        return !value.isNullOrBlank()
    }

    /**
     * Validate minimum length
     */
    fun hasMinLength(value: String?, minLength: Int): Boolean {
        return value != null && value.length >= minLength
    }

    /**
     * Validate maximum length
     */
    fun hasMaxLength(value: String?, maxLength: Int): Boolean {
        return value == null || value.length <= maxLength
    }

    /**
     * Validate length range
     */
    fun hasValidLength(value: String?, minLength: Int, maxLength: Int): Boolean {
        return value != null && value.length in minLength..maxLength
    }

    /**
     * Validate email format
     */
    fun isValidEmail(email: String?): Boolean {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Validate phone number format (Cameroon: +237 XXX XXX XXX)
     * Accepts: +237XXXXXXXXX, 237XXXXXXXXX, or XXXXXXXXX (9 digits)
     */
    fun isValidCameroonPhone(phone: String?): Boolean {
        if (phone.isNullOrBlank()) return false

        val cleaned = phone.replace(Regex("[\\s-]"), "")

        return when {
            cleaned.matches(Regex("^\\+237\\d{9}$")) -> true // +237XXXXXXXXX
            cleaned.matches(Regex("^237\\d{9}$")) -> true // 237XXXXXXXXX
            cleaned.matches(Regex("^\\d{9}$")) -> true // XXXXXXXXX
            else -> false
        }
    }

    /**
     * Format Cameroon phone number to standard format
     */
    fun formatCameroonPhone(phone: String?): String? {
        if (phone.isNullOrBlank()) return null

        val cleaned = phone.replace(Regex("[\\s-]"), "")

        return when {
            cleaned.matches(Regex("^\\+237\\d{9}$")) -> {
                "+237 ${cleaned.substring(4, 7)} ${cleaned.substring(7, 10)} ${cleaned.substring(10)}"
            }
            cleaned.matches(Regex("^237\\d{9}$")) -> {
                "+237 ${cleaned.substring(3, 6)} ${cleaned.substring(6, 9)} ${cleaned.substring(9)}"
            }
            cleaned.matches(Regex("^\\d{9}$")) -> {
                "+237 ${cleaned.substring(0, 3)} ${cleaned.substring(3, 6)} ${cleaned.substring(6)}"
            }
            else -> phone
        }
    }

    /**
     * Validate positive number
     */
    fun isPositiveNumber(value: Double?): Boolean {
        return value != null && value > 0.0
    }

    /**
     * Validate non-negative number
     */
    fun isNonNegativeNumber(value: Double?): Boolean {
        return value != null && value >= 0.0
    }

    /**
     * Validate positive integer
     */
    fun isPositiveInteger(value: Int?): Boolean {
        return value != null && value > 0
    }

    /**
     * Validate non-negative integer
     */
    fun isNonNegativeInteger(value: Int?): Boolean {
        return value != null && value >= 0
    }

    /**
     * Validate password strength
     */
    fun isValidPassword(password: String?): Boolean {
        return password != null && password.length >= Constants.MIN_PASSWORD_LENGTH
    }

    /**
     * Validate passwords match
     */
    fun passwordsMatch(password: String?, confirmPassword: String?): Boolean {
        return password != null && password == confirmPassword
    }

    /**
     * Validate date is not in future
     */
    fun isNotFutureDate(timestamp: Long): Boolean {
        return timestamp <= DateUtils.getCurrentTimestamp()
    }

    /**
     * Validate date range
     */
    fun isValidDateRange(startDate: Long, endDate: Long): Boolean {
        return startDate <= endDate
    }

    /**
     * Validate percentage (0-100)
     */
    fun isValidPercentage(value: Double?): Boolean {
        return value != null && value in 0.0..100.0
    }

    /**
     * Validate advance payment amount (max 50% of gross wage)
     */
    fun isValidAdvanceAmount(advanceAmount: Double, grossWage: Double, existingAdvances: Double): Boolean {
        val totalAdvances = existingAdvances + advanceAmount
        val maxAdvance = grossWage * (Constants.MAX_ADVANCE_PERCENTAGE / 100.0)
        return totalAdvances <= maxAdvance
    }

    /**
     * Validate stock availability
     */
    fun hasSufficientStock(required: Double, available: Double): Boolean {
        return required <= available
    }

    /**
     * Validate integer input from string
     */
    fun isValidInteger(value: String?): Boolean {
        return value?.toIntOrNull() != null
    }

    /**
     * Validate double input from string
     */
    fun isValidDouble(value: String?): Boolean {
        return value?.toDoubleOrNull() != null
    }

    /**
     * Get validation error message
     */
    fun getRequiredFieldError(): String {
        return "This field is required"
    }

    fun getMinLengthError(minLength: Int): String {
        return "Minimum $minLength characters required"
    }

    fun getMaxLengthError(maxLength: Int): String {
        return "Maximum $maxLength characters allowed"
    }

    fun getInvalidEmailError(): String {
        return "Invalid email address"
    }

    fun getInvalidPhoneError(): String {
        return "Invalid phone number format"
    }

    fun getPositiveNumberError(): String {
        return "Value must be greater than 0"
    }

    fun getNonNegativeNumberError(): String {
        return "Value cannot be negative"
    }

    fun getInvalidPasswordError(): String {
        return "Password must be at least ${Constants.MIN_PASSWORD_LENGTH} characters"
    }

    fun getPasswordMismatchError(): String {
        return "Passwords do not match"
    }

    fun getFutureDateError(): String {
        return "Date cannot be in the future"
    }

    fun getInvalidDateRangeError(): String {
        return "End date must be after start date"
    }

    fun getInsufficientStockError(available: Double): String {
        return "Insufficient stock. Only ${CurrencyUtils.formatQuantity(available)} available"
    }

    fun getAdvanceExceedsLimitError(): String {
        return "Total advances cannot exceed ${Constants.MAX_ADVANCE_PERCENTAGE}% of gross wage"
    }

    /**
     * Validate harvest data.
     * @return An error message string, or null if validation passes.
     */
    fun validateHarvest(harvest: Harvest): String? {
        if (harvest.harvesterId == 0) {
            return "Worker is required"
        }
        if (harvest.numberOfBunches <= 0) {
            return "Number of bunches must be greater than 0"
        }
        if (harvest.date > System.currentTimeMillis()) {
            return "Harvest date cannot be in the future"
        }
        return null
    }

    /**
     * Validate milling data.
     * @return An error message string, or null if validation passes.
     */
    fun validateMilling(milling: Milling): String? {
        if (milling.millerId == 0) {
            return "Worker is required"
        }
        if (milling.bunchesMilled <= 0) {
            return "Bunches milled must be greater than 0"
        }
        if (milling.drumsCooked <= 0) {
            return "Drums cooked must be greater than 0"
        }
        if (milling.oilProducedGallons <= 0.0) {
            return "Oil produced must be greater than 0"
        }
        if (milling.date > System.currentTimeMillis()) {
            return "Milling date cannot be in the future"
        }
        return null
    }

    /**
     * Validate loose nuts picking data.
     * @return An error message string, or null if validation passes.
     */
    fun validateLooseNuts(looseNuts: LooseNutsPicking): String? {
        if (looseNuts.pickerId == 0) {
            return "Worker is required"
        }
        if (looseNuts.numberOfBags <= 0) {
            return "Number of bags must be greater than 0"
        }
        if (looseNuts.date > System.currentTimeMillis()) {
            return "Date cannot be in the future"
        }
        return null
    }
}


