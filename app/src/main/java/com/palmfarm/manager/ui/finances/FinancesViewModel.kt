package com.palmfarm.manager.ui.finances

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palmfarm.manager.data.database.dao.*
import com.palmfarm.manager.data.repository.ProductionCycleRepository
import com.palmfarm.manager.data.repository.ProductionRepository
import com.palmfarm.manager.utils.Constants
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Finances hub screen
 * Provides summary data for all finance categories
 */
class FinancesViewModel(
    private val expenseDao: ExpenseDao,
    private val fixedCostDao: FixedCostDao,
    private val wagePaymentDao: WagePaymentDao,
    private val advancePaymentDao: AdvancePaymentDao,
    private val saleDao: SaleDao,
    private val consumptionDao: ConsumptionDao,
    private val loanDao: LoanDao,
    private val taskDao: TaskDao,
    private val cycleRepository: ProductionCycleRepository,
    private val productionRepository: ProductionRepository
) : ViewModel() {

    private val _financeSummary = MutableStateFlow(FinanceSummary())
    val financeSummary: StateFlow<FinanceSummary> = _financeSummary.asStateFlow()

    init {
        loadFinanceSummary()
    }

    /**
     * Load summary data for all finance categories
     */
    private fun loadFinanceSummary() {
        viewModelScope.launch {
            cycleRepository.getCurrentCycle()
                .flatMapLatest { cycle ->
                    val cycleId = cycle?.id ?: 0

                    val outstandingFlow: Flow<Double> = combine(
                        taskDao.getAllTasks(),
                        advancePaymentDao.getAllAdvancePayments(),
                        wagePaymentDao.getAllWagePayments()
                    ) { tasks, advances, payments ->
                        calculateOutstandingWages(tasks, advances, payments)
                    }

                    val processedTotalsFlow: Flow<ProcessedTotals> = combine(
                        wagePaymentDao.getAllWagePayments(),
                        advancePaymentDao.getTotalAdvances()
                    ) { payments, totalAdvances ->
                        ProcessedTotals(
                            netPayments = payments.sumOf { it.netPayment },
                            totalAdvances = totalAdvances
                        )
                    }

                    val expensesFlow = expenseDao.getTotalExpenses()
                    val fixedCostsFlow = fixedCostDao.getAllFixedCosts()
                    val wagesPaidFlow = wagePaymentDao.getTotalWagesPaid()
                    val salesFlow = saleDao.getTotalSalesAmount()
                    val consumptionFlow = consumptionDao.getTotalConsumptionValue()
                    val loansFlow = loanDao.getActiveLoans()
                    val advancesFlow = advancePaymentDao.getTotalAdvances()

                    val baseFinanceFlow: Flow<FinanceInputs> = combine(
                        expensesFlow,
                        fixedCostsFlow
                    ) { expenses, fixedCosts ->
                        Pair((expenses as Number).toDouble(), fixedCosts as List<com.palmfarm.manager.data.database.entities.FixedCost>)
                    }.flatMapLatest { (expenses, fixedCosts) ->
                        combine(
                            wagesPaidFlow,
                            salesFlow,
                            consumptionFlow,
                            loansFlow
                        ) { wagesPaid, sales, consumption, loans ->
                            FinanceInputs(
                                expenses = expenses,
                                fixedCosts = fixedCosts,
                                wagesPaid = (wagesPaid as Number).toDouble(),
                                sales = (sales as Number).toDouble(),
                                consumption = (consumption as Number).toDouble(),
                                loans = loans as List<com.palmfarm.manager.data.database.entities.Loan>
                            )
                        }
                    }.combine(advancesFlow) { inputs, advances ->
                        inputs.copy(totalAdvances = advances)
                    }

                    combine(baseFinanceFlow, outstandingFlow, processedTotalsFlow) { inputs, outstanding, processed ->
                        val totalDepreciation = inputs.fixedCosts.sumOf { fixedCost ->
                            val monthsElapsed = calculateMonthsElapsed(fixedCost.date, System.currentTimeMillis())
                            val totalMonths = fixedCost.lifeSpanYears * 12
                            val usedValue = if (monthsElapsed >= totalMonths) {
                                fixedCost.amount
                            } else {
                                (fixedCost.amount / totalMonths.toDouble()) * monthsElapsed.toDouble()
                            }
                            usedValue
                        }

                        val totalLoanDebt = inputs.loans.sumOf { it.totalLeft }
                        val totalIncome = inputs.sales + inputs.consumption
                        val wagesPaidTotal = processed.processedNet
                        val totalExpenses = inputs.expenses +
                            wagesPaidTotal +
                            totalDepreciation +
                            inputs.loans.sumOf { it.monthlyPayment * it.numberOfPaymentsMade.toDouble() }
                        val netBalance = totalIncome - totalExpenses

                        FinanceSummary(
                            totalExpenses = inputs.expenses,
                            totalWagesPaid = wagesPaidTotal,
                            totalWagesUnpaid = outstanding,
                            totalAdvances = inputs.totalAdvances,
                            totalSales = inputs.sales,
                            totalConsumption = inputs.consumption,
                            totalIncome = totalIncome,
                            totalLoanDebt = totalLoanDebt,
                            totalDepreciation = totalDepreciation,
                            netBalance = netBalance,
                            activeFixedCostsCount = inputs.fixedCosts.count { calculateValueLeft(it) > 0 },
                            activeLoansCount = inputs.loans.size,
                            processedNet = processed.processedNet
                        )
                    }
                }
                .collect { summary ->
                    _financeSummary.value = summary
                }
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

    /**
     * Calculate value left for a fixed cost
     */
    private fun calculateValueLeft(fixedCost: com.palmfarm.manager.data.database.entities.FixedCost): Double {
        val monthsElapsed = calculateMonthsElapsed(fixedCost.date, System.currentTimeMillis())
        val totalMonths = fixedCost.lifeSpanYears * 12
        val usedValue = if (monthsElapsed >= totalMonths) {
            fixedCost.amount
        } else {
            (fixedCost.amount / totalMonths.toDouble()) * monthsElapsed.toDouble()
        }
        return fixedCost.amount - usedValue
    }
    private data class FinanceInputs(
        val expenses: Double,
        val fixedCosts: List<com.palmfarm.manager.data.database.entities.FixedCost>,
        val wagesPaid: Double,
        val sales: Double,
        val consumption: Double,
        val loans: List<com.palmfarm.manager.data.database.entities.Loan>,
        val totalAdvances: Double = 0.0
    )

    private data class ProcessedTotals(
        val netPayments: Double,
        val totalAdvances: Double
    ) {
        val processedNet: Double get() = netPayments + totalAdvances
    }

    private fun calculateOutstandingWages(
        tasks: List<com.palmfarm.manager.data.database.entities.Task>,
        advances: List<com.palmfarm.manager.data.database.entities.AdvancePayment>,
        payments: List<com.palmfarm.manager.data.database.entities.WagePayment>
    ): Double {
        val tasksByWorker = tasks.groupBy { it.workerId }
        val advancesByWorker = advances.groupBy { it.workerId }
        val paymentsByWorker = payments.groupBy { it.workerId }

        val workerIds = mutableSetOf<Int>()
        workerIds.addAll(tasksByWorker.keys)
        workerIds.addAll(advancesByWorker.keys)

        var total = 0.0
        workerIds.forEach { workerId ->
            val unpaidTasks = tasksByWorker[workerId]?.filter {
                it.status.equals(Constants.TASK_STATUS_COMPLETED, ignoreCase = true) &&
                        it.paidInWagePaymentId == null
            } ?: emptyList()

            val gross = unpaidTasks.sumOf { (it.quantity ?: 0.0) * it.payRate }
            val totalAdvances = advancesByWorker[workerId]?.sumOf { it.amount } ?: 0.0
            val advancesApplied = paymentsByWorker[workerId]?.sumOf { it.totalAdvances } ?: 0.0
            val outstandingAdvances = (totalAdvances - advancesApplied).coerceAtLeast(0.0)
            val net = gross - outstandingAdvances.coerceAtMost(gross)

            if (gross > 0 || outstandingAdvances > 0) {
                total += net.coerceAtLeast(0.0)
            }
        }

        return total
    }
}

/**
 * Data class for finance summary
 */
data class FinanceSummary(
    val totalExpenses: Double = 0.0,
    val totalWagesPaid: Double = 0.0,
    val totalWagesUnpaid: Double = 0.0,
    val totalAdvances: Double = 0.0,
    val totalSales: Double = 0.0,
    val totalConsumption: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalLoanDebt: Double = 0.0,
    val totalDepreciation: Double = 0.0,
    val netBalance: Double = 0.0,
    val activeFixedCostsCount: Int = 0,
    val activeLoansCount: Int = 0,
    val processedNet: Double = 0.0
)
