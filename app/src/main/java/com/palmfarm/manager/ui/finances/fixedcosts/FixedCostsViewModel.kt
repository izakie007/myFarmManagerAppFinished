package com.palmfarm.manager.ui.finances.fixedcosts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palmfarm.manager.data.database.dao.FixedCostDao
import com.palmfarm.manager.data.database.entities.FixedCost
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Fixed Costs management with depreciation calculations
 */
class FixedCostsViewModel(
    private val fixedCostDao: FixedCostDao
) : ViewModel() {

    private val _filterType = MutableStateFlow<String?>(null)

    /**
     * Get all fixed costs with depreciation calculations
     */
    val fixedCostsWithDepreciation: StateFlow<List<FixedCostWithDepreciation>> = combine(
        fixedCostDao.getAllFixedCosts(),
        _filterType
    ) { fixedCosts, filterType ->
        var filtered = fixedCosts

        // Filter by category
        if (filterType != null) {
            filtered = filtered.filter { it.category == filterType }
        }

        // Map to FixedCostWithDepreciation
        filtered.map { fixedCost ->
            val monthsElapsed = calculateMonthsElapsed(fixedCost.date, System.currentTimeMillis())
            val totalMonths = fixedCost.lifeSpanYears * 12
            val monthlyDepreciation = fixedCost.monthlyDepreciation

            val usedValue = if (monthsElapsed >= totalMonths) {
                fixedCost.amount
            } else {
                monthlyDepreciation * monthsElapsed
            }

            val valueLeft = fixedCost.amount - usedValue
            val depreciationPercent = (usedValue / fixedCost.amount) * 100

            FixedCostWithDepreciation(
                fixedCost = fixedCost,
                usedValue = usedValue,
                valueLeft = valueLeft,
                depreciationPercent = depreciationPercent,
                monthlyDepreciation = monthlyDepreciation,
                isFullyDepreciated = valueLeft <= 0
            )
        }.sortedByDescending { it.fixedCost.date }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * Get total value left for all fixed costs
     */
    val totalValueLeft: StateFlow<Double> = fixedCostsWithDepreciation.map { list ->
        list.sumOf { it.valueLeft }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    /**
     * Get total depreciation
     */
    val totalDepreciation: StateFlow<Double> = fixedCostsWithDepreciation.map { list ->
        list.sumOf { it.usedValue }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    /**
     * Set filter type
     */
    fun setFilterType(type: String?) {
        _filterType.value = type
    }

    /**
     * Save fixed cost
     */
    fun saveFixedCost(fixedCost: FixedCost) {
        viewModelScope.launch {
            if (fixedCost.id == 0) {
                fixedCostDao.insert(fixedCost)
            } else {
                fixedCostDao.update(fixedCost)
            }
        }
    }

    /**
     * Delete fixed cost
     */
    fun deleteFixedCost(fixedCostId: Int) {
        viewModelScope.launch {
            fixedCostDao.deleteById(fixedCostId)
        }
    }

    /**
     * Calculate months elapsed between two timestamps
     */
    private fun calculateMonthsElapsed(startMillis: Long, endMillis: Long): Int {
        val startCalendar = java.util.Calendar.getInstance().apply { timeInMillis = startMillis }
        val endCalendar = java.util.Calendar.getInstance().apply { timeInMillis = endMillis }

        val yearsDiff = endCalendar.get(java.util.Calendar.YEAR) - startCalendar.get(java.util.Calendar.YEAR)
        val monthsDiff = endCalendar.get(java.util.Calendar.MONTH) - startCalendar.get(java.util.Calendar.MONTH)

        return yearsDiff * 12 + monthsDiff
    }
}

/**
 * Data class for Fixed Cost with depreciation info
 */
data class FixedCostWithDepreciation(
    val fixedCost: FixedCost,
    val usedValue: Double,
    val valueLeft: Double,
    val depreciationPercent: Double,
    val monthlyDepreciation: Double,
    val isFullyDepreciated: Boolean
)
