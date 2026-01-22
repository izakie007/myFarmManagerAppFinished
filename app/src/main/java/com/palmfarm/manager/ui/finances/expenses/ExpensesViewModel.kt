package com.palmfarm.manager.ui.finances.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palmfarm.manager.data.database.dao.ExpenseDao
import com.palmfarm.manager.data.database.entities.Expense
import com.palmfarm.manager.data.repository.ProductionCycleRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Expenses management
 */
class ExpensesViewModel(
    private val expenseDao: ExpenseDao,
    private val cycleRepository: ProductionCycleRepository
) : ViewModel() {

    private val _filterCategory = MutableStateFlow<String?>(null)
    private val _filterPeriod = MutableStateFlow(FilterPeriod.ALL)

    private val currentCycleId = cycleRepository.getCurrentCycle()
        .map { it?.id ?: 0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    /**
     * Get filtered expenses
     */
    val expenses: StateFlow<List<Expense>> = combine(
        expenseDao.getAllExpenses(),
        _filterCategory,
        _filterPeriod,
        currentCycleId
    ) { expenses, category, period, cycleId ->
        var filtered = expenses

        // Filter by category
        if (category != null) {
            filtered = filtered.filter { it.category == category }
        }

        // Filter by period
        filtered = when (period) {
            FilterPeriod.CURRENT_CYCLE -> {
                val cycle = cycleRepository.getCurrentCycle().first()
                if (cycle != null) {
                    filtered.filter { it.date >= cycle.startDate && it.date <= cycle.endDate }
                } else {
                    filtered
                }
            }
            FilterPeriod.MONTH -> {
                val calendar = java.util.Calendar.getInstance()
                val startOfMonth = calendar.apply {
                    set(java.util.Calendar.DAY_OF_MONTH, 1)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                filtered.filter { it.date >= startOfMonth }
            }
            FilterPeriod.QUARTER -> {
                val calendar = java.util.Calendar.getInstance()
                val currentMonth = calendar.get(java.util.Calendar.MONTH)
                val quarterStartMonth = (currentMonth / 3) * 3
                val startOfQuarter = calendar.apply {
                    set(java.util.Calendar.MONTH, quarterStartMonth)
                    set(java.util.Calendar.DAY_OF_MONTH, 1)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                filtered.filter { it.date >= startOfQuarter }
            }
            FilterPeriod.YEAR -> {
                val calendar = java.util.Calendar.getInstance()
                val startOfYear = calendar.apply {
                    set(java.util.Calendar.MONTH, java.util.Calendar.JANUARY)
                    set(java.util.Calendar.DAY_OF_MONTH, 1)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                filtered.filter { it.date >= startOfYear }
            }
            FilterPeriod.ALL -> filtered
        }

        filtered.sortedByDescending { it.date }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * Get total expenses for display
     */
    val totalExpenses: StateFlow<Double> = expenses.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    /**
     * Set filter category
     */
    fun setFilterCategory(category: String?) {
        _filterCategory.value = category
    }

    /**
     * Set filter period
     */
    fun setFilterPeriod(period: FilterPeriod) {
        _filterPeriod.value = period
    }

    /**
     * Save expense
     */
    fun saveExpense(expense: Expense) {
        viewModelScope.launch {
            if (expense.id == 0) {
                expenseDao.insert(expense)
            } else {
                expenseDao.update(expense)
            }
        }
    }

    /**
     * Delete expense
     */
    fun deleteExpense(expenseId: Int) {
        viewModelScope.launch {
            expenseDao.deleteById(expenseId)
        }
    }

    /**
     * Get a single expense by id
     */
    suspend fun getExpenseById(expenseId: Int): Expense? {
        return expenseDao.getExpenseById(expenseId)
    }
}

/**
 * Filter period options
 */
enum class FilterPeriod {
    ALL,
    CURRENT_CYCLE,
    MONTH,
    QUARTER,
    YEAR
}
