package com.palmfarm.manager.ui.dashboard

/**
 * Data models for Dashboard screen
 */

/**
 * Key metrics for dashboard display
 */
data class KeyMetrics(
    val bunchesPerTree: Double = 0.0,
    val oilPerBunch: Double = 0.0,
    val cycleExpense: Double = 0.0,
    val cycleIncome: Double = 0.0
)

/**
 * Recent activity item for dashboard
 */
data class ActivityItem(
    val type: ActivityType,
    val title: String,
    val description: String,
    val date: Long,
    val icon: Int = 0
)

/**
 * Activity types
 */
enum class ActivityType {
    HARVEST,
    MILLING,
    TASK,
    EXPENSE,
    SALE,
    LOAN_PAYMENT,
    WAGE_PAYMENT,
    OTHER
}

/**
 * Cycle history summary for dashboard
 */
data class CycleHistorySummary(
    val cycleId: Int,
    val cycleName: String,
    val realizedBunches: Int,
    val expectedBunches: Int,
    val totalExpenses: Double,
    val totalIncome: Double,
    val profit: Double
)
