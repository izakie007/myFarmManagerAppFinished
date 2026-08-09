package com.palmfarm.manager.data.repository

import com.palmfarm.manager.data.database.dao.*
import com.palmfarm.manager.data.database.entities.ProductionCycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Repository for ProductionCycle operations
 */
class ProductionCycleRepository(
    private val productionCycleDao: ProductionCycleDao,
    private val expenseDao: ExpenseDao,
    private val saleDao: SaleDao,
    private val wagePaymentDao: WagePaymentDao,
    private val advancePaymentDao: AdvancePaymentDao,
    private val loanDao: LoanDao
) {

    /**
     * Get current active production cycle
     */
    fun getCurrentCycle(): Flow<ProductionCycle?> {
        return productionCycleDao.getCurrentCycle()
    }

    /**
     * Get all production cycles ordered by start date descending
     */
    fun getAllCycles(): Flow<List<ProductionCycle>> {
        return productionCycleDao.getAllCycles()
    }

    /**
     * Operational expenses for a cycle
     * (expenses + wages/net payments + advances + loan payments)
     */
    fun getCycleExpensesFlow(cycleId: Int): Flow<Double> {
        return combine(
            expenseDao.getTotalExpensesByCycle(cycleId),
            wagePaymentDao.getTotalWagesPaidForCycleFlow(cycleId),
            advancePaymentDao.getTotalAdvancesForCycleFlow(cycleId),
            loanDao.getActiveLoans()
        ) { expenses, wages, advances, loans ->
            val loanPayments = loans.sumOf { it.monthlyPayment * it.numberOfPaymentsMade }
            expenses + wages + advances + loanPayments
        }
    }

    /**
     * Total income for a cycle (sales)
     */
    fun getCycleIncomeFlow(cycleId: Int): Flow<Double> {
        return saleDao.getTotalIncomeForCycle(cycleId)
    }
}
