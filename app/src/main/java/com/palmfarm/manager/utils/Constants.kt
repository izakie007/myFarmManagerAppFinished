package com.palmfarm.manager.utils

/**
 * Application-wide constants
 */
object Constants {

    // Date Formats
    const val DATE_FORMAT_DISPLAY = "dd MMMM yyyy" // 15 March 2024
    const val DATE_FORMAT_FILE = "yyyy-MM-dd_HHmmss" // 2024-03-15_143022
    const val DATE_FORMAT_SHORT = "dd/MM/yyyy" // 15/03/2024

    // Currency
    const val CURRENCY_CODE = "XAF"
    const val CURRENCY_SYMBOL = "XAF"

    // Authentication
    const val AUTH_METHOD_NONE = "NONE"
    const val AUTH_METHOD_BIOMETRIC = "BIOMETRIC"
    const val AUTH_METHOD_PASSWORD = "PASSWORD"
    const val MAX_PASSWORD_ATTEMPTS = 5
    const val LOCKOUT_DURATION_MS = 30000L // 30 seconds
    const val MIN_PASSWORD_LENGTH = 6

    // Task Status
    const val TASK_STATUS_PENDING = "PENDING"
    const val TASK_STATUS_IN_PROGRESS = "IN_PROGRESS"
    const val TASK_STATUS_COMPLETED = "COMPLETED"
    const val TASK_STATUS_CANCELLED = "CANCELLED"

    // Task Categories
    const val TASK_CATEGORY_HARVESTING = "Harvesting"
    const val TASK_CATEGORY_MAINTENANCE = "Maintenance"
    const val TASK_CATEGORY_MILLING = "Milling"
    const val TASK_CATEGORY_OTHER = "Other"

    // Expense Categories
    const val EXPENSE_CATEGORY_SUPPLIES = "SUPPLIES"
    const val EXPENSE_CATEGORY_MAINTENANCE = "MAINTENANCE"
    const val EXPENSE_CATEGORY_FUEL = "FUEL"
    const val EXPENSE_CATEGORY_TRANSPORT = "TRANSPORT"

    // Sale Units
    const val SALE_UNIT_GALLON = "GALLON"
    const val SALE_UNIT_TONNE = "TONNE"
    const val SALE_UNIT_BUNCH = "BUNCH"

    // Fixed Cost Categories
    const val FIXED_COST_CATEGORY_LAND = "LAND"
    const val FIXED_COST_CATEGORY_EQUIPMENT = "EQUIPMENT"

    // Fixed Cost Payment Types
    const val FIXED_COST_PAYMENT_RENT = "RENT"
    const val FIXED_COST_PAYMENT_PURCHASE = "PURCHASE"

    // Wage Payment Methods
    const val PAYMENT_METHOD_CASH = "CASH"
    const val PAYMENT_METHOD_TRANSFER = "TRANSFER"
    const val PAYMENT_METHOD_OTHER = "OTHER"

    // Worker Specialties
    const val SPECIALTY_RABATTAGE = "Rabattage"
    const val SPECIALTY_CUTTING = "Cutting"
    const val SPECIALTY_MILLING = "Milling"

    // Conversion Factors
    const val LITRES_PER_GALLON = 20.0

    // Advance Payment Limits
    const val MAX_ADVANCE_PERCENTAGE = 50.0

    // File Paths
    const val BACKUP_FOLDER = "PalmFarm/Backups"
    const val PAYSLIP_FOLDER = "PalmFarm/Payslips"
    const val RECEIPT_FOLDER = "receipts"
    const val REPORT_FOLDER = "PalmFarm/Reports"

    // File Prefixes
    const val BACKUP_FILE_PREFIX = "PalmFarmBackup_"
    const val PAYSLIP_FILE_PREFIX = "Payslip_"
    const val REPORT_FILE_PREFIX = "FinancialReport_"
    const val ANALYTICS_REPORT_PREFIX = "AnalyticsReport_"

    // SharedPreferences Keys
    const val PREFS_NAME = "palm_farm_prefs"
    const val PREF_LAST_BACKUP_TIME = "last_backup_time"
    const val PREF_FAILED_AUTH_ATTEMPTS = "failed_auth_attempts"
    const val PREF_LOCKOUT_TIME = "lockout_time"

    // UI Limits
    const val RECENT_ACTIVITY_LIMIT = 5
    const val CYCLE_HISTORY_LIMIT = 3
    const val MAX_INPUT_LENGTH_SHORT = 50
    const val MAX_INPUT_LENGTH_MEDIUM = 100
    const val MAX_INPUT_LENGTH_LONG = 200
    const val MAX_INPUT_LENGTH_DESCRIPTION = 500

    // Months
    val MONTHS = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
}
