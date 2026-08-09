package com.palmfarm.manager.ui.analytics

import com.palmfarm.manager.data.database.dao.*
import com.palmfarm.manager.data.repository.ProductionCycleRepository
import com.palmfarm.manager.data.repository.ProductionRepository
import com.palmfarm.manager.ui.common.BaseViewModel
import kotlinx.coroutines.flow.*
import java.util.Calendar

/**
 * ViewModel for Analytics screen with KPIs and charts
 */
class AnalyticsViewModel(
    private val cycleRepository: ProductionCycleRepository,
    private val productionRepository: ProductionRepository,
    private val harvestDao: HarvestDao,
    private val millingDao: MillingDao,
    private val farmDao: FarmDao,
    private val expenseDao: ExpenseDao,
    private val wagePaymentDao: WagePaymentDao,
    private val advancePaymentDao: AdvancePaymentDao,
    private val saleDao: SaleDao,
    private val consumptionDao: ConsumptionDao,
    private val fixedCostDao: FixedCostDao
) : BaseViewModel() {

    /**
     * Get production KPIs
     */
    fun getProductionKPIs(): Flow<ProductionKPIs> = combine(
        cycleRepository.getCurrentCycle(),
        harvestDao.getAllHarvests(),
        millingDao.getAllMillings(),
        farmDao.getTotalPalmsFlow()
    ) { currentCycle, allHarvests, allMillings, totalPalms ->

        val currentCycleId = currentCycle?.id ?: 0
        val currentHarvests = allHarvests
            .filter { it.cycleId == currentCycleId }
            .sortedBy { it.date }
        val currentMillings = allMillings
            .filter { it.cycleId == currentCycleId }
            .sortedBy { it.date }

        val cycleTotalBunches = currentHarvests.sumOf { it.numberOfBunches }
        val cycleBunchesMilled = currentMillings.sumOf { it.bunchesMilled }
        val cycleOilProduced = currentMillings.sumOf { it.oilProducedGallons }

        // All-Time = average for the entire (current) production cycle
        val allTimeBunchesPerTree = if (totalPalms > 0) {
            cycleTotalBunches.toDouble() / totalPalms.toDouble()
        } else 0.0

        val allTimeOilPerBunch = if (cycleBunchesMilled > 0) {
            cycleOilProduced * 20 / cycleBunchesMilled.toDouble()
        } else 0.0

        // Current = last recorded value in the cycle
        val lastHarvest = currentHarvests.lastOrNull()
        val currentBunchesPerTree = if (totalPalms > 0 && lastHarvest != null) {
            lastHarvest.numberOfBunches.toDouble() / totalPalms.toDouble()
        } else 0.0

        val lastMilling = currentMillings.lastOrNull()
        val currentOilPerBunch = if (lastMilling != null && lastMilling.bunchesMilled > 0) {
            lastMilling.oilProducedGallons * 20 / lastMilling.bunchesMilled.toDouble()
        } else 0.0

        // Percentage change = difference between current (last) and all-time (cycle average)
        val bunchesPerTreeChange = calculatePercentageChange(
            allTimeBunchesPerTree,
            currentBunchesPerTree
        )

        val oilPerBunchChange = calculatePercentageChange(
            allTimeOilPerBunch,
            currentOilPerBunch
        )

        ProductionKPIs(
            allTimeBunchesPerTree = allTimeBunchesPerTree,
            currentBunchesPerTree = currentBunchesPerTree,
            bunchesPerTreeChange = bunchesPerTreeChange,
            allTimeOilPerBunch = allTimeOilPerBunch,
            currentOilPerBunch = currentOilPerBunch,
            oilPerBunchChange = oilPerBunchChange
        )
    }

    /**
     * Get financial KPIs
     */
    fun getFinancialKPIs(): Flow<FinancialKPIs> = combine(
        cycleRepository.getCurrentCycle(),
        harvestDao.getAllHarvests(),
        expenseDao.getTotalExpenses(),
        wagePaymentDao.getTotalWagesPaid(),
        saleDao.getTotalSalesAmount(),
        consumptionDao.getTotalConsumptionValue(),
        advancePaymentDao.getTotalAdvances(),
        fixedCostDao.getAllFixedCosts()
    ) { values ->

        val currentCycle = values[0] as? com.palmfarm.manager.data.database.entities.ProductionCycle
        @Suppress("UNCHECKED_CAST")
        val allHarvests = values[1] as List<com.palmfarm.manager.data.database.entities.Harvest>
        val totalExpenses = (values[2] as Number).toDouble()
        val totalWagesNet = (values[3] as Number).toDouble()
        val totalSales = (values[4] as Number).toDouble()
        val totalConsumption = (values[5] as Number).toDouble()
        val totalAdvances = (values[6] as Number).toDouble()
        @Suppress("UNCHECKED_CAST")
        val fixedCosts = values[7] as List<com.palmfarm.manager.data.database.entities.FixedCost>

        val currentCycleId = currentCycle?.id ?: 0
        val currentHarvests = allHarvests.filter { it.cycleId == currentCycleId }
        val currentTotalBunches = currentHarvests.sumOf { it.numberOfBunches }

        // Calculate total depreciation
        val totalDepreciation = fixedCosts.sumOf { fixedCost ->
            val monthsElapsed = calculateMonthsElapsed(fixedCost.date, System.currentTimeMillis())
            val totalMonths = fixedCost.lifeSpanYears * 12
            if (monthsElapsed >= totalMonths) {
                fixedCost.amount
            } else {
                (fixedCost.amount / totalMonths) * monthsElapsed
            }
        }

        val totalWages = totalWagesNet + totalAdvances
        val totalCosts = totalExpenses + totalWages + totalDepreciation

        val currentCostPerBunch = if (currentTotalBunches > 0) {
            totalCosts / currentTotalBunches
        } else 0.0

        val totalRevenue = totalSales + totalConsumption
        val profitMargin = if (totalRevenue > 0) {
            ((totalRevenue - totalCosts) / totalRevenue) * 100
        } else 0.0

        val roi = if (totalCosts > 0) {
            ((totalRevenue - totalCosts) / totalCosts) * 100
        } else 0.0

        val incomePerBunch = if (currentTotalBunches > 0) {
            totalRevenue / currentTotalBunches
        } else 0.0

        val profitPerBunch = if (currentTotalBunches > 0) {
            (totalRevenue - totalCosts) / currentTotalBunches
        } else 0.0

        FinancialKPIs(
            costPerBunch = currentCostPerBunch,
            profitMargin = profitMargin,
            roi = roi,
            totalRevenue = totalRevenue,
            totalCosts = totalCosts,
            netProfit = totalRevenue - totalCosts,
            incomePerBunch = incomePerBunch,
            profitPerBunch = profitPerBunch
        )
    }

    /**
     * Get income vs expenses chart data
     */
    fun getIncomeExpensesChartData(period: ChartPeriod): Flow<List<ChartDataPoint>> = combine(
        saleDao.getAllSales(),
        consumptionDao.getAllConsumption(),
        expenseDao.getAllExpenses(),
        wagePaymentDao.getAllWagePayments(),
        advancePaymentDao.getAllAdvancePayments()
    ) { sales, consumption, expenses, wages, advances ->

        val dateRange = getDateRangeForPeriod(period)

        // Filter by period
        val filteredSales = sales.filter { it.date in dateRange }
        val filteredConsumption = consumption.filter { it.date in dateRange }
        val filteredExpenses = expenses.filter { it.date in dateRange }
        val filteredWages = wages.filter { it.paymentDate in dateRange }
        val filteredAdvances = advances.filter { it.date in dateRange }

        // Group by month
        val monthlyData = mutableMapOf<String, Pair<Double, Double>>()

        filteredSales.forEach { sale ->
            val monthKey = getMonthKey(sale.date)
            val (income, expenses) = monthlyData.getOrDefault(monthKey, Pair(0.0, 0.0))
            monthlyData[monthKey] = Pair(income + sale.totalAmount, expenses)
        }

        filteredConsumption.forEach { cons ->
            val monthKey = getMonthKey(cons.date)
            val (income, expenses) = monthlyData.getOrDefault(monthKey, Pair(0.0, 0.0))
            monthlyData[monthKey] = Pair(income + (cons.quantityGallons * cons.valuedAtPrice), expenses)
        }

        filteredExpenses.forEach { expense ->
            val monthKey = getMonthKey(expense.date)
            val (income, expenses) = monthlyData.getOrDefault(monthKey, Pair(0.0, 0.0))
            monthlyData[monthKey] = Pair(income, expenses + expense.amount)
        }

        filteredWages.forEach { wage ->
            val monthKey = getMonthKey(wage.paymentDate)
            val (income, expenses) = monthlyData.getOrDefault(monthKey, Pair(0.0, 0.0))
            monthlyData[monthKey] = Pair(income, expenses + wage.netPayment)
        }

        filteredAdvances.forEach { advance ->
            val monthKey = getMonthKey(advance.date)
            val (income, expenses) = monthlyData.getOrDefault(monthKey, Pair(0.0, 0.0))
            monthlyData[monthKey] = Pair(income, expenses + advance.amount)
        }

        // Convert to chart data points
        monthlyData.map { (month, data) ->
            ChartDataPoint(
                label = month,
                income = data.first,
                expenses = data.second,
                net = data.first - data.second
            )
        }.sortedBy { it.label }
    }

    /**
     * Get production trends chart data
     */
    fun getProductionTrendsChartData(period: ChartPeriod): Flow<List<ProductionDataPoint>> = combine(
        harvestDao.getAllHarvests(),
        millingDao.getAllMillings()
    ) { harvests, millings ->

        val dateRange = getDateRangeForPeriod(period)

        // Filter by period
        val filteredHarvests = harvests.filter { it.date in dateRange }
        val filteredMillings = millings.filter { it.date in dateRange }

        // Group by month
        val monthlyData = mutableMapOf<String, Triple<Int, Int, Double>>()

        filteredHarvests.forEach { harvest ->
            val monthKey = getMonthKey(harvest.date)
            val (bunches, bunchesMilled, oil) = monthlyData.getOrDefault(
                monthKey,
                Triple(0, 0, 0.0)
            )
            monthlyData[monthKey] = Triple(bunches + harvest.numberOfBunches, bunchesMilled, oil)
        }

        filteredMillings.forEach { milling ->
            val monthKey = getMonthKey(milling.date)
            val (bunches, bunchesMilled, oil) = monthlyData.getOrDefault(
                monthKey,
                Triple(0, 0, 0.0)
            )
            monthlyData[monthKey] = Triple(
                bunches,
                bunchesMilled + milling.bunchesMilled,
                oil + milling.oilProducedGallons
            )
        }

        // Convert to chart data points
        monthlyData.map { (month, data) ->
            ProductionDataPoint(
                label = month,
                bunchesHarvested = data.first,
                bunchesMilled = data.second,
                oilProduced = data.third
            )
        }.sortedBy { it.label }
    }

    /**
     * Get expense breakdown chart data
     */
    fun getExpenseBreakdownChartData(period: ChartPeriod): Flow<List<ExpenseBreakdown>> = combine(
        expenseDao.getAllExpenses(),
        wagePaymentDao.getAllWagePayments(),
        advancePaymentDao.getAllAdvancePayments()
    ) { expenses, wages, advances ->

        val dateRange = getDateRangeForPeriod(period)

        // Filter by period
        val filteredExpenses = expenses.filter { it.date in dateRange }
        val filteredWages = wages.filter { it.paymentDate in dateRange }
        val filteredAdvances = advances.filter { it.date in dateRange }

        // Group by category
        val categoryTotals = filteredExpenses.groupBy { it.category }
            .mapValues { (_, expenseList) -> expenseList.sumOf { it.amount } }

        val breakdown = mutableListOf<ExpenseBreakdown>()

        categoryTotals.forEach { (category, amount) ->
            breakdown.add(ExpenseBreakdown(category, amount))
        }

        // Add wages
        val wagesTotal = filteredWages.sumOf { it.netPayment } + filteredAdvances.sumOf { it.amount }
        if (wagesTotal > 0) {
            breakdown.add(ExpenseBreakdown("Wages", wagesTotal))
        }

        breakdown
    }

    /**
     * Calculate percentage change
     */
    private fun calculatePercentageChange(baseline: Double, current: Double): Double {
        return if (baseline > 0) {
            ((current - baseline) / baseline) * 100
        } else if (current > 0) {
            100.0 // If baseline is 0 but current is positive, show 100% increase
        } else {
            0.0
        }
    }

    /**
     * Get date range for chart period
     */
    private fun getDateRangeForPeriod(period: ChartPeriod): LongRange {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis

        return when (period) {
            ChartPeriod.LAST_6_MONTHS -> {
                calendar.add(Calendar.MONTH, -6)
                calendar.timeInMillis..endDate
            }
            ChartPeriod.LAST_12_MONTHS -> {
                calendar.add(Calendar.MONTH, -12)
                calendar.timeInMillis..endDate
            }
            ChartPeriod.THIS_YEAR -> {
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.timeInMillis..endDate
            }
            ChartPeriod.ALL_TIME -> {
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
     * Get all chart data combined
     */
    fun getChartData(): Flow<ChartData> = combine(
        saleDao.getAllSales(),
        consumptionDao.getAllConsumption(),
        expenseDao.getAllExpenses(),
        wagePaymentDao.getAllWagePayments(),
        advancePaymentDao.getAllAdvancePayments(),
        harvestDao.getAllHarvests(),
        millingDao.getAllMillings(),
        fixedCostDao.getAllFixedCosts()
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val sales = values[0] as List<com.palmfarm.manager.data.database.entities.Sale>
        @Suppress("UNCHECKED_CAST")
        val consumption = values[1] as List<com.palmfarm.manager.data.database.entities.Consumption>
        @Suppress("UNCHECKED_CAST")
        val expenses = values[2] as List<com.palmfarm.manager.data.database.entities.Expense>
        @Suppress("UNCHECKED_CAST")
        val wages = values[3] as List<com.palmfarm.manager.data.database.entities.WagePayment>
        @Suppress("UNCHECKED_CAST")
        val advances = values[4] as List<com.palmfarm.manager.data.database.entities.AdvancePayment>
        @Suppress("UNCHECKED_CAST")
        val harvests = values[5] as List<com.palmfarm.manager.data.database.entities.Harvest>
        @Suppress("UNCHECKED_CAST")
        val millings = values[6] as List<com.palmfarm.manager.data.database.entities.Milling>
        @Suppress("UNCHECKED_CAST")
        val fixedCosts = values[7] as List<com.palmfarm.manager.data.database.entities.FixedCost>

        val lastSixMonthLabels = getLastSixMonthLabels()
        val monthlyMetrics = initializeMonthlyMetrics(lastSixMonthLabels)
        
        // Calculate monthly depreciation for each fixed cost
        fixedCosts.forEach { fixedCost ->
            val monthlyDepreciation = fixedCost.amount / (fixedCost.lifeSpanYears * 12)
            val purchaseMonthKey = getMonthKey(fixedCost.date)
            
            // Add depreciation to each month from purchase date onwards
            lastSixMonthLabels.forEach { monthKey ->
                if (monthKey >= purchaseMonthKey) {
                    monthlyMetrics[monthKey]?.cost = monthlyMetrics[monthKey]?.cost?.plus(monthlyDepreciation) ?: 0.0
                }
            }
        }

        harvests.forEach { harvest ->
            val key = getMonthKey(harvest.date)
            monthlyMetrics[key]?.bunches = monthlyMetrics[key]?.bunches?.plus(harvest.numberOfBunches) ?: 0
        }

        millings.forEach { milling ->
            val key = getMonthKey(milling.date)
            monthlyMetrics[key]?.oilGallons = monthlyMetrics[key]?.oilGallons?.plus(milling.oilProducedGallons) ?: 0.0
        }

        sales.forEach { sale ->
            val key = getMonthKey(sale.date)
            monthlyMetrics[key]?.income = monthlyMetrics[key]?.income?.plus(sale.totalAmount) ?: 0.0
        }

        consumption.forEach { cons ->
            val key = getMonthKey(cons.date)
            monthlyMetrics[key]?.income = monthlyMetrics[key]?.income?.plus(cons.quantityGallons * cons.valuedAtPrice) ?: 0.0
        }

        expenses.forEach { expense ->
            val key = getMonthKey(expense.date)
            monthlyMetrics[key]?.cost = monthlyMetrics[key]?.cost?.plus(expense.amount) ?: 0.0
        }

        wages.forEach { wage ->
            val key = getMonthKey(wage.paymentDate)
            monthlyMetrics[key]?.cost = monthlyMetrics[key]?.cost?.plus(wage.netPayment) ?: 0.0
        }

        advances.forEach { advance ->
            val key = getMonthKey(advance.date)
            monthlyMetrics[key]?.cost = monthlyMetrics[key]?.cost?.plus(advance.amount) ?: 0.0
        }

        // Income vs Expenses data (last 6 months)
        val incomeExpensesData = calculateIncomeExpensesByMonth(sales, consumption, expenses, wages, advances, fixedCosts)

        // Production trends data (last 6 months)
        val productionData = calculateProductionByMonth(harvests, millings)

        // Expense breakdown (current cycle or all-time)
        val expenseBreakdown = calculateExpenseBreakdown(expenses, wages, advances, fixedCosts)

        // Cost analysis by category
        val costAnalysis = calculateCostAnalysis(expenses, wages, advances, fixedCosts)

        // Profitability data (last 6 months)
        val profitabilityData = calculateProfitability(sales, consumption, expenses, wages, advances, fixedCosts)

        val bunchesOnlyData = lastSixMonthLabels.map { label ->
            BunchesPoint(label, monthlyMetrics[label]?.bunches ?: 0)
        }

        val oilOnlyData = lastSixMonthLabels.map { label ->
            OilPoint(label, monthlyMetrics[label]?.oilGallons ?: 0.0)
        }

        val oilToBunchRatioData = lastSixMonthLabels.map { label ->
            val metrics = monthlyMetrics[label] ?: MonthlyMetrics()
            val ratio = if (metrics.bunches > 0) metrics.oilGallons / metrics.bunches * 20 else 0.0
            OilToBunchRatioPoint(label, ratio)
        }

        val perBunchFinancialData = lastSixMonthLabels.map { label ->
            val metrics = monthlyMetrics[label] ?: MonthlyMetrics()
            val bunches = metrics.bunches
            val costPerBunch = if (bunches > 0) metrics.cost / bunches else 0.0
            val incomePerBunch = if (bunches > 0) metrics.income / bunches else 0.0
            PerUnitFinancialPoint(label, costPerBunch, incomePerBunch)
        }

        val perGallonFinancialData = lastSixMonthLabels.map { label ->
            val metrics = monthlyMetrics[label] ?: MonthlyMetrics()
            val gallons = metrics.oilGallons
            val costPerGallon = if (gallons > 0) metrics.cost / gallons else 0.0
            val incomePerGallon = if (gallons > 0) metrics.income / gallons else 0.0
            PerUnitFinancialPoint(label, costPerGallon, incomePerGallon)
        }

        // Calculate cumulative cost and income
        var cumulativeCost = 0.0
        var cumulativeIncome = 0.0
        val cumulativeFinancialData = lastSixMonthLabels.map { label ->
            val metrics = monthlyMetrics[label] ?: MonthlyMetrics()
            cumulativeCost += metrics.cost
            cumulativeIncome += metrics.income
            CumulativeFinancialPoint(label, cumulativeCost, cumulativeIncome)
        }

        ChartData(
            incomeExpensesData = incomeExpensesData,
            productionData = productionData,
            expenseBreakdown = expenseBreakdown,
            costAnalysis = costAnalysis,
            profitabilityData = profitabilityData,
            bunchesOnlyData = bunchesOnlyData,
            oilOnlyData = oilOnlyData,
            oilToBunchRatioData = oilToBunchRatioData,
            perBunchFinancialData = perBunchFinancialData,
            perGallonFinancialData = perGallonFinancialData,
            cumulativeFinancialData = cumulativeFinancialData
        )
    }

    private fun calculateIncomeExpensesByMonth(
        sales: List<com.palmfarm.manager.data.database.entities.Sale>,
        consumption: List<com.palmfarm.manager.data.database.entities.Consumption>,
        expenses: List<com.palmfarm.manager.data.database.entities.Expense>,
        wages: List<com.palmfarm.manager.data.database.entities.WagePayment>,
        advances: List<com.palmfarm.manager.data.database.entities.AdvancePayment>,
        fixedCosts: List<com.palmfarm.manager.data.database.entities.FixedCost>
    ): List<IncomeExpensePoint> {
        val labels = getLastSixMonthLabels()
        val monthlyData = labels.associateWith { Pair(0.0, 0.0) }.toMutableMap()
        
        sales.forEach { sale ->
            val key = getMonthKey(sale.date)
            if (key in monthlyData) {
                val (income, expense) = monthlyData[key]!!
                monthlyData[key] = Pair(income + sale.totalAmount, expense)
            }
        }
        
        consumption.forEach { cons ->
            val key = getMonthKey(cons.date)
            if (key in monthlyData) {
                val (income, expense) = monthlyData[key]!!
                monthlyData[key] = Pair(income + (cons.quantityGallons * cons.valuedAtPrice), expense)
            }
        }
        
        expenses.forEach { expense ->
            val key = getMonthKey(expense.date)
            if (key in monthlyData) {
                val (income, exp) = monthlyData[key]!!
                monthlyData[key] = Pair(income, exp + expense.amount)
            }
        }
        
        wages.forEach { wage ->
            val key = getMonthKey(wage.paymentDate)
            if (key in monthlyData) {
                val (income, expense) = monthlyData[key]!!
                monthlyData[key] = Pair(income, expense + wage.netPayment)
            }
        }
        
        advances.forEach { advance ->
            val key = getMonthKey(advance.date)
            if (key in monthlyData) {
                val (income, expense) = monthlyData[key]!!
                monthlyData[key] = Pair(income, expense + advance.amount)
            }
        }
        
        // Add monthly depreciation for each fixed cost
        fixedCosts.forEach { fixedCost ->
            val monthlyDepreciation = fixedCost.amount / (fixedCost.lifeSpanYears * 12)
            val purchaseMonthKey = getMonthKey(fixedCost.date)
            
            labels.forEach { monthKey ->
                if (monthKey >= purchaseMonthKey && monthKey in monthlyData) {
                    val (income, expense) = monthlyData[monthKey]!!
                    monthlyData[monthKey] = Pair(income, expense + monthlyDepreciation)
                }
            }
        }
        
        return labels.map { label ->
            val (income, expense) = monthlyData[label]!!
            IncomeExpensePoint(label, income, expense)
        }
    }

    private fun calculateProductionByMonth(
        harvests: List<com.palmfarm.manager.data.database.entities.Harvest>,
        millings: List<com.palmfarm.manager.data.database.entities.Milling>
    ): List<ProductionPoint> {
        val labels = getLastSixMonthLabels()
        val monthlyData = labels.associateWith { Pair(0, 0.0) }.toMutableMap()
        
        harvests.forEach { harvest ->
            val key = getMonthKey(harvest.date)
            if (key in monthlyData) {
                val (bunches, oil) = monthlyData[key]!!
                monthlyData[key] = Pair(bunches + harvest.numberOfBunches, oil)
            }
        }
        
        millings.forEach { milling ->
            val key = getMonthKey(milling.date)
            if (key in monthlyData) {
                val (bunches, oil) = monthlyData[key]!!
                monthlyData[key] = Pair(bunches, oil + milling.oilProducedGallons)
            }
        }
        
        return labels.map { label ->
            val (bunches, oil) = monthlyData[label]!!
            ProductionPoint(label, bunches, oil)
        }
    }

    private fun calculateExpenseBreakdown(
        expenses: List<com.palmfarm.manager.data.database.entities.Expense>,
        wages: List<com.palmfarm.manager.data.database.entities.WagePayment>,
        advances: List<com.palmfarm.manager.data.database.entities.AdvancePayment>,
        fixedCosts: List<com.palmfarm.manager.data.database.entities.FixedCost>
    ): Map<String, Double> {
        val breakdown = mutableMapOf<String, Double>()
        
        expenses.groupBy { it.category }.forEach { (category, list) ->
            breakdown[category] = list.sumOf { it.amount }
        }
        
        val totalWages = wages.sumOf { it.netPayment } + advances.sumOf { it.amount }
        if (totalWages > 0) {
            breakdown["Wages"] = totalWages
        }
        
        // Calculate total depreciation
        val totalDepreciation = fixedCosts.sumOf { fixedCost ->
            val monthsElapsed = calculateMonthsElapsed(fixedCost.date, System.currentTimeMillis())
            val totalMonths = fixedCost.lifeSpanYears * 12
            if (monthsElapsed >= totalMonths) {
                fixedCost.amount
            } else {
                (fixedCost.amount / totalMonths) * monthsElapsed
            }
        }
        if (totalDepreciation > 0) {
            breakdown["Depreciation"] = totalDepreciation
        }
        
        return breakdown
    }

    private fun calculateCostAnalysis(
        expenses: List<com.palmfarm.manager.data.database.entities.Expense>,
        wages: List<com.palmfarm.manager.data.database.entities.WagePayment>,
        advances: List<com.palmfarm.manager.data.database.entities.AdvancePayment>,
        fixedCosts: List<com.palmfarm.manager.data.database.entities.FixedCost>
    ): List<CostItem> {
        val categoryTotals = expenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        
        val items = mutableListOf<CostItem>()
        
        categoryTotals.forEach { (category, amount) ->
            items.add(CostItem(category, amount))
        }
        
        val totalWages = wages.sumOf { it.netPayment } + advances.sumOf { it.amount }
        if (totalWages > 0) {
            items.add(CostItem("Wages", totalWages))
        }
        
        // Calculate total depreciation
        val totalDepreciation = fixedCosts.sumOf { fixedCost ->
            val monthsElapsed = calculateMonthsElapsed(fixedCost.date, System.currentTimeMillis())
            val totalMonths = fixedCost.lifeSpanYears * 12
            if (monthsElapsed >= totalMonths) {
                fixedCost.amount
            } else {
                (fixedCost.amount / totalMonths) * monthsElapsed
            }
        }
        if (totalDepreciation > 0) {
            items.add(CostItem("Depreciation", totalDepreciation))
        }
        
        return items.sortedByDescending { it.amount }
    }

    private fun calculateProfitability(
        sales: List<com.palmfarm.manager.data.database.entities.Sale>,
        consumption: List<com.palmfarm.manager.data.database.entities.Consumption>,
        expenses: List<com.palmfarm.manager.data.database.entities.Expense>,
        wages: List<com.palmfarm.manager.data.database.entities.WagePayment>,
        advances: List<com.palmfarm.manager.data.database.entities.AdvancePayment>,
        fixedCosts: List<com.palmfarm.manager.data.database.entities.FixedCost>
    ): List<ProfitabilityPoint> {
        val labels = getLastSixMonthLabels()
        val monthlyData = labels.associateWith { Triple(0.0, 0.0, 0.0) }.toMutableMap()
        
        sales.forEach { sale ->
            val key = getMonthKey(sale.date)
            if (key in monthlyData) {
                val (revenue, costs, _) = monthlyData[key]!!
                monthlyData[key] = Triple(revenue + sale.totalAmount, costs, 0.0)
            }
        }
        
        consumption.forEach { cons ->
            val key = getMonthKey(cons.date)
            if (key in monthlyData) {
                val (revenue, costs, _) = monthlyData[key]!!
                monthlyData[key] = Triple(revenue + (cons.quantityGallons * cons.valuedAtPrice), costs, 0.0)
            }
        }
        
        expenses.forEach { expense ->
            val key = getMonthKey(expense.date)
            if (key in monthlyData) {
                val (revenue, costs, _) = monthlyData[key]!!
                monthlyData[key] = Triple(revenue, costs + expense.amount, 0.0)
            }
        }
        
        wages.forEach { wage ->
            val key = getMonthKey(wage.paymentDate)
            if (key in monthlyData) {
                val (revenue, costs, _) = monthlyData[key]!!
                monthlyData[key] = Triple(revenue, costs + wage.netPayment, 0.0)
            }
        }
        
        advances.forEach { advance ->
            val key = getMonthKey(advance.date)
            if (key in monthlyData) {
                val (revenue, costs, _) = monthlyData[key]!!
                monthlyData[key] = Triple(revenue, costs + advance.amount, 0.0)
            }
        }
        
        // Add monthly depreciation for each fixed cost
        fixedCosts.forEach { fixedCost ->
            val monthlyDepreciation = fixedCost.amount / (fixedCost.lifeSpanYears * 12)
            val purchaseMonthKey = getMonthKey(fixedCost.date)
            
            labels.forEach { monthKey ->
                if (monthKey >= purchaseMonthKey && monthKey in monthlyData) {
                    val (revenue, costs, _) = monthlyData[monthKey]!!
                    monthlyData[monthKey] = Triple(revenue, costs + monthlyDepreciation, 0.0)
                }
            }
        }
        
        return labels.map { label ->
            val (revenue, costs, _) = monthlyData[label]!!
            val profit = revenue - costs
            ProfitabilityPoint(label, revenue, costs, profit)
        }
    }

    private fun getLastSixMonthLabels(): List<String> {
        val calendar = Calendar.getInstance()
        val labels = mutableListOf<String>()
        for (i in 5 downTo 0) {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.add(Calendar.MONTH, -i)
            labels.add(getMonthKey(calendar.timeInMillis))
            calendar.add(Calendar.MONTH, i)
        }
        return labels
    }

    private fun initializeMonthlyMetrics(labels: List<String>): MutableMap<String, MonthlyMetrics> {
        return labels.associateWith { MonthlyMetrics() }.toMutableMap()
    }

    /**
     * Calculate months elapsed between two timestamps
     */
    private fun calculateMonthsElapsed(startMillis: Long, endMillis: Long): Int {
        val startCalendar = Calendar.getInstance().apply { timeInMillis = startMillis }
        val endCalendar = Calendar.getInstance().apply { timeInMillis = endMillis }

        val yearsDiff = endCalendar.get(Calendar.YEAR) - startCalendar.get(Calendar.YEAR)
        val monthsDiff = endCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH)

        return yearsDiff * 12 + monthsDiff
    }
}

/**
 * Production KPIs
 */
data class ProductionKPIs(
    val allTimeBunchesPerTree: Double,
    val currentBunchesPerTree: Double,
    val bunchesPerTreeChange: Double,
    val allTimeOilPerBunch: Double,
    val currentOilPerBunch: Double,
    val oilPerBunchChange: Double
)

/**
 * Financial KPIs
 */
data class FinancialKPIs(
    val costPerBunch: Double,
    val profitMargin: Double,
    val roi: Double,
    val totalRevenue: Double,
    val totalCosts: Double,
    val netProfit: Double,
    val incomePerBunch: Double,
    val profitPerBunch: Double
)

/**
 * Chart data point for income/expenses
 */
data class ChartDataPoint(
    val label: String,
    val income: Double,
    val expenses: Double,
    val net: Double
)

/**
 * Production data point for trends
 */
data class ProductionDataPoint(
    val label: String,
    val bunchesHarvested: Int,
    val bunchesMilled: Int,
    val oilProduced: Double
)

/**
 * Expense breakdown by category
 */
data class ExpenseBreakdown(
    val category: String,
    val amount: Double
)

/**
 * Chart period options
 */
enum class ChartPeriod {
    LAST_6_MONTHS,
    LAST_12_MONTHS,
    THIS_YEAR,
    ALL_TIME
}

/**
 * Chart data container for all charts
 */
data class ChartData(
    val incomeExpensesData: List<IncomeExpensePoint>,
    val productionData: List<ProductionPoint>,
    val expenseBreakdown: Map<String, Double>,
    val costAnalysis: List<CostItem>,
    val profitabilityData: List<ProfitabilityPoint>,
    val bunchesOnlyData: List<BunchesPoint>,
    val oilOnlyData: List<OilPoint>,
    val oilToBunchRatioData: List<OilToBunchRatioPoint>,
    val perBunchFinancialData: List<PerUnitFinancialPoint>,
    val perGallonFinancialData: List<PerUnitFinancialPoint>,
    val cumulativeFinancialData: List<CumulativeFinancialPoint>
)

/**
 * Income expense point for line chart
 */
data class IncomeExpensePoint(
    val label: String,
    val income: Double,
    val expense: Double
)

/**
 * Production point for line chart
 */
data class ProductionPoint(
    val label: String,
    val bunches: Int,
    val oilGallons: Double
)

/**
 * Cost item for bar chart
 */
data class CostItem(
    val category: String,
    val amount: Double
)

/**
 * Profitability point for multi-bar chart
 */
data class ProfitabilityPoint(
    val label: String,
    val revenue: Double,
    val costs: Double,
    val profit: Double
)

data class BunchesPoint(
    val label: String,
    val bunches: Int
)

data class OilPoint(
    val label: String,
    val gallons: Double
)

data class OilToBunchRatioPoint(
    val label: String,
    val gallonsPerBunch: Double
)

data class PerUnitFinancialPoint(
    val label: String,
    val costPerUnit: Double,
    val incomePerUnit: Double
)

data class CumulativeFinancialPoint(
    val label: String,
    val cumulativeCost: Double,
    val cumulativeIncome: Double
)

private data class MonthlyMetrics(
    var bunches: Int = 0,
    var oilGallons: Double = 0.0,
    var income: Double = 0.0,
    var cost: Double = 0.0
)
