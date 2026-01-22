package com.palmfarm.manager.ui.finances.reports

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.palmfarm.manager.data.database.dao.*
import com.palmfarm.manager.data.database.entities.AdvancePayment
import com.palmfarm.manager.data.database.entities.Consumption
import com.palmfarm.manager.data.database.entities.Expense
import com.palmfarm.manager.data.database.entities.FixedCost
import com.palmfarm.manager.data.database.entities.Loan
import com.palmfarm.manager.data.database.entities.Sale
import com.palmfarm.manager.data.database.entities.Task
import com.palmfarm.manager.data.database.entities.WagePayment
import com.palmfarm.manager.data.repository.ProductionCycleRepository
import com.palmfarm.manager.data.repository.ProductionRepository
import com.palmfarm.manager.ui.common.BaseViewModel
import com.palmfarm.manager.utils.Constants
import com.palmfarm.manager.utils.CurrencyUtils
import com.palmfarm.manager.utils.PdfGenerator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for Financial Reports
 */
class ReportsViewModel(
    private val expenseDao: ExpenseDao,
    private val fixedCostDao: FixedCostDao,
    private val wagePaymentDao: WagePaymentDao,
    private val saleDao: SaleDao,
    private val consumptionDao: ConsumptionDao,
    private val loanDao: LoanDao,
    private val advancePaymentDao: AdvancePaymentDao,
    private val taskDao: TaskDao,
    private val cashTransactionDao: CashTransactionDao,
    private val appSettingsDao: AppSettingsDao,
    private val cycleRepository: ProductionCycleRepository,
    private val productionRepository: ProductionRepository
) : BaseViewModel() {

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.CURRENT_CYCLE)
    val selectedPeriod: StateFlow<ReportPeriod> = _selectedPeriod.asStateFlow()

    /**
     * Set report period
     */
    fun setReportPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    /**
     * Get cash flow data
     */
    fun getCashFlowData(): Flow<CashFlowData> = combine(
        expenseDao.getAllExpenses(),
        wagePaymentDao.getAllWagePayments(),
        advancePaymentDao.getAllAdvancePayments(),
        saleDao.getAllSales(),
        consumptionDao.getAllConsumption(),
        loanDao.getActiveLoans(),
        fixedCostDao.getAllFixedCosts(),
        selectedPeriod
    ) { expensesArray ->
        val expenses = expensesArray[0] as List<*>
        val wages = expensesArray[1] as List<*>
        val advances = expensesArray[2] as List<*>
        val sales = expensesArray[3] as List<*>
        val consumption = expensesArray[4] as List<*>
        val loans = expensesArray[5] as List<*>
        val fixedCosts = expensesArray[6] as List<*>
        val period = expensesArray[7] as ReportPeriod

        val dateRange = getDateRange(period)

        // Filter data by date range
        val filteredExpenses = expenses.filter { (it as? Expense)?.date in dateRange }
        val filteredWages = wages.filter { (it as? WagePayment)?.paymentDate in dateRange }
        val filteredAdvances = advances.filter { (it as? AdvancePayment)?.date in dateRange }
        val filteredSales = sales.filter { (it as? Sale)?.date in dateRange }
        val filteredConsumption = consumption.filter { (it as? Consumption)?.date in dateRange }

        // Group by month
        val monthlyData = mutableMapOf<String, MonthlyFinancials>()

        // Process sales
        filteredSales.forEach { saleItem ->
            val sale = saleItem as Sale
            val monthKey = getMonthKey(sale.date)
            val current = monthlyData.getOrDefault(monthKey, MonthlyFinancials(monthKey))
            monthlyData[monthKey] = current.copy(income = current.income + sale.totalAmount)
        }

        // Process consumption
        filteredConsumption.forEach { consumptionItem ->
            val cons = consumptionItem as Consumption
            val monthKey = getMonthKey(cons.date)
            val current = monthlyData.getOrDefault(monthKey, MonthlyFinancials(monthKey))
            monthlyData[monthKey] = current.copy(income = current.income + cons.valuedAtPrice)
        }

        // Process expenses
        filteredExpenses.forEach { expenseItem ->
            val expense = expenseItem as Expense
            val monthKey = getMonthKey(expense.date)
            val current = monthlyData.getOrDefault(monthKey, MonthlyFinancials(monthKey))
            monthlyData[monthKey] = current.copy(expenses = current.expenses + expense.amount)
        }

        // Process wages (net payment)
        filteredWages.forEach { wageItem ->
            val wage = wageItem as WagePayment
            val monthKey = getMonthKey(wage.paymentDate)
            val current = monthlyData.getOrDefault(monthKey, MonthlyFinancials(monthKey))
            monthlyData[monthKey] = current.copy(expenses = current.expenses + wage.netPayment)
        }

        // Process advances
        filteredAdvances.forEach { advanceItem ->
            val advance = advanceItem as AdvancePayment
            val monthKey = getMonthKey(advance.date)
            val current = monthlyData.getOrDefault(monthKey, MonthlyFinancials(monthKey))
            monthlyData[monthKey] = current.copy(expenses = current.expenses + advance.amount)
        }

        // Add loan payments for active loans
        (loans as List<Loan>).forEach { loan ->
            if (loan.startDate in dateRange) {
                val monthKey = getMonthKey(loan.startDate)
                val current = monthlyData.getOrDefault(monthKey, MonthlyFinancials(monthKey))
                monthlyData[monthKey] = current.copy(
                    expenses = current.expenses + (loan.monthlyPayment * loan.numberOfPaymentsMade)
                )
            }
        }

        // Calculate depreciation for each month
        (fixedCosts as List<FixedCost>).forEach { fixedCost ->
            if (fixedCost.date <= dateRange.last) {
                val monthlyDepreciation = fixedCost.amount / (fixedCost.lifeSpanYears * 12)
                // Add to each month in range
                monthlyData.keys.forEach { monthKey ->
                    val current = monthlyData.getValue(monthKey)
                    monthlyData[monthKey] = current.copy(
                        expenses = current.expenses + monthlyDepreciation
                    )
                }
            }
        }

        // Sort by month and calculate cumulative
        val sortedMonthly = monthlyData.values.sortedBy { it.month }
        var cumulative = 0.0
        val withCumulative = sortedMonthly.map { monthly ->
            val netCashFlow = monthly.income - monthly.expenses
            cumulative += netCashFlow
            monthly.copy(
                netCashFlow = netCashFlow,
                cumulativeBalance = cumulative
            )
        }

        CashFlowData(
            period = period,
            monthlyData = withCumulative,
            totalIncome = withCumulative.sumOf { it.income },
            totalExpenses = withCumulative.sumOf { it.expenses },
            netCashFlow = withCumulative.sumOf { it.netCashFlow }
        )
    }

    /**
     * Get profitability data
     */
    fun getProfitabilityData(): Flow<ProfitabilityData> = combine(
        saleDao.getTotalSalesAmount(),
        consumptionDao.getTotalConsumptionValue(),
        expenseDao.getTotalExpenses(),
        wagePaymentDao.getTotalWagesPaid(),
        advancePaymentDao.getTotalAdvances(),
        fixedCostDao.getAllFixedCosts(),
        loanDao.getActiveLoans(),
        selectedPeriod
    ) { dataArray ->
        val sales = dataArray[0] as Double
        val consumption = dataArray[1] as Double
        val expenses = dataArray[2] as Double
        val wagesNet = dataArray[3] as Double
        val advances = dataArray[4] as Double
        val fixedCosts = dataArray[5] as List<FixedCost>
        val loans = dataArray[6] as List<Loan>
        val period = dataArray[7] as ReportPeriod

        val totalIncome = sales + consumption
        val wagesTotal = wagesNet + advances

        // Calculate depreciation
        val totalDepreciation = fixedCosts.sumOf { fixedCost ->
            val monthsElapsed = calculateMonthsElapsed(fixedCost.date, System.currentTimeMillis())
            val totalMonths = fixedCost.lifeSpanYears * 12
            if (monthsElapsed >= totalMonths) {
                fixedCost.amount
            } else {
                (fixedCost.amount / totalMonths) * monthsElapsed
            }
        }

        // Operational expenses (excluding depreciation)
        val operationalExpenses = expenses + wagesTotal + loans.sumOf { it.monthlyPayment * it.numberOfPaymentsMade }

        val grossMargin = totalIncome - operationalExpenses
        val ebitda = grossMargin // Same as gross margin before depreciation
        val netBalance = totalIncome - operationalExpenses - totalDepreciation

        val grossMarginPercent = if (totalIncome > 0) (grossMargin / totalIncome) * 100 else 0.0
        val netMarginPercent = if (totalIncome > 0) (netBalance / totalIncome) * 100 else 0.0

        val benefitToCostRatio = if (totalIncome > 0) (totalIncome/operationalExpenses) else 0.0
        val profitRate = if(totalIncome > 0) ((benefitToCostRatio - 1) * 100) else 0.0

        ProfitabilityData(
            period = period,
            totalIncome = totalIncome,
            salesIncome = sales,
            consumptionIncome = consumption,
            operationalExpenses = operationalExpenses,
            expensesBreakdown = mapOf(
                "Expenses" to expenses,
                "Wages" to wagesTotal,
                "Loan Payments" to loans.sumOf { it.monthlyPayment * it.numberOfPaymentsMade }
            ),
            totalDepreciation = totalDepreciation,
            grossMargin = grossMargin,
            ebitda = ebitda,
            netBalance = netBalance,
            grossMarginPercent = grossMarginPercent,
            netMarginPercent = netMarginPercent,
            benefitToCostRatio = benefitToCostRatio,
            profitRate = profitRate
        )
    }

    /**
     * Get balance sheet data
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getBalanceSheetData(): Flow<BalanceSheetData> = cycleRepository.getCurrentCycle()
        .flatMapLatest { cycle ->
            val cycleId = cycle?.id ?: 0
            val bunchesAvailableFlow: Flow<Int> = if (cycleId > 0) {
                productionRepository.calculateBunchesAvailableFlow(cycleId)
            } else {
                flowOf(0)
            }
            val oilPerBunchFlow: Flow<Double> = if (cycleId > 0) {
                productionRepository.oilPerBunchFlow(cycleId)
            } else {
                flowOf(0.0)
            }

            val financialInputsFlow: Flow<FinancialInputs> = combine(
                saleDao.getLastSaleByUnit("GALLON"),
                fixedCostDao.getAllFixedCosts(),
                loanDao.getActiveLoans()
            ) { lastSale, fixedCosts, loans ->
                FinancialInputs(
                    lastSale = lastSale as? Sale,
                    fixedCosts = fixedCosts as List<FixedCost>,
                    loans = loans as List<Loan>
                )
            }

            val assetInputsFlow: Flow<AssetInputs> = combine(
                productionRepository.oilStockFlow(),
                bunchesAvailableFlow,
                oilPerBunchFlow
            ) { oilStock, bunchesAvailable, oilPerBunch ->
                AssetInputs(
                    oilStock = oilStock as Double,
                    bunchesAvailable = bunchesAvailable as Int,
                    oilPerBunch = oilPerBunch as Double
                )
            }

            val processedTotalsFlow: Flow<ProcessedTotals> = combine(
                wagePaymentDao.getWagePaymentsByCycle(cycleId),
                advancePaymentDao.getAdvancePaymentsByCycle(cycleId)
            ) { payments, advances ->
                ProcessedTotals(
                    netPayments = payments.sumOf { it.netPayment },
                    totalAdvances = advances.sumOf { it.amount }
                )
            }

            // Calculate pending gross (sum of all unpaid completed tasks' gross wages)
            val pendingGrossFlow: Flow<Double> = taskDao.getTasksByCycle(cycleId)
                .map { tasks ->
                    tasks.filter {
                        it.status.equals(Constants.TASK_STATUS_COMPLETED, ignoreCase = true) &&
                        it.paidInWagePaymentId == null &&
                        (it.quantity ?: 0.0) > 0.0
                    }.sumOf { (it.quantity ?: 0.0) * it.payRate }
                }

            val outstandingFlow: Flow<Double> = combine(
                taskDao.getTasksByCycle(cycleId),
                advancePaymentDao.getAdvancePaymentsByCycle(cycleId),
                wagePaymentDao.getWagePaymentsByCycle(cycleId)
            ) { tasks, advances, payments ->
                calculateOutstandingWages(tasks, advances, payments, cycleId)
            }

            val cashBalanceFlow: Flow<Double> = cashTransactionDao.getCurrentBalanceFlow()
                .map { it ?: 0.0 }

            financialInputsFlow.flatMapLatest { financial ->
                combine(assetInputsFlow, processedTotalsFlow, pendingGrossFlow, cashBalanceFlow) { assets, processed, pendingGross, cash ->
                    val lastSalesPrice = financial.lastSale?.unitPrice ?: 0.0
                    val oilStockValue = assets.oilStock * lastSalesPrice
                    val bunchesValue = assets.bunchesAvailable * assets.oilPerBunch * lastSalesPrice / 20

                    val totalCurrentAssets = oilStockValue + bunchesValue + cash

                    val fixedAssets = financial.fixedCosts.sumOf { fixedCost ->
                        val monthsElapsed = calculateMonthsElapsed(fixedCost.date, System.currentTimeMillis())
                        val totalMonths = fixedCost.lifeSpanYears * 12
                        val usedValue = if (monthsElapsed >= totalMonths) {
                            fixedCost.amount
                        } else {
                            (fixedCost.amount / totalMonths) * monthsElapsed
                        }
                        fixedCost.amount - usedValue
                    }

                    val totalAssets = totalCurrentAssets + fixedAssets

                    val loansPayable = financial.loans.sumOf { it.totalLeft }
                    val wagesPayable = pendingGross
                    val totalLiabilities = loansPayable + wagesPayable

                    val netWorth = totalAssets - totalLiabilities

                    BalanceSheetData(
                        asOfDate = System.currentTimeMillis(),
                        currentAssets = mapOf(
                            "Oil Stock" to oilStockValue,
                            "Bunches Available" to bunchesValue,
                            "Cash" to cash
                        ),
                        totalCurrentAssets = totalCurrentAssets,
                        fixedAssets = fixedAssets,
                        totalAssets = totalAssets,
                        loansPayable = loansPayable,
                        wagesPayable = wagesPayable,
                        totalLiabilities = totalLiabilities,
                        netWorth = netWorth
                    )
                }
            }
        }

    private data class FinancialInputs(
        val lastSale: Sale?,
        val fixedCosts: List<FixedCost>,
        val loans: List<Loan>
    )

    private data class AssetInputs(
        val oilStock: Double,
        val bunchesAvailable: Int,
        val oilPerBunch: Double
    )

    private data class ProcessedTotals(
        val netPayments: Double,
        val totalAdvances: Double
    )

    private val ProcessedTotals.processedNet: Double
        get() = netPayments + totalAdvances

    /**
     * Get date range based on period
     */
    private suspend fun getDateRange(period: ReportPeriod): LongRange {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis

        return when (period) {
            ReportPeriod.CURRENT_CYCLE -> {
                val cycle = cycleRepository.getCurrentCycle().first()
                if (cycle != null) {
                    cycle.startDate..cycle.endDate
                } else {
                    0L..endDate
                }
            }
            ReportPeriod.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.timeInMillis
                startDate..endDate
            }
            ReportPeriod.QUARTER -> {
                val currentMonth = calendar.get(Calendar.MONTH)
                val quarterStartMonth = (currentMonth / 3) * 3
                calendar.set(Calendar.MONTH, quarterStartMonth)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.timeInMillis
                startDate..endDate
            }
            ReportPeriod.YEAR -> {
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.timeInMillis
                startDate..endDate
            }
            ReportPeriod.ALL_TIME -> {
                0L..endDate
            }
        }
    }

    /**
     * Get month key from timestamp
     */
    private fun getMonthKey(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        return String.format("%d-%02d", year, month)
    }

    /**
     * Calculate months elapsed
     */
    private fun calculateMonthsElapsed(startMillis: Long, endMillis: Long): Int {
        val startCalendar = Calendar.getInstance().apply { timeInMillis = startMillis }
        val endCalendar = Calendar.getInstance().apply { timeInMillis = endMillis }

        val yearsDiff = endCalendar.get(Calendar.YEAR) - startCalendar.get(Calendar.YEAR)
        val monthsDiff = endCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH)

        return yearsDiff * 12 + monthsDiff
    }

    private fun calculateOutstandingWages(
        tasks: List<Task>,
        advances: List<AdvancePayment>,
        payments: List<WagePayment>,
        cycleId: Int
    ): Double {
        // Filter tasks by cycle, completed status, unpaid, and quantity > 0
        val filteredTasks = tasks.filter {
            it.cycleId == cycleId &&
            it.status.equals(Constants.TASK_STATUS_COMPLETED, ignoreCase = true) &&
            it.paidInWagePaymentId == null &&
            (it.quantity ?: 0.0) > 0.0
        }

        val tasksByWorker = filteredTasks.groupBy { it.workerId }
        val advancesByWorker = advances.groupBy { it.workerId }
        val paymentsByWorker = payments.groupBy { it.workerId }

        val workerIds = mutableSetOf<Int>()
        workerIds.addAll(tasksByWorker.keys)
        workerIds.addAll(advancesByWorker.keys)

        var total = 0.0
        workerIds.forEach { workerId ->
            val unpaidTasks = tasksByWorker[workerId] ?: emptyList()

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

    /**
     * Generate comprehensive financial report PDF
     */
    suspend fun generateComprehensiveFinancialReport(context: Context): Result<File> {
        return try {
            showLoading()
            
            val period = selectedPeriod.value
            val periodString = when (period) {
                ReportPeriod.CURRENT_CYCLE -> "Current Cycle"
                ReportPeriod.MONTH -> "Current Month"
                ReportPeriod.QUARTER -> "Current Quarter"
                ReportPeriod.YEAR -> "Current Year"
                ReportPeriod.ALL_TIME -> "All Time"
            }

            // Get all report data
            val cashFlowData = getCashFlowData().first()
            val profitabilityData = getProfitabilityData().first()
            val balanceSheetData = getBalanceSheetData().first()
            val enterpriseName = appSettingsDao.getSettingsOnce()?.enterpriseName ?: "Palm Farm"

            // Generate PDF
            val pdfGenerator = PdfGenerator(context)
            val file = pdfGenerator.generateComprehensiveFinancialReport(
                enterpriseName = enterpriseName,
                cashFlowData = cashFlowData,
                profitabilityData = profitabilityData,
                balanceSheetData = balanceSheetData,
                period = periodString
            )

            if (file != null) {
                hideLoading()
                showSuccess("Financial report generated successfully")
                Result.success(file)
            } else {
                hideLoading()
                showError("Failed to generate financial report")
                Result.failure(Exception("Failed to generate PDF"))
            }
        } catch (e: Exception) {
            hideLoading()
            showError("Error generating report: ${e.message}")
            Result.failure(e)
        }
    }
}

/**
 * Report period options
 */
enum class ReportPeriod {
    CURRENT_CYCLE,
    MONTH,
    QUARTER,
    YEAR,
    ALL_TIME
}

/**
 * Cash flow data
 */
data class CashFlowData(
    val period: ReportPeriod,
    val monthlyData: List<MonthlyFinancials>,
    val totalIncome: Double,
    val totalExpenses: Double,
    val netCashFlow: Double
)

data class MonthlyFinancials(
    val month: String,
    val income: Double = 0.0,
    val expenses: Double = 0.0,
    val netCashFlow: Double = 0.0,
    val cumulativeBalance: Double = 0.0
)

/**
 * Profitability data
 */
data class ProfitabilityData(
    val period: ReportPeriod,
    val totalIncome: Double,
    val salesIncome: Double,
    val consumptionIncome: Double,
    val operationalExpenses: Double,
    val expensesBreakdown: Map<String, Double>,
    val totalDepreciation: Double,
    val grossMargin: Double,
    val ebitda: Double,
    val netBalance: Double,
    val grossMarginPercent: Double,
    val netMarginPercent: Double,
    val benefitToCostRatio: Double,
    val profitRate: Double
)

/**
 * Balance sheet data
 */
data class BalanceSheetData(
    val asOfDate: Long,
    val currentAssets: Map<String, Double>,
    val totalCurrentAssets: Double,
    val fixedAssets: Double,
    val totalAssets: Double,
    val loansPayable: Double,
    val wagesPayable: Double,
    val totalLiabilities: Double,
    val netWorth: Double
)