package com.palmfarm.manager.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility functions for date formatting and calculations
 */
object DateUtils {

    private val displayFormat = SimpleDateFormat(Constants.DATE_FORMAT_DISPLAY, Locale.ENGLISH)
    private val fileFormat = SimpleDateFormat(Constants.DATE_FORMAT_FILE, Locale.ENGLISH)
    private val shortFormat = SimpleDateFormat(Constants.DATE_FORMAT_SHORT, Locale.ENGLISH)

    /**
     * Format timestamp to display format: "15 March 2024"
     */
    fun formatToDisplay(timestamp: Long): String {
        return displayFormat.format(Date(timestamp))
    }

    /**
     * Format timestamp for file names: "2024-03-15_143022"
     */
    fun formatToFileName(timestamp: Long): String {
        return fileFormat.format(Date(timestamp))
    }

    /**
     * Format timestamp to short format: "15/03/2024"
     */
    fun formatToShort(timestamp: Long): String {
        return shortFormat.format(Date(timestamp))
    }

    /**
     * Get current timestamp in milliseconds
     */
    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }

    /**
     * Get timestamp for a specific date
     */
    fun getTimestamp(year: Int, month: Int, dayOfMonth: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, dayOfMonth, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Get year from timestamp
     */
    fun getYear(timestamp: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.YEAR)
    }

    /**
     * Get month from timestamp (1-12)
     */
    fun getMonth(timestamp: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.MONTH) + 1
    }

    /**
     * Get day of month from timestamp
     */
    fun getDayOfMonth(timestamp: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.DAY_OF_MONTH)
    }

    /**
     * Calculate cycle name based on start date and season start month
     * Example: If season starts in March, cycle "2024-2025" runs March 2024 - February 2025
     */
    fun calculateCycleName(startDate: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate
        val startYear = calendar.get(Calendar.YEAR)
        val endYear = startYear + 1
        return "$startYear-$endYear"
    }

    /**
     * Calculate cycle end date (one year minus one day from start date)
     */
    fun calculateCycleEndDate(startDate: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate
        calendar.add(Calendar.YEAR, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        return calendar.timeInMillis
    }

    /**
     * Get cycle start date for given year and season start month
     */
    fun getCycleStartDate(year: Int, seasonStartMonth: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, seasonStartMonth - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Check if current date is past the cycle end date
     */
    fun isCycleEnded(cycleEndDate: Long): Boolean {
        return getCurrentTimestamp() > cycleEndDate
    }

    /**
     * Get the current cycle start date based on season start month
     */
    fun getCurrentCycleStartDate(seasonStartMonth: Int): Long {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1

        // If current month is before season start, use last year
        val cycleYear = if (currentMonth < seasonStartMonth) {
            currentYear - 1
        } else {
            currentYear
        }

        return getCycleStartDate(cycleYear, seasonStartMonth)
    }

    /**
     * Format time ago (e.g., "2 hours ago", "3 days ago")
     */
    fun formatTimeAgo(timestamp: Long): String {
        val now = getCurrentTimestamp()
        val diff = now - timestamp

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = days / 30

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
            hours < 24 -> "$hours ${if (hours == 1L) "hour" else "hours"} ago"
            days < 7 -> "$days ${if (days == 1L) "day" else "days"} ago"
            weeks < 4 -> "$weeks ${if (weeks == 1L) "week" else "weeks"} ago"
            months < 12 -> "$months ${if (months == 1L) "month" else "months"} ago"
            else -> {
                val years = months / 12
                "$years ${if (years == 1L) "year" else "years"} ago"
            }
        }
    }

    /**
     * Get start of day timestamp
     */
    fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Get end of day timestamp
     */
    fun getEndOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    /**
     * Get month name from month number (1-12)
     */
    fun getMonthName(month: Int): String {
        return if (month in 1..12) {
            Constants.MONTHS[month - 1]
        } else {
            "Invalid Month"
        }
    }

    /**
     * Add months to a timestamp
     */
    fun addMonths(timestamp: Long, months: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.add(Calendar.MONTH, months)
        return calendar.timeInMillis
    }

    /**
     * Calculate months between two timestamps
     */
    fun monthsBetween(startTimestamp: Long, endTimestamp: Long): Int {
        val startCal = Calendar.getInstance()
        startCal.timeInMillis = startTimestamp

        val endCal = Calendar.getInstance()
        endCal.timeInMillis = endTimestamp

        val yearDiff = endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)
        val monthDiff = endCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH)

        return yearDiff * 12 + monthDiff
    }
}
