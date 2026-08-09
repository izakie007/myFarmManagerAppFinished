package com.palmfarm.manager.data.repository

import com.palmfarm.manager.data.database.dao.*
import com.palmfarm.manager.data.database.entities.ProductionCycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Repository for ProductionCycle operations
 */
class ProductionCycleRepository(
    private val productionCycleDao: ProductionCycleDao,
    private val harvestDao: HarvestDao,
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
     * Get cycle by ID
     */
    fun getCycleById(cycleId: Int): Flow<ProductionCycle?> {
        return productionCycleDao.getCycleById(cycleId)
    }

    /**
     * Insert new production cycle
     */
    suspend fun insertCycle(cycle: ProductionCycle): Long {
        return productionCycleDao.insertCycle(cycle)
    }

    /**
     * Update existing cycle
     */
    suspend fun updateCycle(cycle: ProductionCycle) {
        productionCycleDao.updateCycle(cycle)
    }

    /**
     * Mark cycle as not current
     */
    suspend fun markCycleAsNotCurrent(cycleId: Int) {
        productionCycleDao.markCycleAsNotCurrent(cycleId)
    }

    /**
     * Update realized bunches for a cycle
     */
    suspend fun updateRealizedBunches(cycleId: Int, realizedBunches: Int) {
        productionCycleDao.updateRealizedBunches(cycleId, realizedBunches)
    }

    /**
     * Get last 3 cycles for history display
     */
    suspend fun getLastThreeCycles(): List<ProductionCycle> {
        return productionCycleDao.getAllCycles().first().take(3)
    }

    /**
     * Calculate cycle progress percentage
     */
    suspend fun calculateCycleProgress(cycleId: Int): Double {
        val cycle = productionCycleDao.getCycleByIdOnce(cycleId)
        return if (cycle != null && cycle.expectedBunches > 0) {
            ((cycle.realizedBunches.toDouble()) / cycle.expectedBunches.toDouble()) * 100.0
        } else {
            0.0
        }
    }

    /**
     * Calculate operational expenses for a cycle
     * (expenses + wages/net payments + advances + loan payments)
     */
    suspend fun getCycleExpenses(cycleId: Int): Double {
        val expenses = expenseDao.getTotalExpensesByCycle(cycleId).first()
        val wages = wagePaymentDao.getTotalWagesPaidForCycle(cycleId)
        val advances = advancePaymentDao.getTotalAdvancesForCycle(cycleId)
        val loanPayments = loanDao.getActiveLoans().first()
            .sumOf { it.monthlyPayment * it.numberOfPaymentsMade }
        return expenses + wages + advances + loanPayments
    }

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
     * Calculate total income for a cycle (sales)
     */
    suspend fun getCycleIncome(cycleId: Int): Double {
        return saleDao.getTotalIncomeForCycle(cycleId).first()
    }

    fun getCycleIncomeFlow(cycleId: Int): Flow<Double> {
        return saleDao.getTotalIncomeForCycle(cycleId)
    }

    /**
     * Calculate bunches per tree for a cycle
     */
    suspend fun calculateBunchesPerTree(cycleId: Int, totalPalmTrees: Int): Double {
        val cycle = productionCycleDao.getCycleByIdOnce(cycleId)
        return if (cycle != null && totalPalmTrees > 0) {
            cycle.realizedBunches.toDouble() / totalPalmTrees.toDouble()
        } else {
            0.0
        }
    }

    fun getCycleHistorySummaries(limit: Int): Flow<List<ProductionCycle>> {
        return productionCycleDao.getCompletedCycles(limit)
    }
}
